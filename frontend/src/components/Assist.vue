<template>
  <div class="assist-wrapper">
  <div class="top-bar" :style="assistBarStyle">
      <div class="top-bar-left"><strong>实验助手</strong></div>
      <div class="top-bar-center">
        <template v-if="!templateLocked">
          <select v-model="selectedType" class="top-select">
            <option value="none">选择报告类型</option>
            <option v-for="t in templates" :key="t.templateCode" :value="t.templateCode">{{ t.templateName || ('模板-'+t.templateCode) }}</option>
          </select>
        </template>
        <span v-else class="template-chip">模板：{{ currentTemplateName }}</span>
      </div>
      <div class="top-bar-right">
  <button class="top-btn" @click="openResetConfirm">重新编辑</button>
  <button v-if="selectedType !== 'none'" class="top-btn" @click="saveDraft">保存草稿</button>
        <div v-if="selectedType !== 'none'" class="submit-dropdown-wrapper" ref="submitWrapper">
          <button class="top-btn submit-btn" :disabled="submitting || eligibleTasks.length===0" @click="toggleSubmitMenu">
            {{ submitting? '提交中...' : (eligibleTasks.length>0? '提交任务 ('+eligibleTasks.length+')' : '无可提交任务') }}
          </button>
          <div v-if="showSubmitMenu" class="submit-menu" @keydown.esc.stop="showSubmitMenu=false">
            <div class="submit-menu-header">
              <span>可提交任务</span>
              <button class="refresh-btn" @click.stop="refreshUserInfo" :disabled="submitting">刷新</button>
            </div>
            <div v-if="eligibleTasks.length===0" class="submit-empty">暂无符合条件任务</div>
            <ul v-else class="submit-list">
              <li v-for="t in eligibleTasks" :key="t.task.id" class="submit-item" :class="{ pending: submittingTaskId===t.task.id }">
                <div class="submit-task-main">
                  <div class="submit-title" :title="t.task.title">{{ t.task.title || ('任务 #'+t.task.id) }}</div>
                  <div class="submit-status">状态: {{ t.status||'无' }}</div>
                </div>
                <button class="submit-action-btn" :disabled="submitting" @click.stop="submitSpecificTask(t)">
                  <span v-if="submittingTaskId===t.task.id">...</span>
                  <span v-else>{{ isCompletedStatus(t.status)? '覆盖' : '提交' }}</span>
                </button>
              </li>
            </ul>
          </div>
        </div>
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
        <div class="work-dropdown-wrapper" ref="workWrapper">
          <button class="top-btn" @click="toggleWorkMenu">继续工作</button>
          <div v-if="showWorkMenu" class="work-menu" @keydown.esc.stop="showWorkMenu=false">
            <div class="work-menu-header">
              <span>已绑定任务提交</span>
              <button class="refresh-btn" @click.stop="fetchWorkItems" :disabled="loadingWork">{{ loadingWork ? '...' : '刷新' }}</button>
            </div>
            <div v-if="workError" class="work-error">{{ workError }}</div>
            <div v-else-if="workItems.length===0" class="work-empty">暂无数据</div>
            <ul v-else class="work-list">
              <li v-for="w in workItems" :key="w.id" class="work-item" :class="{ applying: applyingWorkId===w.id }" @click="applyWork(w)">
                <div class="work-title" :title="w.taskTitle">{{ w.taskTitle || ('任务#'+w.taskId) }}</div>
                <div class="work-meta">更新时间: {{ w.updateTimeFmt }}</div>
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
    <button class="layout-toggle-btn icon-only" @click="toggleLayoutMode"
      :aria-label="layoutMode==='vertical' ? '切换为水平布局' : '切换为垂直布局'"
      :title="layoutMode==='vertical' ? '切换为水平布局' : '切换为垂直布局'">
            <span class="icon-wrapper" aria-hidden="true">
              <svg v-if="layoutMode==='vertical'" class="icon icon-filled" viewBox="0 0 24 24" aria-hidden="true">
                <rect x="3" y="4" width="7" height="6" rx="1"/>
                <rect x="14" y="4" width="7" height="6" rx="1"/>
                <rect x="3" y="14" width="7" height="6" rx="1"/>
                <rect x="14" y="14" width="7" height="6" rx="1"/>
              </svg>
              <svg v-else class="icon icon-filled" viewBox="0 0 24 24" aria-hidden="true">
                <rect x="4" y="3" width="16" height="5" rx="1"/>
                <rect x="4" y="10" width="16" height="5" rx="1"/>
                <rect x="4" y="17" width="16" height="5" rx="1"/>
              </svg>
            </span>
            <!-- 文本隐藏，仅保留可访问名称 -->
          </button>
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
              <DocGenBlock ref="docGenRef" :form-data="resolveViewer()?.getFormData?.() || null" :template-code="selectedType !== 'none' ? Number(selectedType) : null" @toast="triggerToast" />
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
    <div v-if="showUnsavedConfirm || showResetConfirm" class="modal-mask">
      <div class="modal-container">
        <div class="modal-header">{{ resetModalMode==='unsaved' ? '提示' : '重新编辑确认' }}</div>
        <div class="modal-body" v-if="resetModalMode==='unsaved'">当前内容尚未保存，继续将丢失未保存的修改，是否继续？</div>
        <div class="modal-body" v-else>
          该操作将恢复到初始状态：
          <ul style="margin:8px 0 0 18px; font-size:13px; line-height:1.5;">
            <li>清除当前表单内容</li>
            <li>解除模板锁定并重置为“未选择”</li>
            <li>清空聊天上下文与草稿挂载状态</li>
            <li>关闭所有下拉、弹窗状态</li>
          </ul>
          是否确认继续？
        </div>
        <div class="modal-footer" v-if="resetModalMode==='unsaved'">
          <button class="modal-btn secondary" @click="cancelProceed">取消</button>
          <button class="modal-btn primary" @click="confirmProceed">继续</button>
        </div>
        <div class="modal-footer" v-else>
          <button class="modal-btn secondary" @click="closeResetConfirm">取消</button>
            <button class="modal-btn primary" @click="confirmReset">确认重置</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useDropdown } from '../composables/useDropdown'
import dragSort from '../directives/dragsort.js'
import JsonViewer from '../template/JsonViewer.vue'
import DeepseekTool from '../template/DeepseekTool.vue'
import DocGenBlock from '../template/DocGenBlock.vue'
import http from '@/api/http'
import { compressChatHistory, parseMaybeCompressed } from '../utils/chatCompress.js'
import { useToast } from '../composables/useToast.js'
import { useAssistLayout } from '../composables/useAssistLayout.js'

// layout via composable
const { blocks, layoutMode, load: loadLayoutFromStorage, toggle: toggleLayoutMode, onBlocksReordered } = useAssistLayout()

// template selection
const jsonViewerRef = ref(null)
const selectedType = ref('none') // 现在存储 templateCode (字符串形式) 或 'none'
const templateLocked = ref(false)
const templates = ref([])
const currentTemplateUrl = ref('')
const currentTemplateName = computed(()=>{ if(selectedType.value==='none') return '未选择'; const meta=templates.value.find(t=>String(t.templateCode)===String(selectedType.value)); return meta ? (meta.templateName||('模板-'+meta.templateCode)) : selectedType.value })
const globalBarVisible = ref(true)
let mainBarObserver = null
const assistBarStyle = computed(()=>({ top: (globalBarVisible.value?60:0)+'px' }))
function observeMainTopBar(){ const header=document.querySelector('header.top-bar'); if(!header){ globalBarVisible.value=false; return } mainBarObserver=new IntersectionObserver(entries=> entries.forEach(e=> globalBarVisible.value=e.isIntersecting), { threshold:[0,0.01,1]}); mainBarObserver.observe(header) }
async function loadTemplates(){
  try {
    const { data } = await http.get('/api/report/templates')
    if(data && data.success){
      templates.value=(data.templates||[]).map(t=>({
        ...t,
        templateCode: t.templateCode != null ? t.templateCode : (t.templateType ? t.templateType : null)
      })).filter(t=>t.templateCode!=null)
    }
  } catch(_){}
}
watch(selectedType,(val)=>{ const meta=templates.value.find(t=>String(t.templateCode)===String(val)); if(meta){ currentTemplateUrl.value=meta.url; if(val!=='none' && !templateLocked.value) templateLocked.value=true } else if(val==='none') currentTemplateUrl.value='' })

// drafts
const drafts = ref([])
const draftWrapper = ref(null)
const { show: showDraftMenu, open: openDraftMenu, close: closeDraftMenu, toggle: toggleDraftMenuRaw } = useDropdown({
  wrapperRef: draftWrapper,
  beforeOpen: ()=>{ if(hasUnsavedContent()){ pendingOpenDraftMenu=true; showUnsavedConfirm.value=true; return false } return true },
  onOpen: ()=> fetchDrafts()
})
const draftError = ref('')
const loadingDrafts = ref(false)
const applyingDraftId = ref(null)
// continue work
const workWrapper = ref(null)
const { show: showWorkMenu, toggle: toggleWorkMenu } = useDropdown({
  wrapperRef: workWrapper,
  beforeOpen: ()=>{ if(hasUnsavedContent()){ triggerToast('请先保存草稿或确认放弃再继续其他工作'); return false } return true },
  onOpen: ()=> fetchWorkItems()
})
const workItems = ref([])
const workError = ref('')
const loadingWork = ref(false)
const applyingWorkId = ref(null)
// 当前正在编辑的草稿 id；应用某草稿后设置，用于后续保存覆盖
const currentDraftId = ref(null)
// draftWrapper already declared above for dropdown composable
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
function toggleDraftMenu(){ toggleDraftMenuRaw() }
// removed individual document click handlers (handled by composable)
function onDocumentKey(e){ if(e.key==='Escape' && showDraftMenu.value) showDraftMenu.value=false }
function formatTime(str){ if(!str) return ''; try { return str.replace('T',' ').substring(0,19) } catch(_) { return str } }
function buildDraftTitle(parsed,fallback){ if(!parsed||typeof parsed!=='object') return fallback; const keys=Object.keys(parsed); if(!keys.length) return fallback; const firstKey=keys[0]; const val=parsed[firstKey]; let firstVal=''; if(Array.isArray(val)) firstVal=val.slice(0,3).join(' / '); else if(typeof val==='string') firstVal=val.substring(0,40); else if(val&&typeof val==='object') firstVal=JSON.stringify(val).substring(0,40); return firstVal||fallback }
async function fetchDrafts(){
  if(loadingDrafts.value) return;
  loadingDrafts.value=true;
  draftError.value='';
  try {
    // 后端不支持 GET /api/report/draft，列表应请求 /api/report/draft/all (无 id -> list mode)
    const filter = selectedType.value!=="none" ? ('?templateCode='+encodeURIComponent(selectedType.value)) : '';
    try {
      const { data } = await http.get('/api/report/draft/all'+filter)
      if(!data.success){
        draftError.value=data.message||'加载失败';
        drafts.value=[];
      } else {
        drafts.value=(data.drafts||[]).map(d=>({
          id:d.id,
          updateTime:d.updateTime,
          updateTimeFmt:formatTime(d.updateTime),
          parsed:null,
          templateCode:(d.templateCode!=null?d.templateCode:(d.templateType!=null?d.templateType:null)),
          title:((d.templateCode!=null)?('[C'+d.templateCode+'] '):'')+'草稿 #'+d.id
        }))
      }
    } catch(err){
      draftError.value='网络错误: '+err;
      drafts.value=[];
    }
  } catch(e){
    draftError.value='网络错误: '+e;
    drafts.value=[];
  }
  loadingDrafts.value=false;
}
// ====== Continue Work related methods ======
async function fetchWorkItems(){
  if(loadingWork.value) return;
  loadingWork.value=true; workError.value=''; workItems.value=[];
  try {
    let data;
    try {
      const resp = await http.get('/api/task-submissions/user')
      data = resp.data
    } catch(e){ workError.value='网络错误: '+e; loadingWork.value=false; return }
    if(!data.success){ workError.value = data.message || '加载失败'; loadingWork.value=false; return }
    // submissions: 过滤 taskId != null
    const list = (data.submissions||[]).filter(s=> s.taskId != null);
    workItems.value = list.map(s=>({
      id: s.id,
      taskId: s.taskId,
      taskTitle: s.task && s.task.title ? s.task.title : ('任务#'+s.taskId),
      updateTime: s.updateTime,
      updateTimeFmt: formatTime(s.updateTime),
      aiContextUrl: s.aiContextUrl,
      templateCode: s.templateCode,
      raw: s
    }))
  } catch(e){ workError.value='网络错误: '+e }
  loadingWork.value=false;
}
async function applyWork(w){
  if(!w || applyingWorkId.value) return; applyingWorkId.value = w.id;
  try {
    // 拉取该任务的当前用户提交详情，期望字段: submission.taskUrl (表单 JSON), aiContextUrl (聊天上下文)
    let data;
    try {
      const resp = await http.get('/api/task-submissions/user/task/'+encodeURIComponent(w.taskId))
      data = resp.data
    } catch(e){ triggerToast('加载失败: '+e,3000); return }
    if(!data || !data.success || !data.submission){ triggerToast('未找到可恢复内容',3000); return }
    const sub = data.submission;
    // 选择模板（若不同）
    if(sub.templateCode!=null){
      if(selectedType.value==='none' || String(selectedType.value)!==String(sub.templateCode)){
        const meta=templates.value.find(t=>String(t.templateCode)===String(sub.templateCode));
        if(meta){ selectedType.value=String(sub.templateCode); currentTemplateUrl.value=meta.url; if(!templateLocked.value) templateLocked.value=true }
      }
    }
    // 解析表单 JSON（taskUrl 存储的是 JSON 字符串）
    let parsed=null; if(sub.taskUrl){ try { parsed= JSON.parse(sub.taskUrl) } catch(e){ console.warn('解析 taskUrl 失败', e) } }
    // 恢复聊天记录
    if(sub.aiContextUrl){
      try {
        const ctx = parseMaybeCompressed(sub.aiContextUrl);
        if(Array.isArray(ctx)){
          chatHistory.value = ctx;
          const deep = resolveDeepseek();
          if(deep?.restoreFromChatContext) deep.restoreFromChatContext(ctx);
        }
      } catch(e){ console.warn('解析聊天上下文失败', e) }
    }
    if(parsed){
      const inst=resolveViewer();
      if(inst?.fillForm){
        try { inst.fillForm(parsed); triggerToast('已恢复: '+ (w.taskTitle||('#'+w.taskId))); updateSnapshot(); }
        catch(e){ triggerToast('回填失败: '+e,3000) }
      } else {
        // 模板尚未加载，利用 pendingFill 机制
        pendingFill={ data: parsed, draftId: 'work-'+sub.id };
        if(pendingFillTimer){ clearTimeout(pendingFillTimer) }
        pendingFillTimer = setTimeout(()=>{
          if(!pendingFill) return;
          const inst2=resolveViewer();
          if(inst2?.fillForm){
            try{ inst2.fillForm(pendingFill.data); triggerToast('已恢复(延迟): '+ (w.taskTitle||('#'+w.taskId))); updateSnapshot(); }
            catch(err){ triggerToast('延迟回填失败: '+err,3000) }
            pendingFill=null;
          }
        },4000);
      }
    } else {
      triggerToast('该提交未包含可解析内容',3000);
    }
    showWorkMenu.value=false;
  } catch(e){ triggerToast('加载异常: '+e,3000) }
  finally { applyingWorkId.value=null }
}
async function fetchDraftDetail(id){
  const { data } = await http.get('/api/report/draft/all?id='+encodeURIComponent(id))
  if(!data.success) throw new Error(data.message||'加载失败')
  if(data.mode!=='detail'||!data.draft) throw new Error('返回数据格式不正确')
  return data.draft
}
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
      // 兼容：旧草稿内可能存储 templateType；新结构使用 templateCode
      if(d.templateCode==null){
        if(detail.templateCode!=null) d.templateCode=detail.templateCode;
        else if(detail.templateType!=null) d.templateCode=detail.templateType; // 后端旧字段
        else if(parsed && (parsed.templateCode!=null || parsed.templateType!=null)) d.templateCode = parsed.templateCode!=null?parsed.templateCode:parsed.templateType;
      }
    }
    const originalTemplateCode = d.templateCode ?? '(none)';
    const needSwitch = !!(d.templateCode!=null && !templateLocked.value && (selectedType.value==='none' || String(selectedType.value)!==String(d.templateCode)));
    let targetUrl=currentTemplateUrl.value;
    if(needSwitch){
      const meta=templates.value.find(t=>String(t.templateCode)===String(d.templateCode));
      if(meta){
        selectedType.value=String(d.templateCode);
        currentTemplateUrl.value=meta.url;
        targetUrl=meta.url;
        if(!templateLocked.value) templateLocked.value=true;
      } else {
        console.warn('[applyDraft] templateCode missing; direct fill');
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
const docGenRef = ref(null)
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
// === 提交相关状态 ===
const submitting = ref(false)
const userInfo = ref(null) // /api/user/me 返回
const userTasks = computed(()=> (userInfo.value && Array.isArray(userInfo.value.userTasks)) ? userInfo.value.userTasks : [])
// 可提交条件：
// 1) 已选择模板
// 2) 用户任务里存在某条 task.templateCode == selectedType (数字比较)
// 3) 该 userTask.status 未标记为 已提交/完成（假设使用 'completed' 或 'done'，这里先排除 completed）
// 4) 当前表单非空
const matchingUserTask = computed(()=>{
  if(selectedType.value==='none') return null;
  const code = Number(selectedType.value)
  // userTasks 结构：[{ id, status, task:{ id, templateCode, ... } }]
  return userTasks.value.find(ut => ut.task && ut.task.templateCode!=null && Number(ut.task.templateCode)===code) || null
})
function formIsMeaningful(form){ if(!form|| typeof form!=='object') return false; return Object.values(form).some(v=>{ if(Array.isArray(v)) return v.filter(x=>String(x).trim()).length>0; if(typeof v==='string') return v.trim().length>0; return false }) }
const canSubmitComputed = computed(()=>{
  if(selectedType.value==='none') return false;
  const inst = resolveViewer(); const form=inst?.getFormData?.();
  if(!formIsMeaningful(form)) return false;
  const ut = matchingUserTask.value;
  if(!ut) return false;
  if(!ut.status) return true; // 无状态则允许
  const disallow = ['completed','done','submitted'];
  return !disallow.includes(String(ut.status).toLowerCase());
})
async function fetchUserInfo(){
  try {
    const { data } = await http.get('/api/user/me')
    if(data) userInfo.value=data
  } catch(_){}
}
const showSubmitMenu = ref(false)
const submitWrapper = ref(null)
const submittingTaskId = ref(null)
function toggleSubmitMenu(){ showSubmitMenu.value = !showSubmitMenu.value }
function closeSubmitMenu(e){ if(!showSubmitMenu.value) return; if(submitWrapper.value && !submitWrapper.value.contains(e.target)) showSubmitMenu.value=false }
const eligibleTasks = computed(()=>{
  // 允许已完成再次提交：只排除明确禁止的 submitted (如果有)；其余显示
  if(selectedType.value==='none') return [];
  const code = Number(selectedType.value);
  const disallow = ['submitted']; // 保留一个真正锁定状态（可按需要调整）
  return userTasks.value
    .filter(ut=> ut.task && ut.task.templateCode!=null && Number(ut.task.templateCode)===code)
    .filter(ut=> !disallow.includes(String(ut.status||'').toLowerCase()))
})
async function submitSpecificTask(ut){
  if(submitting.value) return;
  const inst=resolveViewer(); const form=inst?.getFormData?.();
  if(!formIsMeaningful(form)){ triggerToast('请先填写内容后再提交'); return }
  if(isCompletedStatus(ut.status)){
    const ok = window.confirm('该任务已完成，继续操作将覆盖之前的提交，是否继续？');
    if(!ok) return;
  }
  submitting.value=true; submittingTaskId.value=ut.task.id;
  try {
  const deep=resolveDeepseek(); const chatCtx = deep?.getChatHistory?.() || chatHistory.value || [];
  const compressed = compressChatHistory(chatCtx) || JSON.stringify(chatCtx);
  const payload = { taskId: ut.task.id, templateCode: ut.task.templateCode ?? (selectedType.value!=='none'?Number(selectedType.value):null), submit: true, ...form, aiContextUrl: compressed };
  const { data } = await http.post('/api/report/draft', payload);
    if(!data || !data.success){ triggerToast('提交失败: '+(data&&data.message?data.message:'未知错误')); return }
    triggerToast('已提交任务: '+ (ut.task.title||('#'+ut.task.id)) );
    await fetchUserInfo();
    showSubmitMenu.value=false;
  } catch(e){ triggerToast('提交异常: '+e, 3000) }
  finally { submitting.value=false; submittingTaskId.value=null; }
}
async function refreshUserInfo(){ await fetchUserInfo(); }
function isCompletedStatus(status){ if(!status) return false; const s=String(status).toLowerCase(); return ['completed','done','已完成'].includes(s) }
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
  const draftData={ id: currentDraftId.value || undefined, templateCode: selectedType.value!=='none'?Number(selectedType.value):null, ...form, aiContextUrl: compressed };
  try {
  const { data } = await http.post('/api/report/draft', draftData);
    if(data && data.success){
      if(data.id && !currentDraftId.value) currentDraftId.value = data.id;
      if(currentDraftId.value){
        const item = drafts.value.find(d=>d.id===currentDraftId.value);
        if(item){
          const now = new Date();
          const pad=n=>String(n).padStart(2,'0');
          const fmt = `${now.getFullYear()}-${pad(now.getMonth()+1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
            item.updateTimeFmt = fmt;
        }
      }
      triggerToast(currentDraftId.value?('草稿已覆盖保存 #'+currentDraftId.value):'草稿已保存！');
      updateSnapshot();
    } else {
      triggerToast('保存失败',3000);
    }
  } catch(e){ triggerToast('网络错误：'+e,3200)}
}

// 删除草稿
async function deleteDraft(id){
  const ok = window.confirm('确认删除草稿 #' + id + ' ?');
  if(!ok) return;
  try {
  const { data } = await http.delete('/api/report/draft', { data:{ id } });
    if(!data.success){ triggerToast(data.message || '删除失败'); return }
    drafts.value = drafts.value.filter(d=>d.id!==id);
    if(currentDraftId.value===id) currentDraftId.value=null;
    triggerToast('已删除草稿 #'+id);
  } catch(e){ triggerToast('删除异常: '+e,3000) }
}

// ===== 重置(重新编辑)逻辑 =====
const showResetConfirm = ref(false)
const resetModalMode = ref('unsaved') // 'unsaved' | 'reset'
function openResetConfirm(){
  // 如果有未保存内容，先复用原未保存提示流程
  if(hasUnsavedContent()){
    resetModalMode.value='unsaved';
    showUnsavedConfirm.value=true;
    pendingOpenDraftMenu=false; // 与草稿打开无关
  } else {
    resetModalMode.value='reset';
    showResetConfirm.value=true;
  }
}
function closeResetConfirm(){ showResetConfirm.value=false }
function confirmReset(){
  // 全局重置：表单、模板、聊天、草稿状态、菜单开关
  try {
    // 1. 重置模板状态
    selectedType.value='none';
    templateLocked.value=false;
    currentTemplateUrl.value='';
    // 2. 清空表单 (JsonViewer 组件若提供 clear 接口; fallback: 重新挂载)
    const inst=resolveViewer();
    if(inst && inst.clearForm){ inst.clearForm(); }
    else if(inst && inst.fillForm){ inst.fillForm({}); }
    // 3. 清空聊天
    chatHistory.value=[];
    const deep=resolveDeepseek();
    if(deep && deep.resetContext) deep.resetContext();
    // 4. 草稿/继续工作状态
    currentDraftId.value=null;
    drafts.value=[]; workItems.value=[];
    applyingDraftId.value=null; applyingWorkId.value=null;
    showDraftMenu.value=false; showWorkMenu.value=false; showSubmitMenu.value=false;
    pendingFill=null; if(pendingFillTimer){ clearTimeout(pendingFillTimer); pendingFillTimer=null }
    lastSnapshotSignature.value='';
    // 5. 重置文档生成预览
    try { const dg = docGenRef.value; if(dg?.resetPreview) dg.resetPreview(); } catch(_){ }
    // 6. 滚动视图归位（若定义）
    if(typeof scrollToEditor === 'function'){
      try { scrollToEditor(); } catch(_) {}
    } else {
      // fallback: 平滑滚动到顶
      try { window.scrollTo({ top:0, behavior:'smooth' }); } catch(_) { window.scrollTo(0,0); }
    }
    triggerToast('已恢复初始状态');
  } catch(e){ triggerToast('重置失败: '+e,3000) }
  finally { showResetConfirm.value=false }
}
// 当未保存提示确认后，如果触发来源是重新编辑，则进入第二层
const originalConfirmProceed = confirmProceed;
confirmProceed = function(){
  // 调用原逻辑（用于草稿菜单打开），然后如果是“重新编辑”场景转入下一步
  originalConfirmProceed();
  if(resetModalMode.value==='unsaved' && hasUnsavedContent()==false){
    // 用户刚刚同意放弃，本次是重置意图
    resetModalMode.value='reset';
    showResetConfirm.value=true;
  }
}

onMounted(()=>{ loadTemplates(); observeMainTopBar(); loadLayoutFromStorage(); document.addEventListener('keydown', onDocumentKey) })
onMounted(()=>{ fetchUserInfo(); })
onMounted(()=>{ document.addEventListener('click', closeSubmitMenu) })
onBeforeUnmount(()=>{ document.removeEventListener('click', closeSubmitMenu) })
onBeforeUnmount(()=>{ if(mainBarObserver) mainBarObserver.disconnect(); document.removeEventListener('keydown', onDocumentKey) })
</script>
<script>
export default { directives: { dragSort } }
</script>
