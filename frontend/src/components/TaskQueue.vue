<template>
  <div class="task-queue-container">
    <h1>任务队列</h1>
    <ul>
      <li>队列任务A：待审核</li>
      <li>队列任务B：待处理</li>
      <li>队列任务C：已完成</li>
    </ul>
  </div>
</template>

<script setup>
// 可扩展为实际队列数据
</script>

<style scoped>
.task-queue-container {
  max-width: 600px;
  margin: 60px auto;
  padding: 32px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px #eee;
  text-align: center;
}
</style>
