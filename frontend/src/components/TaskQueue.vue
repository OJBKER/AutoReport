<template>
  <div class="task-queue-wrapper">
    <div class="tq-header">
      <h2 class="tq-title">任务队列</h2>
      <div class="tq-actions">
        <button class="tq-btn ghost" :disabled="loading" @click="manualRefresh">{{ loading?'刷新中...':'刷新' }}</button>
        <label class="auto-refresh" :title="'每 '+refreshInterval/1000+' 秒自动刷新'">
          <input type="checkbox" v-model="autoRefresh" /> 自动
        </label>
      </div>
    </div>
    <div class="tq-subline" v-if="currentUser">
      当前用户：<span class="user-tag">{{ currentUser.name || currentUser.login || ('UID '+currentUser.id) }}</span>
      <span v-if="currentUser.needsBinding" class="bind-warn">(未绑定学号，无法匹配任务)</span>
    </div>
    <div v-if="error" class="tq-error">{{ error }}</div>
  <div v-else-if="!loading && tasks.length===0" class="tq-empty">
    <template v-if="currentUser && currentUser.needsBinding">未绑定学号，暂无任务，请先绑定。</template>
    <template v-else>暂无任务</template>
    <span v-if="currentUser" class="debug-id" title="studentNumber / userId">(SN: {{ currentUser.studentNumber || '未绑定' }} / UID: {{ currentUser.id || currentUser.userId }})</span>
  </div>
    <ol v-if="tasks.length>0" class="tq-list">
      <li v-for="t in tasks" :key="t.id" class="tq-item" :class="'st-'+(t.status||'unknown')">
        <div class="tq-item-main">
          <div class="tq-line1">
            <span class="task-title">{{ t.task?.title || ('任务 #'+(t.task?.id||t.id)) }}</span>
            <span class="status-chip" :class="'s-'+(t.status||'unknown')">{{ t.status || '未知' }}</span>
          </div>
          <div class="tq-line2">
            <span>ID {{ t.id }}</span>
            <span v-if="t.task?.id">Task {{ t.task.id }}</span>
            <span v-if="t.score!=null">分数: {{ t.score }}</span>
            <span v-if="t.submitTime">提交: {{ formatTime(t.submitTime) }}</span>
            <span v-if="t.updateTime">更新: {{ formatTime(t.updateTime) }}</span>
          </div>
        </div>
      </li>
    </ol>
    <div class="tq-footer" v-if="lastFetchTime">最近刷新：{{ formatTime(lastFetchTime) }}</div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import http from '@/api/http'

const loading = ref(false)
const error = ref('')
const tasks = ref([])
const currentUser = ref(null)
const lastFetchTime = ref('')
const autoRefresh = ref(true)
const refreshInterval = 60000 // 60s
let timer = null

function formatTime(str){ if(!str) return ''; try { return str.replace('T',' ').substring(0,19) } catch(_) { return str } }

async function fetchCurrentUser(){
  try {
    const { data } = await http.get('/api/user/me')
    return data && (data.id || data.userId) ? data : null
  } catch { return null }
}

async function fetchUserTasks(user){
  if(!user) return
  if(user.needsBinding){
    tasks.value=[]
    return
  }
  error.value=''
  loading.value=true
  try {
    const sn = user.studentNumber
    if(!sn){ throw new Error('未获得 studentNumber') }
  const queryUrl = `/api/user-tasks?studentNumber=${encodeURIComponent(sn)}`
  const { data } = await http.get(queryUrl)
  if(!Array.isArray(data)) throw new Error('返回格式不是数组')
  tasks.value = data.map(d=>({
      id: d.id,
      status: d.status || null,
      score: d.score ?? null,
      submitTime: d.submitTime || d.submittedAt || null,
      updateTime: d.updateTime || d.updatedAt || d.submitTime || null,
      task: d.task ? { id: d.task.id, title: d.task.title } : null
    }))
    tasks.value.sort((a,b)=>{
      const orderStatus = (s)=> s==='未开始'?0 : (s==='进行中'?1 : (s==='已完成'?2:3))
      const ds = orderStatus(a.status) - orderStatus(b.status)
      if(ds!==0) return ds
      const ta = a.submitTime || a.updateTime || ''
      const tb = b.submitTime || b.updateTime || ''
      return (tb>ta?1:(tb<ta?-1:0)) || (b.id - a.id)
    })
    lastFetchTime.value = new Date().toISOString()
    if(tasks.value.length===0){ console.info('[TaskQueue] studentNumber='+sn+' 返回空列表'); }
  } catch(e){
    error.value = String(e)
    tasks.value=[]
  } finally { loading.value=false }
}

async function init(){
  loading.value=true
  currentUser.value = await fetchCurrentUser()
  await fetchUserTasks(currentUser.value)
  setupTimer()
}

function setupTimer(){
  clearTimer()
  if(autoRefresh.value){
    timer = setInterval(()=>{ fetchUserTasks(currentUser.value) }, refreshInterval)
  }
}
function clearTimer(){ if(timer){ clearInterval(timer); timer=null } }

function manualRefresh(){ fetchUserTasks(currentUser.value) }

watch(autoRefresh, ()=> setupTimer())
onMounted(()=> init())
onBeforeUnmount(()=> clearTimer())
</script>

<style scoped>
.task-queue-wrapper { padding:18px 20px 28px; display:flex; flex-direction:column; gap:12px; }
.tq-header { display:flex; justify-content:space-between; align-items:center; }
.tq-title { margin:0; font-size:18px; font-weight:600; }
.tq-actions { display:flex; gap:10px; align-items:center; }
.tq-btn { padding:6px 14px; background:#409eff; color:#fff; border:none; border-radius:6px; cursor:pointer; font-size:13px; }
.tq-btn.ghost { background:#eef2f6; color:#333; }
.tq-btn.ghost:hover { background:#dde5ec; }
.tq-btn:disabled { opacity:.55; cursor:not-allowed; }
.auto-refresh { font-size:12px; display:flex; align-items:center; gap:4px; user-select:none; }
.tq-subline { font-size:12px; color:#555; }
.user-tag { background:#eef2f6; padding:2px 8px; border-radius:12px; }
.tq-error { color:#d93025; font-size:13px; }
.tq-empty { font-size:13px; color:#666; padding:12px 4px; }
.tq-list { list-style:none; margin:0; padding:0; display:flex; flex-direction:column; gap:10px; }
.tq-item { background:#fff; border:1px solid #e2e8f0; border-radius:10px; padding:10px 14px; display:flex; flex-direction:column; gap:4px; position:relative; box-shadow:0 1px 2px rgba(0,0,0,0.04); }
.tq-item::before { content:""; position:absolute; left:0; top:0; bottom:0; width:4px; border-radius:4px 0 0 4px; background:linear-gradient(180deg,#60a5fa,#2563eb); opacity:.55; }
.tq-item.st-已完成::before { background:linear-gradient(180deg,#34d399,#059669); }
.tq-item.st-进行中::before { background:linear-gradient(180deg,#fde68a,#f59e0b); }
.tq-item.st-未开始::before { background:linear-gradient(180deg,#cbd5e1,#94a3b8); }
.tq-line1 { display:flex; justify-content:space-between; align-items:center; gap:8px; }
.task-title { font-size:14px; font-weight:600; color:#1e293b; flex:1; }
.status-chip { display:inline-block; padding:2px 8px; border-radius:12px; font-size:12px; background:#e2e8f0; color:#1e293b; }
.status-chip.s-已完成 { background:#d1fae5; color:#065f46; }
.status-chip.s-进行中 { background:#bfdbfe; color:#1e3a8a; }
.status-chip.s-未开始 { background:#fef3c7; color:#92400e; }
.tq-line2 { display:flex; flex-wrap:wrap; gap:14px; font-size:11px; color:#475569; }
.tq-footer { font-size:11px; color:#64748b; margin-top:6px; }
@media (max-width:640px){
  .tq-line2 { flex-direction:column; gap:2px; }
  .task-title { font-size:13px; }
}
</style>
