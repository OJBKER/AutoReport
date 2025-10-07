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
              <DocGenBlock :form-data="resolveViewer()?.getFormData?.() || null" :template-code="selectedType !== 'none' ? Number(selectedType) : null" @toast="triggerToast" />
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
import { getCsrfToken } from '../utils/csrf.js'
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
async function loadTemplates(){ try { const res=await fetch('/api/report/templates'); if(!res.ok) return; const data=await res.json(); if(data.success){ // 兼容后端仍可能返回 templateType
  templates.value=(data.templates||[]).map(t=>({
    ...t,
    templateCode: t.templateCode != null ? t.templateCode : (t.templateType ? t.templateType : null) // 如果后端暂时只有 templateType，保留原值
  })).filter(t=>t.templateCode!=null)
} } catch(_){} }
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
    const csrfToken=await getCsrfToken();
    // 后端不支持 GET /api/report/draft，列表应请求 /api/report/draft/all (无 id -> list mode)
  const filter = selectedType.value!=="none" ? ('?templateCode='+encodeURIComponent(selectedType.value)) : '';
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
        drafts.value=(data.drafts||[]).map(d=>({
          id:d.id,
          updateTime:d.updateTime,
          updateTimeFmt:formatTime(d.updateTime),
          parsed:null,
          templateCode:(d.templateCode!=null?d.templateCode:(d.templateType!=null?d.templateType:null)),
          title:((d.templateCode!=null)?('[C'+d.templateCode+'] '):'')+'草稿 #'+d.id
        }))
      }
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
    const csrfToken = await getCsrfToken();
    const res = await fetch('/api/task-submissions/user', { credentials:'include', headers:{'X-CSRF-TOKEN': csrfToken} });
    if(!res.ok){ workError.value='HTTP '+res.status; loadingWork.value=false; return }
    const data = await res.json();
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
    const csrfToken = await getCsrfToken();
    const res = await fetch('/api/task-submissions/user/task/'+encodeURIComponent(w.taskId), { credentials:'include', headers:{'X-CSRF-TOKEN': csrfToken} });
    if(!res.ok){ triggerToast('加载失败 HTTP '+res.status,3000); return }
    const data = await res.json().catch(()=>null);
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
async function fetchUserInfo(){ try { const res=await fetch('/api/user/me',{ credentials:'include'}); if(res.ok){ const data=await res.json(); userInfo.value=data } } catch(e){ /* ignore */ } }
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
  if(submitting.value) return; // 全局阻塞，简单处理
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
    const csrfToken = await getCsrfToken();
    const payload = { taskId: ut.task.id, templateCode: ut.task.templateCode ?? (selectedType.value!=='none'?Number(selectedType.value):null), submit: true, ...form, aiContextUrl: compressed };
    const res = await fetch('/api/report/draft', { method:'POST', headers:{'Content-Type':'application/json','X-CSRF-TOKEN':csrfToken}, credentials:'include', body: JSON.stringify(payload) });
    if(!res.ok){ triggerToast('提交失败 HTTP '+res.status); return }
    const data = await res.json().catch(()=>null);
    if(!data || !data.success){ triggerToast('提交失败: '+(data&&data.message?data.message:'未知错误')); return }
    triggerToast('已提交任务: '+ (ut.task.title||('#'+ut.task.id)) );
    // 刷新用户信息更新状态
    await fetchUserInfo();
    // 自动关闭并重置
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
    // 5. 滚动视图归位
    scrollToEditor();
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
.top-btn.submit-btn[disabled] { opacity: .55; cursor: not-allowed; }
.submit-dropdown-wrapper { position: relative; }
.submit-menu { position:absolute; top:40px; right:0; width:360px; max-height:420px; background:var(--color-surface); border:1px solid var(--color-border); border-radius: var(--radius-md); box-shadow: var(--shadow-pop); padding: var(--space-2) var(--space-3); display:flex; flex-direction:column; gap:var(--space-2); animation: fadeIn .18s ease; }
.submit-menu-header { display:flex; justify-content:space-between; align-items:center; font-size: var(--font-size-sm); padding:2px 4px 6px; border-bottom:1px solid var(--color-gray-100); }
.submit-empty { font-size: var(--font-size-xs); color: var(--color-text-secondary); padding: var(--space-2) 4px; }
.submit-list { list-style:none; margin:0; padding:0; overflow-y:auto; flex:1; display:flex; flex-direction:column; gap:6px; }
.submit-item { display:flex; justify-content:space-between; align-items:flex-start; gap:6px; border:1px solid var(--color-border); border-radius: var(--radius-sm); padding:6px 8px; background: var(--color-surface-subtle); }
.submit-item.pending { opacity:.6; }
.submit-task-main { flex:1; display:flex; flex-direction:column; gap:2px; }
.submit-title { font-size: var(--font-size-sm); font-weight:500; color: var(--color-text-primary); white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.submit-status { font-size: var(--font-size-2xs); color: var(--color-text-secondary); }
.submit-action-btn { background: var(--color-primary); color:#fff; border:none; border-radius: var(--radius-xs); padding:4px 10px; font-size: var(--font-size-xs); cursor:pointer; box-shadow: var(--shadow-xs); }
.submit-action-btn:hover { background: var(--color-primary-hover); }
.work-dropdown-wrapper { position: relative; }
.work-menu { position:absolute; top:40px; right:0; width:360px; max-height:420px; background:var(--color-surface); border:1px solid var(--color-border); border-radius: var(--radius-md); box-shadow: var(--shadow-pop); padding: var(--space-2) var(--space-3); display:flex; flex-direction:column; gap:var(--space-2); animation: fadeIn .18s ease; }
.work-menu-header { display:flex; justify-content:space-between; align-items:center; font-size: var(--font-size-sm); padding:2px 4px 6px; border-bottom:1px solid var(--color-gray-100); }
.work-error { color: var(--color-red-500); font-size: var(--font-size-xs); padding: var(--space-2) 4px; }
.work-empty { font-size: var(--font-size-xs); color: var(--color-text-secondary); padding: var(--space-2) 4px; }
.work-list { list-style:none; margin:0; padding:0; overflow-y:auto; flex:1; display:flex; flex-direction:column; gap:6px; }
.work-item { padding:6px 8px; border:1px solid var(--color-border); border-radius: var(--radius-sm); cursor:pointer; background: var(--color-surface-subtle); display:flex; flex-direction:column; gap:2px; transition: background var(--transition-fast); }
.work-item:hover { background: var(--color-gray-100); }
.work-item.applying { opacity:.6; }
.work-title { font-size: var(--font-size-sm); font-weight:500; color: var(--color-text-primary); white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.work-meta { font-size: var(--font-size-2xs); color: var(--color-text-secondary); }
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
.layout-toggle-btn { --btn-bg: var(--color-primary); --btn-bg-hover: var(--color-primary-hover); display:inline-flex; align-items:center; gap:6px; padding:6px 14px; background: var(--btn-bg); color:#fff; border:none; border-radius: var(--radius-sm); cursor:pointer; font-size: var(--font-size-sm); box-shadow: var(--shadow-sm); line-height:1; transition: background var(--transition-base), box-shadow var(--transition-fast); }
.layout-toggle-btn:hover { background: var(--btn-bg-hover); }
.layout-toggle-btn.icon-only { padding:6px; width:auto; min-width:0; }
.layout-toggle-btn.icon-only .icon { width:20px; height:20px; }
.layout-toggle-btn.icon-only .label-text { display:none; }
.layout-toggle-btn .icon-wrapper { display:inline-flex; align-items:center; justify-content:center; }
.layout-toggle-btn .icon { width:18px; height:18px; display:block; }
.layout-toggle-btn .icon-filled { fill: currentColor; }
/* 在浅背景上保证可见性；若主题色太浅可加发光 */
.layout-toggle-btn:not(:disabled):hover .icon-filled { filter: drop-shadow(0 0 2px rgba(0,0,0,0.25)); }
.layout-toggle-btn:active { transform: translateY(1px); }
.layout-toggle-btn:focus-visible { outline:2px solid var(--color-primary); outline-offset:2px; }

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
