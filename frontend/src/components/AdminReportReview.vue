<template>
  <section class="panel admin-report-review">
    <header class="review-header">
      <div class="title-block">
        <h2>实验报告批阅</h2>
        <p class="subtitle">选择任务查看学生提交，支持预览内容与登记分数。</p>
      </div>
      <div class="actions">
        <label class="form-label">选择任务</label>
        <select class="form-field" v-model="selectedTaskId" @change="handleTaskChange" :disabled="loadingTasks || taskOptions.length===0">
          <option value="" disabled>请选择任务</option>
          <option v-for="task in taskOptions" :key="task.id" :value="task.id">
            {{ task.title || ('任务 #' + task.id) }}
          </option>
        </select>
        <button class="btn" @click="refreshTasks" :disabled="loadingTasks">{{ loadingTasks ? '刷新中...' : '刷新任务' }}</button>
      </div>
    </header>

    <div v-if="taskOptions.length===0 && !loadingTasks" class="placeholder">暂无任务数据，请先发布任务或刷新。</div>
    <div v-else>
      <div v-if="loadingSubmissions" class="placeholder">加载提交中...</div>
      <div v-else-if="currentError" class="placeholder error">{{ currentError }}</div>
      <div v-else>
        <div class="info-row" v-if="currentTask">
          <div><strong>任务：</strong>{{ currentTask.title || ('任务 #' + currentTask.id) }}</div>
          <div><strong>模板：</strong>{{ currentTask.templateCode ?? '—' }}</div>
          <div><strong>学生数量：</strong>{{ submissions.length }}</div>
        </div>

        <table class="user-table submissions-table">
          <thead>
            <tr>
              <th>#</th>
              <th>学号</th>
              <th>姓名</th>
              <th>GitHub</th>
              <th>状态</th>
              <th>分数</th>
              <th>提交时间</th>
              <th>最新提交</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, idx) in submissions" :key="row.userTaskId">
              <td>{{ idx + 1 }}</td>
              <td>{{ row.studentNumber || '—' }}</td>
              <td>{{ row.studentName || '—' }}</td>
              <td>{{ row.githubId || '—' }}</td>
              <td>
                <select v-model="row._status" class="form-field status-field">
                  <option value="">保持不变</option>
                  <option v-for="opt in statusOptions" :key="opt" :value="opt">{{ opt }}</option>
                </select>
              </td>
              <td>
                <input type="number" class="form-field score-field" v-model="row._score" placeholder="—" />
              </td>
              <td>{{ formatTime(row.submitTime) }}</td>
              <td>
                <div class="latest">
                  <span v-if="row.submissionUpdateTime">{{ formatTime(row.submissionUpdateTime) }}</span>
                  <span v-else>—</span>
                  <span v-if="row.submitFlag" class="tag tag-success">已提交</span>
                  <span v-else-if="row.submissionId" class="tag">草稿</span>
                </div>
              </td>
              <td class="op-col">
                <button class="btn" @click="openPreview(row)" :disabled="!row.submissionId">预览</button>
                <button class="btn btn-primary" @click="saveRow(row)" :disabled="row._saving">
                  {{ row._saving ? '保存中...' : '保存' }}
                </button>
              </td>
            </tr>
            <tr v-if="!submissions.length">
              <td colspan="9" style="text-align:center; color:#64748b;">当前任务暂无学生记录</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="toastVisible" class="toast">{{ toastMessage }}</div>

    <div v-if="previewVisible" class="preview-mask" @click.self="closePreview">
      <div class="preview-dialog">
        <header class="preview-header">
          <h3>报告预览 - {{ previewMeta.studentName || previewMeta.studentNumber || '未命名' }}</h3>
          <button class="close-btn" @click="closePreview">×</button>
        </header>
        <section class="preview-body">
          <div class="preview-meta">
            <div><strong>任务：</strong>{{ currentTask ? (currentTask.title || ('任务 #' + currentTask.id)) : '—' }}</div>
            <div><strong>提交时间：</strong>{{ formatTime(previewMeta.submissionUpdateTime) }}</div>
            <div><strong>模板：</strong>{{ previewMeta.templateCode ?? (currentTask?.templateCode ?? '—') }}</div>
          </div>
          <div class="preview-content docgen-wrapper">
            <DocGenBlock
              v-if="previewFormData"
              ref="docGenRef"
              :key="previewDocKey"
              :form-data="previewFormData"
              :template-code="activeTemplateCode"
              @toast="toast"
            />
            <template v-else>
              <div class="placeholder">该提交未包含可解析的表单内容。</div>
              <div v-if="previewRawContent" class="raw-preview">
                <h4>原始内容</h4>
                <pre>{{ previewRawContent }}</pre>
              </div>
            </template>
          </div>
          <div v-if="previewMeta.aiContextUrl" class="preview-content">
            <h4>AI 对话 / 附加信息</h4>
            <pre class="ai-context">{{ previewMeta.aiContextUrl }}</pre>
          </div>
        </section>
        <footer class="preview-footer">
          <button class="btn" @click="closePreview">关闭</button>
        </footer>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import http from '@/api/http'
import { useToast } from '@/composables/useToast'
import DocGenBlock from '@/template/DocGenBlock.vue'

const loadingTasks = ref(false)
const taskOptions = ref([])
const selectedTaskId = ref('')
const currentTask = ref(null)
const loadingSubmissions = ref(false)
const currentError = ref('')
const submissions = ref([])

const statusOptions = ['未开始', '进行中', '已完成']

const previewVisible = ref(false)
const previewFormData = ref(null)
const previewRawContent = ref('')
const previewMeta = ref({})
const previewDocKey = ref(0)
const docGenRef = ref(null)

const { show: toastVisibleRef, message: toastMessageRef, toast } = useToast()
const toastVisible = toastVisibleRef
const toastMessage = toastMessageRef

const activeTemplateCode = computed(() => {
  return previewMeta.value.templateCode ?? (currentTask.value?.templateCode ?? null)
})

const formatTime = (val) => {
  if (!val) return '—'
  try {
    return String(val).replace('T', ' ').substring(0, 19)
  } catch (e) {
    return val
  }
}

async function refreshTasks() {
  loadingTasks.value = true
  currentError.value = ''
  try {
    const { data } = await http.get('/api/admin/class-user-tasks')
    if (!data.success) {
      currentError.value = data.error || '获取任务列表失败'
      taskOptions.value = []
      return
    }
    const list = Array.isArray(data.data) ? data.data : []
    const map = new Map()
    list.forEach((item) => {
      if (!map.has(item.taskId)) {
        map.set(item.taskId, {
          id: item.taskId,
          title: item.taskTitle,
          templateCode: item.templateCode ?? null,
        })
      }
    })
    taskOptions.value = Array.from(map.values()).sort((a, b) => (b.id || 0) - (a.id || 0))
    if (!taskOptions.value.length) {
      selectedTaskId.value = ''
      currentTask.value = null
      submissions.value = []
    } else if (!selectedTaskId.value || !taskOptions.value.some((t) => String(t.id) === String(selectedTaskId.value))) {
      selectedTaskId.value = taskOptions.value[0].id
      handleTaskChange()
    } else {
      handleTaskChange()
    }
  } catch (e) {
    currentError.value = '任务数据异常: ' + e
    taskOptions.value = []
  } finally {
    loadingTasks.value = false
  }
}

async function handleTaskChange() {
  if (!selectedTaskId.value) {
    submissions.value = []
    currentTask.value = null
    return
  }
  const taskMeta = taskOptions.value.find((t) => String(t.id) === String(selectedTaskId.value))
  currentTask.value = taskMeta || null
  await loadSubmissions()
}

async function loadSubmissions() {
  if (!selectedTaskId.value) return
  loadingSubmissions.value = true
  currentError.value = ''
  try {
    const { data } = await http.get(`/api/admin/task-submissions/${selectedTaskId.value}`)
    if (!data.success) {
      currentError.value = data.error || '提交数据获取失败'
      submissions.value = []
      return
    }
    const rows = Array.isArray(data.data) ? data.data : []
    submissions.value = rows.map((row) => ({
      ...row,
      _score: row.score ?? '',
      _status: row.status || '',
      _saving: false,
    }))
    currentTask.value = data.task || currentTask.value
  } catch (e) {
    currentError.value = '提交数据异常: ' + e
    submissions.value = []
  } finally {
    loadingSubmissions.value = false
  }
}

function parseScore(val) {
  if (val === '' || val === null || val === undefined) return null
  const num = Number(val)
  if (Number.isNaN(num)) return null
  return num
}

async function saveRow(row) {
  if (!row || !row.userTaskId) return
  row._saving = true
  try {
    const payload = {
      score: parseScore(row._score),
    }
    if (row._status) {
      payload.status = row._status
    }
    const { data } = await http.post(`/api/admin/user-tasks/${row.userTaskId}/score`, payload)
    if (!data.success) {
      toast(data.error || '保存失败')
      return
    }
    const updated = data.data || {}
    row.score = updated.score ?? row.score
    row.status = updated.status ?? row.status
    row.submitTime = updated.submitTime ?? row.submitTime
    row._score = updated.score ?? ''
    row._status = updated.status || ''
    toast('保存成功')
  } catch (e) {
    toast('保存异常: ' + e)
  } finally {
    row._saving = false
  }
}

function openPreview(row) {
  if (!row || !row.submissionId) {
    toast('暂无可预览内容')
    return
  }
  previewMeta.value = {
    studentName: row.studentName,
    studentNumber: row.studentNumber,
    submissionUpdateTime: row.submissionUpdateTime,
    templateCode: row.submissionTemplateCode,
    aiContextUrl: row.aiContextUrl,
    submissionId: row.submissionId,
  }
  previewFormData.value = null
  previewRawContent.value = ''
  previewDocKey.value = Date.now()
  if (row.taskUrl) {
    try {
      previewFormData.value = JSON.parse(row.taskUrl)
    } catch (e) {
      previewRawContent.value = row.taskUrl
    }
  }
  previewVisible.value = true
}

function closePreview() {
  if (docGenRef.value?.resetPreview) {
    docGenRef.value.resetPreview()
  }
  previewVisible.value = false
  previewFormData.value = null
  previewRawContent.value = ''
  previewMeta.value = {}
}

onMounted(() => {
  refreshTasks()
})
</script>

<style scoped>
.admin-report-review {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.review-header {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
}

.title-block h2 {
  margin: 0;
}

.subtitle {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 0.85rem;
}

.actions {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.form-label {
  font-size: 0.75rem;
  color: #6b7280;
}

.placeholder {
  padding: 14px 16px;
  background: #f8fafc;
  border-radius: 8px;
  color: #475569;
}

.placeholder.error {
  color: #b91c1c;
  background: #fef2f2;
}

.info-row {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  padding: 10px 12px;
  background: #f1f5f9;
  border-radius: 10px;
  font-size: 0.9rem;
}

.submissions-table {
  margin-top: 8px;
}

.status-field {
  min-width: 120px;
}

.score-field {
  width: 90px;
}

.latest {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 0.8rem;
  color: #475569;
}

.tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px 8px;
  border-radius: 10px;
  background: #e2e8f0;
  font-size: 0.75rem;
}

.tag-success {
  background: #d1fae5;
  color: #047857;
}

.op-col {
  display: flex;
  gap: 8px;
}

.toast {
  position: fixed;
  right: 26px;
  bottom: 34px;
  background: rgba(31, 41, 55, 0.92);
  color: #fff;
  padding: 10px 18px;
  border-radius: 8px;
  box-shadow: 0 10px 25px rgba(15, 23, 42, 0.25);
  font-size: 0.85rem;
  z-index: 3000;
}

.preview-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 30px;
  z-index: 2999;
}

.preview-dialog {
  width: min(1200px, 92vw);
  max-height: 90vh;
  background: #fff;
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.25);
}

.preview-header {
  padding: 14px 20px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.preview-body {
  padding: 16px 20px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.preview-content pre {
  background: #0f172a;
  color: #e2e8f0;
  padding: 16px;
  border-radius: 10px;
  font-size: 0.85rem;
  overflow-x: auto;
}

.docgen-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.raw-preview pre {
  background: #111827;
  color: #f8fafc;
}

.ai-context {
  background: #111827;
}

.preview-footer {
  padding: 12px 20px 18px;
  border-top: 1px solid #e2e8f0;
  display: flex;
  justify-content: flex-end;
}

.close-btn {
  background: transparent;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #475569;
}

@media (max-width: 720px) {
  .op-col {
    flex-direction: column;
  }
  .preview-dialog {
    width: 96vw;
  }
}
</style>
