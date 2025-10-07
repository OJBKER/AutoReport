<template>
  <section class="panel">
    <h2>任务统计</h2>
  <div v-if="loading" class="placeholder">加载中...</div>
    <div v-else>
      <div v-if="error" class="placeholder" style="color:#b91c1c;">{{ error }}</div>
      <template v-else>
        <div class="kv-list" style="margin-bottom:10px;">
          <div><strong>班级：</strong>{{ classId || '—' }}</div>
          <div><strong>总记录：</strong>{{ total }}</div>
        </div>
        <h3 style="margin:6px 0 8px;font-size:.95rem;">状态分布</h3>
        <table class="user-table" style="margin-bottom:18px;">
          <thead>
            <tr><th>状态</th><th>数量</th><th>占比</th></tr>
          </thead>
          <tbody>
            <tr v-for="row in statusBreakdown" :key="row.status">
              <td>{{ row.status || '—' }}</td>
              <td>{{ row.count }}</td>
              <td>{{ total? ((row.count/total*100).toFixed(1)+'%'):'-' }}</td>
            </tr>
            <tr v-if="!statusBreakdown.length"><td colspan="3" style="text-align:center;color:#666;">暂无数据</td></tr>
          </tbody>
        </table>
        <h3 style="margin:6px 0 8px;font-size:.95rem;">按任务与状态</h3>
        <table class="user-table">
          <thead>
            <tr><th>任务ID</th><th>状态</th><th>数量</th></tr>
          </thead>
            <tbody>
              <tr v-for="(row,i) in taskStatus" :key="i">
                <td>{{ row.taskId }}</td>
                <td>{{ row.status || '—' }}</td>
                <td>{{ row.count }}</td>
              </tr>
              <tr v-if="!taskStatus.length"><td colspan="3" style="text-align:center;color:#666;">暂无数据</td></tr>
            </tbody>
        </table>
        <div class="table-footer">共 {{ taskStatus.length }} 条任务状态组合</div>

        <h3 style="margin:18px 0 10px;font-size:.95rem;">明细筛选</h3>
        <div style="display:flex; flex-wrap:wrap; gap:10px; margin-bottom:12px;">
          <input class="form-field" style="max-width:140px;" v-model="filters.studentNumber" placeholder="学号" />
          <input class="form-field" style="max-width:140px;" v-model="filters.name" placeholder="姓名" />
          <input class="form-field" style="max-width:140px;" v-model="filters.status" placeholder="状态" />
          <input class="form-field" style="max-width:140px;" v-model="filters.taskId" placeholder="任务ID" />
          <button class="btn" @click="resetFilters" :disabled="allFiltersEmpty">重置</button>
        </div>
        <table class="user-table" style="margin-top:4px;">
          <thead>
            <tr>
              <th>#</th>
              <th>学号</th>
              <th>姓名</th>
              <th>任务ID</th>
              <th>任务标题</th>
              <th>状态</th>
              <th>分数</th>
              <th>提交时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(r,i) in filteredRows" :key="r.id">
              <td>{{ i+1 }}</td>
              <td>{{ r.studentNumber || '—' }}</td>
              <td>{{ r.name || '—' }}</td>
              <td>{{ r.taskId }}</td>
              <td>{{ r.taskTitle || '—' }}</td>
              <td>{{ r.status || '—' }}</td>
              <td>{{ r.score == null ? '—' : r.score }}</td>
              <td>{{ formatTime(r.submitTime) }}</td>
            </tr>
            <tr v-if="!filteredRows.length"><td colspan="8" style="text-align:center;color:#666;">无符合条件数据</td></tr>
          </tbody>
        </table>
        <div class="table-footer">筛选结果：{{ filteredRows.length }} 条 / 原始 {{ detailRows.length }} 条</div>
        <div style="margin-top:14px; display:flex; gap:12px; flex-wrap:wrap;">
          <button class="btn btn-primary" :disabled="loading" @click="refreshAll">{{ loading? '刷新中...' : '刷新统计+明细' }}</button>
        </div>
      </template>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import http from '@/api/http'

const loading = ref(false)
const error = ref('')
const classId = ref('')
const total = ref(0)
const statusBreakdown = ref([])
const taskStatus = ref([])
const detailRows = ref([]) // 原始明细
const filters = ref({ studentNumber:'', name:'', status:'', taskId:'' })

const allFiltersEmpty = computed(()=>{
  const f = filters.value
  return !f.studentNumber && !f.name && !f.status && !f.taskId
})

const filteredRows = computed(()=>{
  const f = filters.value
  return detailRows.value.filter(r => {
    if (f.studentNumber && String(r.studentNumber||'').indexOf(f.studentNumber.trim())===-1) return false
    if (f.name && !(r.name||'').includes(f.name.trim())) return false
    if (f.status && !(r.status||'').includes(f.status.trim())) return false
    if (f.taskId && String(r.taskId).indexOf(f.taskId.trim())===-1) return false
    return true
  })
})

function resetFilters(){ filters.value = { studentNumber:'', name:'', status:'', taskId:'' } }

function formatTime(ts){
  if(!ts) return '—'
  // Expecting ISO string; fallback safe slicing
  try { return String(ts).replace('T',' ').substring(0,19) } catch(e){ return ts }
}

async function fetchDetails(){
  try {
    const { data } = await http.get('/api/admin/class-user-tasks')
    if(!data.success){ error.value = data.error || '明细获取失败'; return }
    detailRows.value = data.data || []
  } catch(e){
    error.value = '明细异常: '+ e
  }
}

async function fetchStats(){
  loading.value = true
  error.value = ''
  try {
    const { data } = await http.get('/api/admin/class-task-stats')
    if(!data.success){ error.value = data.error || '获取失败'; return }
    classId.value = data.classId
    total.value = data.total || 0
    statusBreakdown.value = data.statusBreakdown || []
    taskStatus.value = data.taskStatus || []
  } catch(e){
    error.value = '异常: '+ e
  } finally { loading.value = false }
}

async function refreshAll(){
  loading.value = true
  error.value = ''
  try {
    await Promise.all([fetchStats(), fetchDetails()])
  } finally { loading.value = false }
}

onMounted(()=>{ refreshAll() })
</script>

<style scoped></style>
