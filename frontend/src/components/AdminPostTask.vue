<template>
  <section class="panel">
    <h2>发布任务</h2>
    <div class="placeholder" style="margin-bottom:14px;">填写信息并提交，任务会绑定当前管理员班级。</div>
  <form @submit.prevent="submitTask" class="task-form u-flex-col" style="gap:14px; max-width:760px;">
      <div>
        <label class="lbl">标题 <span style="color:#dc2626;">*</span></label>
        <input v-model="form.title" class="form-field" placeholder="例如：第3次实验报告" required />
      </div>
      <div>
        <label class="lbl">描述</label>
        <textarea v-model="form.description" class="form-field" rows="4" placeholder="任务说明（可选）"></textarea>
      </div>
      <div class="u-flex" style="gap:14px; flex-wrap:wrap;">
        <div style="flex:1; min-width:200px;">
          <label class="lbl">截止时间 (本地时间)</label>
          <input type="datetime-local" v-model="form.deadline" class="form-field" />
        </div>
        <div style="flex:1; min-width:160px;">
          <label class="lbl">模板代码</label>
          <input v-model="form.templateCode" class="form-field" placeholder="数字，可留空" />
        </div>
      </div>
      <div class="u-flex" style="gap:14px; flex-wrap:wrap; align-items:flex-end;">
        <div style="min-width:220px;">
          <label class="lbl">分配方式</label>
          <select v-model="assignMode" class="form-field">
            <option value="class">整个班级</option>
            <option value="single">单个学生</option>
          </select>
        </div>
        <div v-if="assignMode==='single'" style="flex:1; min-width:260px;">
          <label class="lbl">选择学生（学号 - 姓名）</label>
          <select v-model="targetStudentNumber" class="form-field">
            <option value="" disabled>请选择学生</option>
            <option v-for="stu in classUsers" :key="stu.studentNumber" :value="stu.studentNumber">{{ stu.studentNumber }} - {{ stu.name || '未命名' }}</option>
          </select>
        </div>
        <div v-if="assignMode==='single'" style="min-width:120px;">
          <button type="button" class="btn" @click="loadUsers" :disabled="loadingUsers" style="margin-top:18px;">{{ loadingUsers? '加载中...' : '刷新学生' }}</button>
        </div>
      </div>
      <div class="u-flex" style="gap:10px;">
        <button type="submit" class="btn btn-primary" :disabled="submitting">{{ submitting? '提交中...' : '发布任务' }}</button>
        <button type="button" class="btn" @click="resetForm" :disabled="submitting">重置</button>
      </div>
    </form>
    <div v-if="message.text" :style="messageStyle" style="margin-top:16px; font-size:.85rem;">{{ message.text }}</div>
    <div v-if="recentTask" class="recent-task" style="margin-top:22px;">
      <h3 style="margin:0 0 8px; font-size:.95rem;">最新创建任务</h3>
      <ul class="kv-list" style="margin:0;">
        <li><strong>ID：</strong>{{ recentTask.id }}</li>
        <li><strong>标题：</strong>{{ recentTask.title }}</li>
        <li><strong>截止：</strong>{{ recentTask.deadline? formatTime(recentTask.deadline) : '—' }}</li>
        <li><strong>模板：</strong>{{ recentTask.templateCode ?? '—' }}</li>
      </ul>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import http from '@/api/http'

const form = ref({ title:'', description:'', deadline:'', templateCode:'' })
const submitting = ref(false)
const message = ref({ text:'', type:'' })
const recentTask = ref(null)
const assignMode = ref('class') // 'class' | 'single'
const classUsers = ref([])
const loadingUsers = ref(false)
const targetStudentNumber = ref('')

const messageStyle = computed(()=>({ color: message.value.type==='error'? '#dc2626' : '#16a34a' }))

function resetForm(){
  form.value = { title:'', description:'', deadline:'', templateCode:'' }
  assignMode.value = 'class'
  targetStudentNumber.value = ''
}

async function loadUsers(){
  loadingUsers.value = true
  try {
    const { data } = await http.get('/api/admin/class-users')
    if(data.success){
      classUsers.value = data.data || []
    }
  } catch(e){
    // 静默失败即可
  } finally {
    loadingUsers.value = false
  }
}

onMounted(()=>{ loadUsers() })

function normalizeDeadline(value){
  if(!value) return ''
  if(value.length === 16) return value + ':00'
  return value
}

async function submitTask(){
  if(!form.value.title.trim()) { message.value={text:'标题必填', type:'error'}; return }
  submitting.value = true
  message.value={text:'', type:''}
  try {
    const payload = {
      title: form.value.title.trim(),
      description: form.value.description || '',
      deadline: normalizeDeadline(form.value.deadline),
      templateCode: form.value.templateCode? Number(form.value.templateCode) : null
    }
    if(assignMode.value === 'single'){
      if(!targetStudentNumber.value){
        message.value={text:'请选择一个学生', type:'error'}; return
      }
      payload.targetStudentNumber = targetStudentNumber.value
    }
  const { data } = await http.post('/api/admin/tasks', payload)
  if(!data.success){ message.value={text: data.error || '创建失败', type:'error'}; return }
  recentTask.value = data.data
  const createdCount = data.createdUserTasks ?? 0
  if(data.assignMode === 'single'){
    message.value={text:`创建成功，已为学号 ${targetStudentNumber.value} 生成任务记录`, type:'success'}
  } else {
    message.value={text:`创建成功，已为 ${createdCount} 名学生生成任务记录`, type:'success'}
  }
  } catch(e){
    message.value={text:'异常: '+ e, type:'error'}
  } finally { submitting.value=false }
}

function formatTime(ts){
  try { return String(ts).replace('T',' ').substring(0,19) } catch(e){ return ts }
}
</script>

<style scoped>
.lbl { display:block; font-size:.75rem; margin-bottom:4px; color: var(--color-text-secondary); letter-spacing:.5px; }
.task-form :deep(textarea) { resize: vertical; }
</style>
