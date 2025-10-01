<template>
  <div class="class-tasks-container">
    <h1>当前班级任务</h1>
    <div v-if="className">
      <p>班级：{{ className }}</p>
    </div>
    <ul>
      <li v-for="task in tasks" :key="task.id">
        {{ task.title }}<span v-if="task.deadline">（截止：{{ task.deadline.split('T')[0] }}）</span>
      </li>
    </ul>
    <div v-if="tasks.length === 0">暂无任务</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const className = ref('')
const classId = ref(null)
const tasks = ref([])

onMounted(async () => {
  // 获取当前用户信息
  const userRes = await fetch('/api/user/me', { credentials: 'include' })
  if (userRes.ok) {
    const user = await userRes.json()
    if (user.classes && user.classes.classId) {
      className.value = user.classes.name
      classId.value = user.classes.classId
      // 获取班级任务（使用className作为参数）
      const taskRes = await fetch(`/api/tasks?className=${user.classes.classId}`, { credentials: 'include' })
      if (taskRes.ok) {
        tasks.value = await taskRes.json()
      }
    }
  }
})
</script>

<style scoped>
.class-tasks-container {
  max-width: 600px;
  margin: 60px auto;
  padding: 32px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px #eee;
  text-align: center;
}
</style>
