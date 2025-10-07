<template>
  <div class="deepseek-tool">
    <h2 class="tool-title">Deepseek API 测试</h2>
    <textarea
      v-model="prompt"
      v-autoresize="{ max: 320 }"
      class="prompt-input"
      rows="1"
      placeholder="输入测试内容..."
    />
    <div class="tool-actions">
      <button class="btn-run" :disabled="loading" @click="run">{{ loading ? '测试中...' : '测试 Deepseek' }}</button>
      <button class="btn-clear" :disabled="loading && !result && !error" @click="clearAll">清空</button>
    </div>
    <div v-if="error" class="tool-error">{{ error }}</div>
    <div v-if="history.length" class="tool-result">
      <div class="result-header">
        <h3>返回结果</h3>
        <div class="result-actions">
          <button class="mini-btn" @click="clearHistory" :disabled="!history.length">清空历史</button>
        </div>
      </div>
      <ul class="result-history">
        <li v-for="item in reversedHistory" :key="item.id" class="history-item" :class="[ item.role, { latest: item.id === (history[history.length-1] && history[history.length-1].id) } ]">
          <div class="history-meta">
            <span class="badge" :class="item.role">{{ item.role==='user' ? '我' : 'AI' }}</span>
            <span class="time">{{ item.timeText }}</span>
            <button class="mini-btn link" @click="copy(item.content)">复制</button>
          </div>
          <textarea class="result-output" :value="item.content" readonly spellcheck="false"></textarea>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import autoResize from '../directives/autoresize.js'
import http from '@/api/http'

/**
 * Props:
 *  - schema: 模板结构对象（用于 system 提示 & 字段过滤）
 *  - model: 模型名称（默认 deepseek-chat）
 *  - autoSystem: 是否自动注入 system 提示
 */
const props = defineProps({
  schema: { type: Object, default: null },
  model: { type: String, default: 'deepseek-chat' },
  autoSystem: { type: Boolean, default: true },
  // 外部传入的当前表单数据，将在发送 prompt 前作为上下文注入（仅当变更）
  formData: { type: Object, default: null }
})

// Emits:
//  fill(parsedObject) -> 请求返回且解析成功，外部可用来填充表单
//  chat-update(historyArray) -> 每次对话变更
//  error(message)
const emit = defineEmits(['fill', 'chat-update', 'error'])

const prompt = ref('你好，介绍一下deepseek api')
const result = ref('')
// 历史输出：[{ id:number, ts:number, timeText:string, content:string }]
const history = ref([])
const reversedHistory = computed(()=> [...history.value].reverse())
const error = ref('')
const loading = ref(false)
// auto-resize handled by directive v-autoresize

// 对话历史
const chatHistory = ref([
  { role: 'system', content: '你是实验报告智能助手，请严格按照json格式回复，不允许json中套用json。' }
])

const allowedExtraKey = 'talk'

// 确保对象中存在 talk 字段：
// 1) 若缺失且存在其它字段，则拼接非空字段形成说明
// 2) 若解析失败前只有原始文本，则由调用方包装 { talk: 原始文本 }
function ensureTalk(obj){
  if(!obj || typeof obj!=='object') return obj;
  if(Object.prototype.hasOwnProperty.call(obj, allowedExtraKey)) return obj;
  // 聚合其它字段内容
  const parts=[];
  for(const [k,v] of Object.entries(obj)){
    if(v==null) continue;
    if(Array.isArray(v) && v.length===0) continue;
    if(typeof v==='string' && v.trim()==='') continue;
    let text;
    if(Array.isArray(v)) text = v.map(x=> typeof x==='string'? x : JSON.stringify(x)).join('\n');
    else if(typeof v==='object') text = JSON.stringify(v);
    else text = String(v);
    parts.push(`${k}: ${text}`);
  }
  obj[allowedExtraKey] = parts.length ? parts.join('\n\n') : '';
  return obj;
}

function ensureSystemPrompt(schema){
  if(!schema||!props.autoSystem) return;
  const marker='模板结构(JSON)';
  if(chatHistory.value.some(m=>m.role==='system'&&m.content.includes(marker))) return;
  const headerKeys=(schema.header||[]).map(f=>f.key);
  const sectionKeys=(schema.sections||[]).map(f=>f.key);
  const footerKeys=(schema.footer||[]).map(f=>f.key);
  const allKeys=[...headerKeys,...sectionKeys,...footerKeys];
  const fieldList = allKeys.join(', ');
  const instruction=`你是实验报告智能助手。\n`+
`模板结构(JSON)：${JSON.stringify(schema)}\n`+
`严格输出规范（务必遵守）：\n`+
`1. 仅输出一个【顶层 JSON 对象】，不允许任何额外说明、Markdown、代码块标记。\n`+
`2. 顶层字段：必须包含所有模板字段：${fieldList}，以及必备字段 talk。=> 最终总字段集合 = (${fieldList}) + talk。\n`+
`3. talk 字段始终必须存在：\n   - 类型为字符串。\n   - 用于自然语言解释、总结或补充说明。\n   - 如果没有可解释内容，也必须输出空字符串 \\\"\\\"（不可省略字段）。\n`+
`4. 除 talk 以外，禁止新增任何未列出的字段；禁止输出 response、result、data 等其它名称。\n`+
`5. list 类型字段输出【字符串数组】；textarea 与 text 输出【字符串】。\n`+
`6. 不允许任何值是嵌套对象或再嵌套 JSON 字符串（需要表达复合含义时请在 talk 中描述）。\n`+
`7. 缺失/未知内容一律写空字符串 \\\"\\\" 或空数组 []（对 list 字段）。\n`+
`8. 不要将整个 JSON 再包为字符串（不要出现转义的 { 或 \"key\" 形式的整体字符串）。\n`+
`9. 严禁输出多个 JSON；若你认为需要多段说明，请全部合并进 talk 字段。\n`+
`10. 如果用户请求与模板无关的额外问题，只在 talk 中回答，模板字段仍需保留（空字符串或原有内容），严禁新增结构。`;
  chatHistory.value.unshift({role:'system',content:instruction})
}

function normalizeAiOutput(parsed,schema){ if(!parsed||!schema) return parsed; const allowed=new Set([...(schema.header||[]).map(f=>f.key),...(schema.sections||[]).map(f=>f.key),...(schema.footer||[]).map(f=>f.key),allowedExtraKey]); Object.keys(parsed).forEach(k=>{ if(!allowed.has(k)) delete parsed[k] }); const listKeys=new Set([...(schema.sections||[]).filter(s=>s.type==='list').map(s=>s.key),...(schema.header||[]).filter(h=>h.type==='list').map(h=>f.key),...(schema.footer||[]).filter(f=>f.type==='list').map(f=>f.key)]); for(const key of Object.keys(parsed)){ if(key===allowedExtraKey) continue; const val=parsed[key]; if(listKeys.has(key)){ if(Array.isArray(val)) parsed[key]=val.map(v=>typeof v==='string'?v:JSON.stringify(v)); else if(typeof val==='string') parsed[key]=val.split(/[,，;；\n]/).map(s=>s.trim()).filter(Boolean); else if(val==null) parsed[key]=[]; else parsed[key]=[JSON.stringify(val)]; } else { if(Array.isArray(val)) parsed[key]=val.map(v=>typeof v==='string'?v:JSON.stringify(v)).join('；'); else if(val&&typeof val==='object') parsed[key]=JSON.stringify(val); else if(val==null) parsed[key]=''; } } return parsed }

let runCounter = 0
function formatTime(ts){ const d=new Date(ts); const pad=n=>String(n).padStart(2,'0'); return `${d.getHours()}:${pad(d.getMinutes())}:${pad(d.getSeconds())}` }
async function run() {
  result.value = ''
  error.value = ''
  loading.value = true
  try {
    ensureSystemPrompt(props.schema)
    // 1) 在用户 prompt 之前注入表单上下文（如果有且发生变更）
    if(props.formData && typeof props.formData==='object'){
      const serialized = JSON.stringify(props.formData)
      if(!runState.lastFormSignature || runState.lastFormSignature!==serialized){
        chatHistory.value.push({ role: 'user', content: '__FORM_CONTEXT__'+serialized })
        runState.lastFormSignature = serialized
      }
    }
    // 插入用户消息到对话与历史
    chatHistory.value.push({ role: 'user', content: prompt.value })
    const userTs = Date.now()
    history.value.push({ id: ++runCounter, ts: userTs, timeText: formatTime(userTs), content: prompt.value, role: 'user' })
    const payload = { model: props.model, messages: chatHistory.value }
  // 使用 axios http 发送 (拦截器自动附带 CSRF)
  const { data: rawResponse } = await http.post('/api/deepseek/chat', payload, { responseType:'text' })
    const text = typeof rawResponse === 'string' ? rawResponse : (rawResponse?.data || JSON.stringify(rawResponse))
    let parsed = null
    const trimmed = text.trim()
    if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
      try { parsed = JSON.parse(trimmed) } catch (_) {}
    }
    if (!parsed) {
      let jsons = []
      let matches = text.match(/\{[\s\S]*?\}/g)
      if (matches) {
        matches.forEach(str => { try { jsons.push(JSON.parse(str)) } catch (_) {} })
      }
      if (jsons.length > 0) parsed = jsons[jsons.length - 1]
      result.value = jsons.length > 0 ? jsons.map(j => JSON.stringify(j, null, 2)).join('\n---\n') : '[返回内容不是有效JSON]\n' + text
    } else {
      result.value = JSON.stringify(parsed, null, 2)
    }
    if (parsed) {
      // 如果模型返回的是 response 字段而不是期望的 talk，则进行映射
      if (parsed.response !== undefined && parsed[allowedExtraKey] === undefined) {
        parsed[allowedExtraKey] = parsed.response
        delete parsed.response
      }
      ensureTalk(parsed)
      normalizeAiOutput(parsed, props.schema)
      chatHistory.value.push({ role: 'assistant', content: JSON.stringify(parsed) })
      emit('chat-update', chatHistory.value)
      emit('fill', parsed)
      const talkVal = parsed[allowedExtraKey]
      let displayText
      if (talkVal !== undefined) {
        if (Array.isArray(talkVal)) displayText = talkVal.join('\n')
        else if (typeof talkVal === 'string') displayText = talkVal
        else displayText = JSON.stringify(talkVal)
      } else {
        displayText = JSON.stringify(parsed, null, 2)
      }
      result.value = displayText
  // 推入历史 (assistant)
  const ts = Date.now()
  history.value.push({ id: ++runCounter, ts, timeText: formatTime(ts), content: displayText, role: 'assistant' })
    } else {
      // 无法解析出结构化 JSON，直接包装为 { talk: 原始文本 }
      const fallback = { [allowedExtraKey]: text.trim() }
      chatHistory.value.push({ role: 'assistant', content: JSON.stringify(fallback) })
      emit('chat-update', chatHistory.value)
      // 不 emit fill，因为没有结构化字段
      const displayText = fallback[allowedExtraKey]
      const ts = Date.now()
      history.value.push({ id: ++runCounter, ts: userTs, timeText: formatTime(ts), content: displayText, role: 'assistant' })
    }
  } catch (e) {
    error.value = e.message || String(e)
    emit('error', error.value)
  }
  loading.value = false
}

function clearAll(){ prompt.value=''; result.value=''; error.value=''; }
function clearHistory(){ history.value=[] }
function copy(text){ try { navigator.clipboard.writeText(text) } catch(_) { /* ignore */ } }
// 折叠功能已移除

// 从已有 chatHistory (包含 assistant 消息) 恢复可视化历史列表
function restoreFromChatContext(context){
  if(!Array.isArray(context)) return;
  // 1) 先覆盖内部 chatHistory（完整保留原始消息，包括 system 与隐藏 form context）
  chatHistory.value = context.map(m=> ({ ...m }));
  // 若不存在 system 且需要自动注入，则追加（保持兼容旧草稿）
  if(!context.some(m=>m && m.role==='system')) ensureSystemPrompt(props.schema);
  // 2) 重建可视化历史（仅展示 user/assistant，且跳过隐藏 form context）
  history.value=[];
  let localCounter = 0;
  context.forEach(m=>{
    if(!m || (m.role!=='assistant' && m.role!=='user')) return;
    if(typeof m.content !== 'string') return;
    if(m.role==='user' && m.content.startsWith('__FORM_CONTEXT__')) return; // skip hidden form context
    let display = m.content;
    if(m.role==='assistant'){
      try {
        const parsedObj = JSON.parse(m.content);
        if(parsedObj && typeof parsedObj==='object'){
          if(parsedObj[allowedExtraKey]!==undefined){
            const tv = parsedObj[allowedExtraKey];
            if(Array.isArray(tv)) display = tv.join('\n');
            else if(typeof tv==='string') display = tv; else display = JSON.stringify(tv);
          } else {
            display = JSON.stringify(parsedObj, null, 2);
          }
        }
      } catch(_) { /* ignore parse error */ }
    }
    const ts = Date.now() - (context.length - localCounter)*40;
    history.value.push({ id: ++localCounter, ts, timeText: formatTime(ts), content: display, role: m.role })
  });
  runCounter = localCounter;
}

// 暴露方法供父组件获取状态
// 初始暴露延后，与 resetContext 合并

// 运行时状态记录表单签名，用于避免重复注入
const runState = { lastFormSignature: '' }

// 对外提供重置方法，被父组件在“重新编辑”时调用
function resetContext(){
  // 保留最初的 system 指令（如果 autoSystem=true 会在首次 run 前注入；此处直接清空后再在下一次 run 时自动补）
  chatHistory.value = [ { role: 'system', content: '你是实验报告智能助手，请严格按照json格式回复，不允许json中套用json。' } ];
  history.value = [];
  prompt.value = '';
  result.value = '';
  error.value = '';
  runState.lastFormSignature = '';
}

// 对外暴露能力
defineExpose({ getChatHistory: () => chatHistory.value, getResultHistory: () => history.value, restoreFromChatContext, resetContext })

</script>

<script>
export default { directives: { autoresize: autoResize } }
</script>

<style scoped>
.deepseek-tool { text-align:left; }
.tool-title { margin:0 0 var(--space-3); font-size:1.1rem; }
.prompt-input { width:95%; display:block; margin:0 auto; resize:none; border:1px solid var(--color-border); border-radius: var(--radius-sm); padding: var(--space-3); font-family: inherit; font-size: var(--font-size-sm); background: var(--color-surface); }
.prompt-input:focus { outline:none; border-color: var(--color-primary); box-shadow:0 0 0 2px rgba(59,130,246,0.15); }
.tool-actions { display:flex; gap: var(--space-3); margin: var(--space-2) 0 var(--space-3); }
.btn-run { background: var(--color-primary-alt); color:#fff; border:none; padding:6px 14px; border-radius: var(--radius-sm); cursor:pointer; }
.btn-run:hover { background: var(--color-primary-alt-hover); }
.btn-clear { background: var(--color-gray-150); border:none; padding:6px 14px; border-radius: var(--radius-sm); cursor:pointer; }
.btn-clear:hover { background: var(--color-gray-200); }
.tool-result { background: var(--color-blue-50); border-radius: var(--radius-sm); padding: var(--space-3); font-size: var(--font-size-xs); white-space: pre-wrap; word-break: break-all; }
.tool-error { margin: var(--space-2) 0; font-size: var(--font-size-xs); color: var(--color-red-500); }
.result-output { width:100%; box-sizing:border-box; background:#fff; border:1px solid var(--color-border); border-radius: var(--radius-sm); padding:8px 10px; font-family: inherit; font-size: 13px; line-height:1.5; resize: vertical; max-height:300px; min-height:120px; overflow:auto; white-space:pre-wrap; }
.result-output:focus { outline:none; border-color: var(--color-primary); box-shadow:0 0 0 2px rgba(59,130,246,0.12); }
.result-header { display:flex; align-items:center; justify-content:space-between; margin-bottom:6px; }
.result-actions { display:flex; gap:6px; }
.mini-btn { background: var(--color-gray-150); border:1px solid var(--color-border); padding:2px 8px; font-size:12px; border-radius:4px; cursor:pointer; }
.mini-btn:hover { background: var(--color-gray-200); }
.mini-btn.link { background:transparent; border:none; color: var(--color-primary); padding:0 6px; }
.mini-btn.link:hover { text-decoration:underline; background:transparent; }
.result-history { list-style:none; padding:0; margin:0; display:flex; flex-direction:column; gap:14px; }
.history-item { position:relative; display:flex; flex-direction:column; gap:6px; }
.history-item.latest { border-left:4px solid var(--color-primary); padding-left:8px; }
.history-meta { display:flex; align-items:center; gap:8px; font-size:12px; color: var(--color-text-secondary); }
.badge { display:inline-block; background: var(--color-primary); color:#fff; font-size:11px; padding:2px 6px; border-radius:10px; }
.badge.user { background: var(--color-secondary); }
.badge.assistant { background: var(--color-primary); }
.history-item.user .result-output { background:#fff; }
.history-item.assistant .result-output { background:#f5f9ff; }
</style>
