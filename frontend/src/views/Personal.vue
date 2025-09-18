<template>
  <div class="personal-container">
    <h1>个人中心</h1>
    <div v-if="user">
      <img :src="user.avatar_url" alt="avatar" class="avatar" v-if="user.avatar_url" />
      <p><strong>用户ID：</strong>{{ user.id }}</p>
      <p><strong>用户名：</strong>{{ user.login }}</p>
      <p><strong>昵称：</strong>{{ user.name }}</p>
      <p><strong>邮箱：</strong>{{ user.email }}</p>
    </div>
    <div v-else>
      <p>未获取到用户信息，请先登录。</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
const user = ref(null)

onMounted(async () => {
  try {
    const res = await fetch('/api/user/me', { credentials: 'include' })
    if (res.ok) {
      user.value = await res.json()
    }
  } catch (e) {
    user.value = null
  }
})
</script>

<style scoped>
.personal-container {
  max-width: 400px;
  margin: 60px auto;
  padding: 32px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px #eee;
  text-align: center;
}
.avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  margin-bottom: 16px;
}
</style>
