<template>
  <div class="assist-wrapper">
  <div class="top-bar" :style="assistBarStyle">
      <div class="top-bar-left"><strong>实验助手</strong></div>
      <div class="top-bar-center">
        <template v-if="!templateLocked">
          <select v-model="selectedType" class="top-select">
            <option value="none">选择报告类型</option>
            <option v-for="t in templates" :key="t.templateType" :value="t.templateType">{{ t.templateName || t.templateType }}</option>
          </select>
        </template>
        <span v-else class="template-chip">模板：{{ currentTemplateName }}</span>
      </div>
      <div class="top-bar-right">
        <button class="top-btn" @click="scrollToEditor">回到编辑</button>
  <button v-if="selectedType !== 'none'" class="top-btn" @click="saveDraft">保存草稿</button>
        <div class="draft-dropdown-wrapper" ref="draftWrapper">
          <button class="top-btn" @click="toggleDraftMenu">使用草稿</button>
          <div v-if="showDraftMenu" class="draft-menu" @keydown.esc.stop="showDraftMenu=false">
            <div class="draft-menu-header">
              <span>我的草稿</span>
              <button class="refresh-btn" @click.stop="fetchDrafts" :disabled="loadingDrafts">{{ loadingDrafts ? '...' : '刷新' }}</button>
            </div>
            <div v-if="draftError" class="draft-error">{{ draftError }}</div>
            <div v-else-if="drafts.length===0" class="draft-empty">暂无草稿</div>
            <ul v-else class="draft-list">
              <li v-for="d in drafts" :key="d.id" class="draft-item" @click="applyDraft(d)" :class="{ applying: applyingDraftId===d.id }">
                <div class="draft-title" :title="d.title">
                  <span v-if="applyingDraftId===d.id" class="draft-spinner" aria-hidden="true"></span>
                  {{ d.title }}
                </div>
                <div class="draft-time">{{ applyingDraftId===d.id ? '加载中...' : d.updateTimeFmt }}</div>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
    <div class="assist-container">
      <div class="assist-header">
        <div class="assist-header-left">
          <h1>自助辅助编写</h1>
          <p class="assist-intro">拖拽下面的模块可重新排序；可切换垂直/水平布局。</p>
        </div>
        <div class="assist-header-actions">
          <button class="layout-toggle-btn" @click="toggleLayoutMode">{{ layoutMode==='vertical' ? '切换为水平布局' : '切换为垂直布局' }}</button>
        </div>
      </div>
   <div class="assist-layout" :class="'mode-' + layoutMode" 
     v-drag-sort="{ list: blocks, key: 'id', storageKey: LS_KEY_ORDER, dragClass: 'dragging', overClass: 'over', requireGrabCursor: true, handles: '.block-header' }"
     @drag-sort-update="onBlocksReordered">
        <div v-for="b in blocks" :key="b.id" class="assist-block" :data-drag-key="b.id">
          <div class="block-header" draggable="true">
            <span class="block-title">{{ b.title }}</span>
            <span class="drag-hint">⇅</span>
          </div>
          <div class="block-body">
            <template v-if="b.type==='viewer'">
              <div v-if="currentTemplateUrl" class="viewer-wrapper">
                <JsonViewer ref="jsonViewerRef" :json-url="currentTemplateUrl" @template-loaded="onTemplateLoaded" />
              </div>
              <div v-else class="placeholder">请选择上方模板以加载结构</div>
            </template>
            <template v-else-if="b.type==='deepseek'">
              <DeepseekTool ref="deepseekRef" :schema="templateSchema" :form-data="currentFormData" @fill="onDeepseekFill" @chat-update="onDeepseekChatUpdate" @error="onDeepseekError" />
            </template>
            <template v-else-if="b.type==='docgen'">
              <DocGenBlock :form-data="resolveViewer()?.getFormData?.() || null" :template-type="selectedType !== 'none' ? selectedType : null" @toast="triggerToast" />
            </template>
          </div>
        </div>
      </div>
    </div>
    <transition name="toast-fade">
      <div v-if="showToast" class="toast">
        <span>{{ toastMessage }}</span>
        <button class="toast-close" @click="closeToast">×</button>
      </div>
    </transition>
    <div v-if="showUnsavedConfirm" class="modal-mask">
      <div class="modal-container">
        <div class="modal-header">提示</div>
        <div class="modal-body">当前内容尚未保存，继续将丢失未保存的修改，是否继续？</div>
        <div class="modal-footer">
          <button class="modal-btn secondary" @click="cancelProceed">取消</button>
          <button class="modal-btn primary" @click="confirmProceed">继续</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import dragSort from '../directives/dragsort.js'
import JsonViewer from '../template/JsonViewer.vue'
import DeepseekTool from '../template/DeepseekTool.vue'
import DocGenBlock from '../template/DocGenBlock.vue'
import { getCsrfToken } from '../utils/csrf.js'
import { compressChatHistory, parseMaybeCompressed } from '../utils/chatCompress.js'
import { useToast } from '../composables/useToast.js'
import { useAssistLayout } from '../composables/useAssistLayout.js'

// layout via composable
const { blocks, layoutMode, load: loadLayoutFromStorage, toggle: toggleLayoutMode, onBlocksReordered } = useAssistLayout()

// template selection
const jsonViewerRef = ref(null)
const selectedType = ref('none')
const templateLocked = ref(false)
const templates = ref([])
const currentTemplateUrl = ref('')
const currentTemplateName = computed(()=>{ if(selectedType.value==='none') return '未选择'; const meta=templates.value.find(t=>t.templateType===selectedType.value); return meta ? (meta.templateName||meta.templateType) : selectedType.value })
const globalBarVisible = ref(true)
let mainBarObserver = null
const assistBarStyle = computed(()=>({ top: (globalBarVisible.value?60:0)+'px' }))
function observeMainTopBar(){ const header=document.querySelector('header.top-bar'); if(!header){ globalBarVisible.value=false; return } mainBarObserver=new IntersectionObserver(entries=> entries.forEach(e=> globalBarVisible.value=e.isIntersecting), { threshold:[0,0.01,1]}); mainBarObserver.observe(header) }
async function loadTemplates(){ try { const res=await fetch('/api/report/templates'); if(!res.ok) return; const data=await res.json(); if(data.success) templates.value=data.templates||[] } catch(_){} }
watch(selectedType,(val)=>{ const meta=templates.value.find(t=>t.templateType===val); if(meta){ currentTemplateUrl.value=meta.url; if(val!=='none' && !templateLocked.value) templateLocked.value=true } else if(val==='none') currentTemplateUrl.value='' })

// drafts
const drafts = ref([])
const showDraftMenu = ref(false)
const draftError = ref('')
const loadingDrafts = ref(false)
const applyingDraftId = ref(null)
// 当前正在编辑的草稿 id；应用某草稿后设置，用于后续保存覆盖
const currentDraftId = ref(null)
const draftWrapper = ref(null)
const showUnsavedConfirm = ref(false)
let pendingOpenDraftMenu = false
let pendingFill = null
let pendingFillTimer = null
function resolveViewer(){ let inst=jsonViewerRef.value; return Array.isArray(inst)?inst[0]:inst||null }
function doFillDraft(parsed,id,label){ const MAX=10; let n=0; (function loop(){ const inst=resolveViewer(); if(inst?.fillForm){ try{ inst.fillForm(parsed); triggerToast(label||('已加载草稿 #'+id)); showDraftMenu.value=false; pendingFill=null; updateSnapshot(); }catch(e){ triggerToast('填充失败: '+e,3000) } return } if(n++<MAX) setTimeout(loop,100) })() }
const lastSnapshotSignature = ref('')
function signature(obj){ if(!obj) return ''; const keys=Object.keys(obj).sort(); return keys.map(k=>k+':'+(typeof obj[k]==='string'?obj[k]:JSON.stringify(obj[k]))).join('|') }
function updateSnapshot(){ const inst=resolveViewer(); const form=inst?.getFormData?.(); if(!form) return; lastSnapshotSignature.value=signature(form) }
function hasUnsavedContent(){ const inst=resolveViewer(); const form=inst?.getFormData?.(); if(!form) return false; const sig=signature(form); if(sig===lastSnapshotSignature.value) return false; return Object.values(form).some(v=>{ if(Array.isArray(v)) return v.length>0; if(typeof v==='string') return v.trim().length>0; return false }) }
function cancelProceed(){ showUnsavedConfirm.value=false; pendingOpenDraftMenu=false }
function confirmProceed(){ showUnsavedConfirm.value=false; if(pendingOpenDraftMenu){ pendingOpenDraftMenu=false; showDraftMenu.value=true; fetchDrafts() } }
function toggleDraftMenu(){ const next=!showDraftMenu.value; if(next){ if(hasUnsavedContent()){ pendingOpenDraftMenu=true; showUnsavedConfirm.value=true; return } showDraftMenu.value=true; fetchDrafts() } else showDraftMenu.value=false }
function onDocumentClick(e){ if(!showDraftMenu.value) return; if(draftWrapper.value && !draftWrapper.value.contains(e.target)) showDraftMenu.value=false }
function onDocumentKey(e){ if(e.key==='Escape' && showDraftMenu.value) showDraftMenu.value=false }
function formatTime(str){ if(!str) return ''; try { return str.replace('T',' ').substring(0,19) } catch(_) { return str } }
function buildDraftTitle(parsed,fallback){ if(!parsed||typeof parsed!=='object') return fallback; const keys=Object.keys(parsed); if(!keys.length) return fallback; const firstKey=keys[0]; const val=parsed[firstKey]; let firstVal=''; if(Array.isArray(val)) firstVal=val.slice(0,3).join(' / '); else if(typeof val==='string') firstVal=val.substring(0,40); else if(val&&typeof val==='object') firstVal=JSON.stringify(val).substring(0,40); return firstVal||fallback }
async function fetchDrafts(){
  if(loadingDrafts.value) return;
  loadingDrafts.value=true;
  draftError.value='';
  try {
    const csrfToken=await getCsrfToken();
    // 后端不支持 GET /api/report/draft，列表应请求 /api/report/draft/all (无 id -> list mode)
    const filter = selectedType.value!=="none" ? ('?templateType='+encodeURIComponent(selectedType.value)) : '';
    const res=await fetch('/api/report/draft/all'+filter,{ credentials:'include', headers:{'X-CSRF-TOKEN': csrfToken} });
    if(!res.ok){
      draftError.value='HTTP '+res.status;
      drafts.value=[];
      if(res.status===405) triggerToast('草稿列表接口方式错误(405)：请确认后端仅支持 /draft/all 列表',4000);
    } else {
      const data=await res.json();
      if(!data.success){
        draftError.value=data.message||'加载失败';
        drafts.value=[];
      } else {
        // list mode 返回: mode=list, drafts:[{id,updateTime,templateType}]
        drafts.value=(data.drafts||[]).map(d=>({
          id:d.id,
          updateTime:d.updateTime,
          updateTimeFmt:formatTime(d.updateTime),
          parsed:null,
          templateType:d.templateType||null,
          title:(d.templateType?('['+d.templateType+'] '):'')+'草稿 #'+d.id
        }))
      }
    }
  } catch(e){
    draftError.value='网络错误: '+e;
    drafts.value=[];
  }
  loadingDrafts.value=false;
}
async function fetchDraftDetail(id){ const csrfToken=await getCsrfToken(); const res=await fetch('/api/report/draft/all?id='+encodeURIComponent(id), { credentials:'include', headers:{'X-CSRF-TOKEN': csrfToken} }); if(!res.ok) throw new Error('HTTP '+res.status); const data=await res.json(); if(!data.success) throw new Error(data.message||'加载失败'); if(data.mode!=='detail'||!data.draft) throw new Error('返回数据格式不正确'); return data.draft }
function onTemplateLoaded(url){
  if(pendingFillTimer){ clearTimeout(pendingFillTimer); pendingFillTimer=null }
  const inst=resolveViewer();
  if(pendingFill && inst?.fillForm){
    try {
      inst.fillForm(pendingFill.data);
      triggerToast('已加载草稿 #'+pendingFill.draftId);
    } catch(e){
      triggerToast('填充失败: '+e, 3000);
    }
    showDraftMenu.value=false;
    pendingFill=null;
    updateSnapshot();
  }
}
async function applyDraft(d){
  if(!d || applyingDraftId.value) return; applyingDraftId.value = d.id;
  try {
    if(!d.parsed){ const detail=await fetchDraftDetail(d.id);
      let parsed=null; try { parsed= detail.taskUrl ? JSON.parse(detail.taskUrl) : null } catch(err){ console.warn('parse taskUrl failed', err) }
      d.parsed=parsed;
      if(detail.aiContextUrl){
        try {
          const ctx = parseMaybeCompressed(detail.aiContextUrl);
          if(Array.isArray(ctx)) {
            chatHistory.value = ctx;
            const deep = resolveDeepseek();
            if(deep?.restoreFromChatContext) {
              deep.restoreFromChatContext(ctx);
            }
          }
        } catch(err){ console.warn('parse aiContext failed', err) }
      }
      d.title=buildDraftTitle(parsed,d.title);
      if(!d.templateType && detail.templateType) d.templateType=detail.templateType;
      if(!d.templateType && parsed && parsed.templateType) d.templateType=parsed.templateType;
    }
    const originalTemplateType = d.templateType || '(none)';
    const needSwitch = !!(d.templateType && !templateLocked.value && (selectedType.value==='none' || selectedType.value!==d.templateType));
    let targetUrl=currentTemplateUrl.value;
    if(needSwitch){
      const meta=templates.value.find(t=>t.templateType===d.templateType);
      if(meta){
        selectedType.value=d.templateType;
        currentTemplateUrl.value=meta.url;
        targetUrl=meta.url;
        if(!templateLocked.value) templateLocked.value=true;
      } else {
        console.warn('[applyDraft] templateType missing; direct fill');
      }
    }
    if(!d.parsed){ triggerToast('草稿内容为空或解析失败',3000); return }
    if(!needSwitch){
      const inst=resolveViewer();
      if(inst?.fillForm){
        inst.fillForm(d.parsed);
        triggerToast('已加载草稿 #'+d.id);
        currentDraftId.value = d.id;
        showDraftMenu.value=false;
        updateSnapshot();
        return;
      } else {
        doFillDraft(d.parsed, d.id, '已加载草稿 #'+d.id+'(延迟)');
        currentDraftId.value = d.id;
        return;
      }
    }
    pendingFill={ data:d.parsed, draftId:d.id };
  currentDraftId.value = d.id;
    if(pendingFillTimer){ clearTimeout(pendingFillTimer) }
    // fallback: 如果 4s 内没触发 template-loaded 事件，尝试直接填充一次
    pendingFillTimer = setTimeout(()=>{
      if(!pendingFill) return;
      const inst2=resolveViewer();
      if(inst2?.fillForm){
        try{ jsonViewerRef.value.fillForm(pendingFill.data); triggerToast('已加载草稿 #'+pendingFill.draftId+'(超时回填)'); showDraftMenu.value=false; updateSnapshot(); }
        catch(e){ console.error('fallback fill failed', e); triggerToast('超时回填失败: '+e,3000) }
        pendingFill=null;
      }
    }, 4000);
    // race-safe second check removed; immediate path handled earlier
  } catch(e){
    triggerToast('加载异常: '+e,3000);
  } finally {
    applyingDraftId.value=null;
  }
}

// toast via composable
const { show: showToast, message: toastMessage, toast: triggerToast, close: closeToast } = useToast()

// deepseek
const deepseekRef = ref(null)
const chatHistory = ref([])
const templateSchema = computed(()=> resolveViewer()?.getTemplateSchema?.() || null)
// 实时获取当前表单数据（浅拷贝）供 DeepseekTool 参考
const currentFormData = computed(()=>{
  const inst = resolveViewer();
  if(!inst?.getFormData) return null;
  try {
    const data = inst.getFormData();
    if(!data || typeof data !== 'object') return null;
    // 仅传输纯数据对象，避免响应式污染
    return JSON.parse(JSON.stringify(data));
  } catch(_) { return null }
})
function resolveDeepseek(){ let inst=deepseekRef.value; return Array.isArray(inst)?inst[0]:inst||null }
function onDeepseekFill(parsed){
  const inst = resolveViewer();
  if(!inst?.fillForm || !inst?.getFormData || !parsed || typeof parsed!=='object') return;
  const current = inst.getFormData();
  const filtered = {};
  let skipped = 0;
  const isEmptyAiValue = (v)=>{
    if(v==null) return true;
    if(Array.isArray(v)) return v.map(x=> (''+x).trim()).filter(Boolean).length===0;
    if(typeof v==='string') return v.trim()==='';
    return false; // 其它类型(数字等)视为非空
  };
  Object.entries(parsed).forEach(([k,v])=>{
    if(!(k in current)) return; // 不在表单中
    if(isEmptyAiValue(v)) { skipped++; return; }
    filtered[k]=v;
  });
  if(Object.keys(filtered).length===0){
    triggerToast('AI 内容为空或均被跳过，未修改');
    return;
  }
  try {
    inst.fillForm(filtered);
    triggerToast('已填充 AI 内容 (跳过空字段 '+skipped+')');
    updateSnapshot();
  } catch(e){
    triggerToast('AI 填充失败: '+e, 3000);
  }
}
function onDeepseekChatUpdate(history){ chatHistory.value=[...history] }
function onDeepseekError(msg){ triggerToast('AI 错误: '+msg, 3000) }

// save draft
async function saveDraft(){
  const inst=resolveViewer();
  const form=inst?.getFormData?.() || null;
  if(!form){ triggerToast('表单数据获取失败！',2200); return }
  const deep=resolveDeepseek();
  const chatCtx = deep?.getChatHistory?.() || chatHistory.value || [];
  const compressed = compressChatHistory(chatCtx) || JSON.stringify(chatCtx);
  const draftData={ id: currentDraftId.value || undefined, templateType: selectedType.value!=='none'?selectedType.value:null, ...form, aiContextUrl: compressed };
  try {
    const csrfToken=await getCsrfToken();
    const res=await fetch('/api/report/draft',{
      method:'POST',
      headers:{'Content-Type':'application/json','X-CSRF-TOKEN':csrfToken},
      body: JSON.stringify(draftData),
      credentials:'include'
    });
    if(res.ok){
      let data=null; try { data=await res.json() } catch(_){}
      // 后端如果返回新草稿 id 或更新时间，尝试刷新本地状态
      if(data && data.success){
        if(data.id && !currentDraftId.value){
          currentDraftId.value = data.id;
        }
        // 更新列表条目（若已存在）
        if(currentDraftId.value){
          const item = drafts.value.find(d=>d.id===currentDraftId.value);
          if(item){
            const now = new Date();
            const pad=n=>String(n).padStart(2,'0');
            const fmt = `${now.getFullYear()}-${pad(now.getMonth()+1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
            item.updateTimeFmt = fmt;
          }
        }
      }
      triggerToast(currentDraftId.value?('草稿已覆盖保存 #'+currentDraftId.value):'草稿已保存！（压缩）');
      updateSnapshot();
    }
    else { triggerToast('保存失败：'+res.status,3000) }
  } catch(e){ triggerToast('网络错误：'+e,3200)}
}

function scrollToEditor(){ const el=document.querySelector('.assist-container'); if(el) el.scrollIntoView({behavior:'smooth', block:'start'}); else window.scrollTo({ top:0, behavior:'smooth'}) }

onMounted(()=>{ loadTemplates(); observeMainTopBar(); loadLayoutFromStorage(); document.addEventListener('click', onDocumentClick); document.addEventListener('keydown', onDocumentKey) })
onBeforeUnmount(()=>{ if(mainBarObserver) mainBarObserver.disconnect(); document.removeEventListener('click', onDocumentClick); document.removeEventListener('keydown', onDocumentKey) })
</script>
<script>
export default { directives: { dragSort } }
</script>
<style scoped>
/* assist-wrapper: 去掉额外 padding-top，使用 sticky 内部栏占位。 */
.assist-wrapper { position: relative; }
/* 内部实验助手工具栏：sticky 贴在全局导航(60px)下方 */
.top-bar { position: sticky; top: 60px; height: 54px; display: flex; align-items: center; justify-content: space-between; padding: 0 var(--space-5); background: var(--color-backdrop-blur-bg); backdrop-filter: saturate(180%) blur(12px); border-bottom: 1px solid var(--color-border); box-shadow: var(--shadow-md); z-index: var(--z-assist-bar); }
.top-bar-left { font-size: var(--font-size-md); color: var(--color-text-primary); }
.top-bar-center { flex: 1; text-align: center; }
.top-select { padding: 6px 10px; font-size: var(--font-size-base); border-radius: var(--radius-sm); border: 1px solid var(--color-border); background: var(--color-surface); }
.template-chip { display:inline-block; background: var(--color-chip-bg); color: var(--color-chip-text); padding: 6px 14px; border-radius: 20px; font-size: var(--font-size-sm); box-shadow: var(--shadow-sm); }
.template-chip-inline { display:inline-block; background: var(--color-chip-bg); color: var(--color-chip-text); padding:4px 10px; border-radius:16px; font-size: var(--font-size-sm); }
.top-bar-right { display:flex; gap: var(--space-3); }
.top-btn { padding: 6px 14px; background: var(--color-primary); color:#fff; border:none; border-radius: var(--radius-sm); cursor:pointer; font-size: var(--font-size-sm); box-shadow: var(--shadow-sm); transition: background var(--transition-base); }
.top-btn:hover { background: var(--color-primary-hover); }
.draft-dropdown-wrapper { position: relative; }
.draft-menu { position: absolute; top: 40px; right: 0; width: 340px; max-height: 420px; background: var(--color-surface); border:1px solid var(--color-border); border-radius: var(--radius-md); box-shadow: var(--shadow-pop); padding: var(--space-2) var(--space-3); display:flex; flex-direction:column; gap: var(--space-2); animation: fadeIn .2s ease; }
.draft-menu-header { display:flex; justify-content:space-between; align-items:center; font-size: var(--font-size-sm); padding:2px 4px 6px; border-bottom:1px solid var(--color-gray-100); }
.refresh-btn { background: var(--color-gray-100); border:1px solid var(--color-border); border-radius: var(--radius-xs); padding:2px 8px; font-size: var(--font-size-xs); cursor:pointer; }
.refresh-btn:hover { background: var(--color-gray-150); }
.draft-error { color: var(--color-red-500); font-size: var(--font-size-xs); padding: var(--space-2) 4px; }
.draft-empty { font-size: var(--font-size-xs); color: var(--color-text-secondary); padding: var(--space-2) 4px; }
.draft-list { list-style:none; margin:0; padding:0; overflow-y:auto; flex:1; }
.draft-item { padding:6px; border-radius: var(--radius-sm); cursor:pointer; display:flex; flex-direction:column; gap:2px; border:1px solid transparent; transition: background var(--transition-fast), border-color var(--transition-fast); }
.draft-item:hover { background: var(--color-gray-50); border-color: var(--color-gray-200); }
.draft-item.applying { opacity:.65; pointer-events:none; }
.draft-title { font-size: var(--font-size-xs); color: var(--color-text-primary); white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.draft-time { font-size: var(--font-size-xs); color: var(--color-text-secondary); }
.draft-spinner { display:inline-block; width:10px; height:10px; border:2px solid var(--color-primary); border-right-color:transparent; border-radius:50%; margin-right:4px; animation: spin .6s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
@keyframes fadeIn { from { opacity:0; transform:translateY(-4px);} to { opacity:1; transform:translateY(0);} }
.assist-container { width: 90%; margin: 20px auto 60px; padding: var(--space-8); background: var(--color-surface); border-radius: var(--radius-md); box-shadow: var(--shadow-lg); text-align: center; }
/* Unified textarea style for experimentNameDetail and viewer textareas */
#experimentNameDetail, .unified-textarea {
  width: 100%;
  max-width: 1100px;
  min-height: 120px;
  font-size: 15px;
  line-height: 1.55;
  padding: 12px 14px;
  border: 1.5px solid #b3c2d4;
  border-radius: 8px;
  background: #f8fbff;
  box-shadow: 0 1px 4px #e0e7ef inset;
  resize: vertical;
  transition: border-color .2s, box-shadow .2s;
  font-family: inherit;
  box-sizing: border-box;
}
#experimentNameDetail:focus, .unified-textarea:focus {
  outline: none;
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64,158,255,0.25);
  background: #fff;
}
/* New draggable layout styles */
.assist-header { display:flex; justify-content:space-between; align-items:flex-end; margin-bottom: var(--space-6); text-align:left; }
.assist-header h1 { margin:0 0 var(--space-2); font-size: 1.55rem; font-weight: var(--font-weight-semibold); }
.assist-intro { margin:0; font-size: var(--font-size-sm); color: var(--color-text-secondary); }
.assist-header-actions { display:flex; gap: var(--space-3); }
.layout-toggle-btn { padding:6px 14px; background: var(--color-secondary); color:#fff; border:none; border-radius: var(--radius-sm); cursor:pointer; font-size: var(--font-size-sm); box-shadow: var(--shadow-sm); }
.layout-toggle-btn:hover { background: var(--color-secondary-hover); }

.assist-layout { display:flex; flex-direction:column; gap: var(--space-6); width:100%; }
.assist-layout.mode-horizontal { flex-direction:row; align-items:stretch; flex-wrap: wrap; }
.assist-layout.mode-horizontal .assist-block { flex:1 1 calc(50% - var(--space-4)); max-width: calc(50% - var(--space-4)); }
/* 水平模式下：如果该行只有一个 block（总数为1或最后落单），让它占满整行 */
.assist-layout.mode-horizontal .assist-block:only-child,
.assist-layout.mode-horizontal .assist-block:last-child:nth-child(2n+1) {
  flex: 1 1 80%;
  max-width: 95%;
  margin-left: auto;
  margin-right: auto;
}

.assist-block { position:relative; background: var(--color-gray-75); border:1px solid var(--color-gray-150); border-radius: var(--radius-lg); box-shadow: var(--shadow-md); display:flex; flex-direction:column; min-height:260px; transition: box-shadow var(--transition-fast), transform var(--transition-fast), border-color var(--transition-fast); }
.assist-block.dragging { opacity:.85; box-shadow: var(--shadow-pop); border-color: var(--color-primary); }
.assist-block.over { outline:2px dashed var(--color-primary); outline-offset: 2px; }
.block-header { cursor:grab; user-select:none; padding: var(--space-3) var(--space-4); display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid var(--color-gray-150); background: linear-gradient(135deg,var(--color-gray-100), var(--color-gray-50)); border-top-left-radius: inherit; border-top-right-radius: inherit; }
.assist-block.dragging .block-header { cursor:grabbing; }
.block-title { font-size: var(--font-size-sm); font-weight: var(--font-weight-medium); color: var(--color-text-primary); }
.drag-hint { font-size: var(--font-size-sm); color: var(--color-text-tertiary); }
.block-body { padding: var(--space-4); text-align:left; overflow:auto; }
.viewer-wrapper { text-align:left; }
.placeholder { font-size: var(--font-size-sm); color: var(--color-text-secondary); padding: var(--space-3); }

/* Deepseek panel */
.deepseek-panel h2 { margin:0 0 var(--space-3); font-size:1.1rem; }
.prompt-input { width:100%; resize:vertical; border:1px solid var(--color-border); border-radius: var(--radius-sm); padding: var(--space-3); font-family: inherit; font-size: var(--font-size-sm); background: var(--color-surface); }
.prompt-input:focus { outline:none; border-color: var(--color-primary); box-shadow:0 0 0 2px rgba(59,130,246,0.15); }
.deepseek-actions { display:flex; gap: var(--space-3); margin: var(--space-2) 0 var(--space-3); }
.run-btn { background: var(--color-primary-alt); }
.run-btn:hover { background: var(--color-primary-alt-hover); }
.test-result { background: var(--color-blue-50); border-radius: var(--radius-sm); padding: var(--space-3); margin-top: var(--space-2); font-size: var(--font-size-xs); white-space: pre-wrap; word-break: break-all; overflow-wrap: break-word; max-width: 100%; box-sizing: border-box; }
.test-error { margin-top: var(--space-2); font-size: var(--font-size-xs); }
.error-text { color: var(--color-red-500); }

/* Scroll adjustments for horizontal layout */
.assist-layout.mode-horizontal { gap: var(--space-4); }
.assist-layout.mode-horizontal .block-body { max-height: 640px; }

@media (max-width: 1180px) {
  .assist-layout.mode-horizontal { flex-direction:column; }
}
.toast { position: fixed; top: 70px; right: 28px; background: var(--color-gray-900); color: #fff; padding: 12px 18px; border-radius: var(--radius-md); box-shadow: var(--shadow-pop); font-size: var(--font-size-base); display: flex; align-items: center; gap: var(--space-2); z-index: var(--z-toast); animation: pop-in .35s ease; }
.toast-success { background: var(--color-green-500); }
.toast-error { background: var(--color-red-500); }
.toast-close { background: transparent; border: none; color: #fff; font-size: 16px; cursor: pointer; padding: 0 4px; line-height: 1; }
.toast-fade-enter-from, .toast-fade-leave-to { opacity: 0; transform: translateY(-6px); }
.toast-fade-enter-active, .toast-fade-leave-active { transition: all .25s ease; }
@keyframes pop-in { from { transform: translateY(-8px) scale(.96); opacity:0; } to { transform: translateY(0) scale(1); opacity:1; } }
.modal-mask { position: fixed; z-index: var(--z-modal); top:0; left:0; right:0; bottom:0; background:rgba(0,0,0,0.35); display:flex; align-items:center; justify-content:center; }
.modal-container { width:380px; background: var(--color-surface); border-radius: var(--radius-lg); box-shadow: var(--shadow-modal); animation: pop-in .28s ease; overflow:hidden; }
.modal-header { font-weight: var(--font-weight-semibold); padding:14px 18px; border-bottom:1px solid var(--color-gray-150); }
.modal-body { padding:18px; font-size: var(--font-size-base); line-height:1.55; color: var(--color-gray-800); }
.modal-footer { padding:12px 18px 18px; display:flex; justify-content:flex-end; gap: var(--space-3); }
.modal-btn { padding:6px 16px; border-radius: var(--radius-sm); border:1px solid transparent; font-size: var(--font-size-sm); cursor:pointer; transition: background var(--transition-fast); }
.modal-btn.secondary { background: var(--color-gray-100); border-color: var(--color-border); }
.modal-btn.secondary:hover { background: var(--color-gray-150); }
.modal-btn.primary { background: var(--color-primary-alt); color:#fff; }
.modal-btn.primary:hover { background: var(--color-primary-alt-hover); }
</style>
