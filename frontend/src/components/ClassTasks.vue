<template>
  <div class="class-tasks-container">
    <h1>当前班级任务</h1>
    
    <!-- 班级信息卡片 -->
    <div v-if="className" class="class-info-card">
      <h3>{{ className }}</h3>
      <p><strong>班级ID：</strong>{{ classId }}</p>
      <p><strong>任务总数：</strong>{{ tasks.length }} 个</p>
      <p><strong>未开始：</strong>{{ getTaskCountByStatus('未开始') }} 个</p>
      <p><strong>进行中：</strong>{{ getTaskCountByStatus('进行中') }} 个</p>
      <p><strong>已完成：</strong>{{ getTaskCountByStatus('已完成') }} 个</p>
    </div>

    <!-- 任务列表 -->
    <div v-if="tasks.length > 0" class="tasks-section">
      <h3>任务详情</h3>
      <div v-for="task in tasks" :key="task.id" class="task-card">
        <div class="task-header">
          <h4>{{ task.title }}</h4>
          <span class="task-status" :class="getStatusClass(getTaskStatus(task.id))">
            {{ getTaskStatus(task.id) }}
          </span>
        </div>
        
        <div class="task-content">
          <p class="task-description">{{ task.description }}</p>
          
          <div class="task-meta">
            <div class="meta-item">
              <strong>任务ID：</strong>{{ task.id }}
            </div>
            <div class="meta-item" v-if="task.deadline">
              <strong>截止时间：</strong>{{ formatDeadline(task.deadline) }}
            </div>
            <div class="meta-item">
              <strong>所属班级：</strong>{{ task.classes?.name || className }}
            </div>
          </div>

          <!-- 任务统计信息 -->
          <div class="task-stats">
            <div class="stat-item">
              <span class="stat-label">完成人数</span>
              <span class="stat-value">{{ getCompletionCount(task.id) }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">认领人数</span>
              <span class="stat-value">{{ getTaskParticipants(task.id) }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">完成率</span>
              <span class="stat-value">{{ getCompletionRate(task.id) }}%</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">平均分</span>
              <span class="stat-value">{{ getAverageScore(task.id) }}</span>
            </div>
          </div>

          <!-- 我的任务状态 -->
          <div class="my-task-status" v-if="getMyTaskInfo(task.id)">
            <h5>我的完成情况</h5>
            <div class="my-status-info">
              <p><strong>状态：</strong>{{ getMyTaskInfo(task.id).status }}</p>
              <p v-if="getMyTaskInfo(task.id).submitTime">
                <strong>提交时间：</strong>{{ formatSubmitTime(getMyTaskInfo(task.id).submitTime) }}
              </p>
              <p v-if="getMyTaskInfo(task.id).score">
                <strong>得分：</strong>{{ getMyTaskInfo(task.id).score }} 分
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <div v-else class="no-tasks">
      <p>暂无班级任务</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import http from '@/api/http'

const className = ref('')
const classId = ref(null)
const tasks = ref([])
const userTasks = ref([])
const user = ref(null)
const classStatistics = ref({})

onMounted(async () => {
  try {
    const { data: userData } = await http.get('/api/user/me')
    user.value = userData
    if (user.value && user.value.classes && user.value.classes.classId) {
      className.value = user.value.classes.name
      classId.value = user.value.classes.classId
      if (user.value.userTasks) userTasks.value = user.value.userTasks
      const { data: detail } = await http.get(`/api/tasks/detailed?className=${user.value.classes.classId}`)
      if (detail) {
        tasks.value = detail.tasks || []
        classStatistics.value = detail.classStatistics || {}
      }
    }
  } catch(_){}
})

// 获取任务状态统计
const getTaskCountByStatus = (status) => {
  return userTasks.value.filter(userTask => userTask.status === status).length
}

// 根据任务ID获取我的任务状态
const getTaskStatus = (taskId) => {
  const myTask = userTasks.value.find(userTask => userTask.task.id === taskId)
  return myTask ? myTask.status : '未开始'
}

// 获取我的任务详细信息
const getMyTaskInfo = (taskId) => {
  return userTasks.value.find(userTask => userTask.task.id === taskId)
}

// 获取状态样式类
const getStatusClass = (status) => {
  switch(status) {
    case '已完成': return 'status-completed'
    case '进行中': return 'status-in-progress'
    case '未开始': return 'status-not-started'
    default: return 'status-unknown'
  }
}

// 格式化截止时间
const formatDeadline = (deadline) => {
  if (!deadline) return '无截止时间'
  const date = new Date(deadline)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 格式化提交时间
const formatSubmitTime = (submitTime) => {
  if (!submitTime) return '未提交'
  const date = new Date(submitTime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 从后端数据获取完成人数
const getCompletionCount = (taskId) => {
  const task = tasks.value.find(t => t.id === taskId)
  return task?.statistics?.completedCount || 0
}

// 从后端数据获取任务认领总人数
const getTaskParticipants = (taskId) => {
  const task = tasks.value.find(t => t.id === taskId)
  return task?.statistics?.taskTotalParticipants || 0
}

// 从后端数据获取完成率
const getCompletionRate = (taskId) => {
  const task = tasks.value.find(t => t.id === taskId)
  return task?.statistics?.completionRate || 0
}

// 获取平均分
const getAverageScore = (taskId) => {
  const task = tasks.value.find(t => t.id === taskId)
  return task?.statistics?.averageScore || 0
}
</script>

<style scoped>
.class-tasks-container { max-width:800px; margin:60px auto; padding: var(--space-8); background: var(--color-surface); border-radius: var(--radius-md); box-shadow: var(--shadow-lg); text-align:left; }
.class-info-card { background: linear-gradient(135deg,var(--color-purple-grad-start) 0%, var(--color-purple-grad-end) 100%); color:#fff; padding: var(--space-6); border-radius:12px; margin-bottom: var(--space-8); box-shadow: 0 4px 15px rgba(0,0,0,0.1); }
.class-info-card h3 { margin:0 0 var(--space-4) 0; font-size:24px; font-weight: var(--font-weight-semibold); }
.class-info-card p { margin: var(--space-2) 0; font-size:16px; }
.tasks-section h3 { color: var(--color-text-primary); margin-bottom: var(--space-5); font-size:22px; border-bottom:2px solid var(--color-primary); padding-bottom: var(--space-2); }
.task-card { background: var(--color-gray-75); border:1px solid var(--color-border); border-radius:12px; margin-bottom: var(--space-6); overflow:hidden; transition: box-shadow var(--transition-base), transform var(--transition-base); box-shadow: var(--shadow-md); }
.task-card:hover { box-shadow: var(--shadow-pop); transform: translateY(-2px); }
.task-header { background: var(--color-surface); padding: var(--space-5); border-bottom:1px solid var(--color-border); display:flex; justify-content:space-between; align-items:center; }
.task-header h4 { margin:0; color: var(--color-text-primary); font-size:20px; font-weight: var(--font-weight-semibold); }
.task-status { padding:6px 12px; border-radius:20px; font-size:14px; font-weight:500; }
.status-completed { background: var(--color-green-bg); color: var(--color-green-550); }
.status-in-progress { background: var(--color-yellow-bg); color: var(--color-yellow-500); }
.status-not-started { background: var(--color-red-bg); color: var(--color-red-500); }
.status-unknown { background: var(--color-gray-200); color: var(--color-text-secondary); }
.task-content { padding: var(--space-5); }
.task-description { color: var(--color-text-secondary); line-height:1.6; margin-bottom: var(--space-5); font-size:16px; }
.task-meta { display:grid; grid-template-columns: repeat(auto-fit,minmax(200px,1fr)); gap:12px; margin-bottom: var(--space-5); }
.meta-item { background: var(--color-surface); padding:12px; border-radius: var(--radius-md); border-left:4px solid var(--color-primary); }
.task-stats { display:flex; justify-content:space-around; background: var(--color-surface); padding: var(--space-4); border-radius: var(--radius-md); margin-bottom: var(--space-5); }
.stat-item { text-align:center; }
.stat-label { display:block; font-size:14px; color: var(--color-text-secondary); margin-bottom:4px; }
.stat-value { display:block; font-size:24px; font-weight: var(--font-weight-semibold); color: var(--color-primary); }
.my-task-status { background: var(--color-blue-50); padding: var(--space-4); border-radius: var(--radius-md); border-left:4px solid var(--color-primary); }
.my-task-status h5 { margin:0 0 var(--space-3) 0; color: var(--color-primary); font-size:16px; }
.my-status-info p { margin:6px 0; font-size:14px; }
.no-tasks { text-align:center; padding:60px 20px; color: var(--color-text-muted); font-size:18px; }
</style>
