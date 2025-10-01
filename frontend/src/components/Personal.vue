<template>
  <div class="personal-container">
    <h1>个人中心</h1>
    <div v-if="user">
      <img :src="user.avatar_url" alt="avatar" class="avatar" v-if="user.avatar_url" />
      
      <!-- 基本信息 -->
      <div class="info-section">
        <h3>基本信息</h3>
        <div v-if="user.loginType === 'github'">
          <p><strong>用户ID：</strong>{{ user.id }}</p>
          <p><strong>用户名：</strong>{{ user.login }}</p>
          <p><strong>昵称：</strong>{{ user.name }}</p>
          <p><strong>邮箱：</strong>{{ user.email }}</p>
          <p v-if="user.githubId"><strong>GitHub ID：</strong>{{ user.githubId }}</p>
        </div>
        <div v-else-if="user.loginType === 'school'">
          <p><strong>学号：</strong>{{ user.studentId }}</p>
          <p v-if="user.name"><strong>姓名：</strong>{{ user.name }}</p>
          <p v-if="user.userId"><strong>用户ID：</strong>{{ user.userId }}</p>
        </div>
        <div v-else>
          <p><strong>用户ID：</strong>{{ user.id || user.userId }}</p>
          <p><strong>用户名：</strong>{{ user.login || user.studentId }}</p>
          <p v-if="user.name"><strong>姓名：</strong>{{ user.name }}</p>
          <p v-if="user.email"><strong>邮箱：</strong>{{ user.email }}</p>
        </div>
      </div>

      <!-- 班级信息 -->
      <div class="info-section" v-if="user.classes">
        <h3>班级信息</h3>
        <p><strong>班级ID：</strong>{{ user.classes.classId }}</p>
        <p><strong>班级名称：</strong>{{ user.classes.name }}</p>
      </div>

      <!-- 登录状态 -->
      <div class="info-section">
        <h3>登录状态</h3>
        <div class="login-info">
          <span style="color: #409eff; font-size: 15px;">
            当前登录方式：
            <template v-if="user.loginType === 'github'">GitHub 登录</template>
            <template v-else-if="user.loginType === 'school'">学校账号登录</template>
            <template v-else>未知</template>
          </span>
          <br v-if="user.loginType === 'github'" />
          <div v-if="user.loginType === 'github'" style="margin-top: 8px;">
            <span v-if="user.githubIdExists" style="color: #67c23a;">GitHub账号已绑定本地账号</span>
            <span v-else-if="'githubIdExists' in user" style="color: #e6a23c;">GitHub账号尚未绑定本地账号</span>
          </div>
          <div v-if="user.dbUser" style="margin-top: 8px; font-size: 14px; color: #666;">
            数据库状态：{{ user.dbUser }}
          </div>
        </div>
      </div>

      <!-- 任务完成情况 -->
      <div class="info-section" v-if="userTasks.length > 0">
        <h3>任务完成情况</h3>
        <div v-for="task in userTasks" :key="task.id" class="task-item">
          <p><strong>任务：</strong>{{ task.task.title }}</p>
          <p><strong>状态：</strong>{{ task.status }}</p>
          <p v-if="task.submitTime"><strong>提交时间：</strong>{{ task.submitTime.split('T')[0] }}</p>
          <p v-if="task.score"><strong>得分：</strong>{{ task.score }}</p>
        </div>
      </div>
      <div class="info-section" v-else>
        <h3>任务完成情况</h3>
        <p>暂无任务记录</p>
      </div>
    </div>
    <div v-else>
      <p>未获取到用户信息，请先登录。</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
const user = ref(null)
const userTasks = ref([])

onMounted(async () => {
  try {
    // 获取用户信息
    const res = await fetch('/api/user/me', { credentials: 'include' })
    if (res.ok) {
      user.value = await res.json()
      
      // 获取用户任务完成情况
      if (user.value && user.value.userTasks) {
        userTasks.value = user.value.userTasks
      }
    }
  } catch (e) {
    console.error('获取用户信息失败:', e)
    user.value = null
  }
})
</script>

<style scoped>
.personal-container {
  max-width: 600px;
  margin: 60px auto;
  padding: 32px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px #eee;
  text-align: left;
}
.avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  margin-bottom: 16px;
  display: block;
  margin-left: auto;
  margin-right: auto;
}
.info-section {
  margin-bottom: 24px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 6px;
  border-left: 4px solid #409eff;
}
.info-section h3 {
  margin-top: 0;
  margin-bottom: 12px;
  color: #409eff;
}
.task-item {
  background: #fff;
  padding: 12px;
  margin-bottom: 8px;
  border-radius: 4px;
  border: 1px solid #e0e0e0;
}
</style>
