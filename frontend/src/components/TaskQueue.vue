<template>
  <div class="task-queue-container">
    <h1>任务队列</h1>
    
    <!-- 任务完成情况 -->
    <div v-if="user">
      <div class="info-section" v-if="userTasks.length > 0">
        <h3>我的任务完成情况</h3>
        <div v-for="task in userTasks" :key="task.id" class="task-item">
          <p><strong>任务：</strong>{{ task.task && task.task.title ? task.task.title : '无' }}</p>
          <p><strong>状态：</strong>{{ task.status ?? '无' }}</p>
          <p v-if="task.submitTime"><strong>提交时间：</strong>{{ (task.submitTime && task.submitTime.split) ? task.submitTime.split('T')[0] : '' }}</p>
          <p v-if="task.score"><strong>得分：</strong>{{ task.score }}</p>
        </div>
      </div>
      <div class="info-section" v-else>
        <h3>我的任务完成情况</h3>
        <p>暂无任务记录</p>
      </div>
      <div class="info-section" v-if="userSubmissions.length > 0">
        <h3>我的任务提交记录</h3>
        <div v-for="sub in userSubmissions" :key="sub.id" class="task-item">
          <p><strong>任务ID：</strong>{{ sub.task ? sub.task.id : '无' }}</p>
          <p><strong>提交时间：</strong>{{ (sub.updateTime || sub.createTime || '').split('T')[0] }}</p>
          <p><strong>AI对话历史：</strong>{{ sub.aiContextUrl ? sub.aiContextUrl.slice(0, 100) + '...' : '无' }}</p>
        </div>
      </div>
    </div>
    <div v-else>
      <p>未获取到用户信息，请先登录。</p>
    </div>
  </div>
</template>

<script setup>

import { ref, onMounted } from 'vue'
import { getCsrfToken } from '../utils/csrf.js'

const user = ref(null)
const userTasks = ref([])
const userSubmissions = ref([])

onMounted(async () => {
  try {
    // 获取 CSRF token
    const csrfToken = await getCsrfToken()
    // 获取用户信息
    const res = await fetch('/api/user/me', {
      credentials: 'include',
      headers: { 'X-CSRF-TOKEN': csrfToken }
    })
    if (res.ok) {
      user.value = await res.json()
      // 获取用户任务完成情况
      if (user.value && user.value.userTasks) {
        userTasks.value = user.value.userTasks
      }
      // 获取 task_submissions 信息
      const subRes = await fetch('/api/task-submissions/user', {
        credentials: 'include',
        headers: { 'X-CSRF-TOKEN': csrfToken }
      })
      if (subRes.ok) {
        const subData = await subRes.json()
        if (subData.success && Array.isArray(subData.submissions)) {
          userSubmissions.value = subData.submissions
        }
      }
    }
  } catch (e) {
    console.error('获取用户信息或提交记录失败:', e)
    user.value = null
  }
})
</script>

<style scoped>
.task-queue-container { max-width:600px; margin:60px auto; padding: var(--space-8); background: var(--color-surface); border-radius: var(--radius-md); box-shadow: var(--shadow-lg); text-align:left; }
.info-section { margin-bottom: var(--space-6); padding: var(--space-4); background: var(--color-gray-75); border-radius: var(--radius-sm); border-left:4px solid var(--color-primary); }
.info-section h3 { margin-top:0; margin-bottom: var(--space-3); color: var(--color-primary); }
.task-item { background: var(--color-surface); padding: var(--space-3); margin-bottom: var(--space-2); border-radius: var(--radius-xs); border:1px solid var(--color-border); }
</style>
