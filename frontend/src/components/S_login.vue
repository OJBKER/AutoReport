<template>
  <div class="s-login-container">
    <form @submit.prevent="handleLogin">
      <div class="form-group">
        <label for="studentId">学号</label>
        <input v-model="studentId" id="studentId" type="text" required placeholder="请输入学号" />
      </div>
      <div class="form-group">
        <label for="password">密码</label>
        <input v-model="password" id="password" type="password" required placeholder="请输入密码" />
      </div>
      <button type="submit" class="login-btn">登录</button>
      <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>
    </form>
  </div>
</template>

<script setup>


import { ref } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'
import { getCsrfToken } from '../utils/csrf.js'

const router = useRouter()

const studentId = ref('')
const password = ref('')
const errorMsg = ref('')

async function handleLogin() {
  errorMsg.value = ''
  try {
    // 获取 CSRF token
    const csrfToken = await getCsrfToken()
    const res = await axios.post('/api/slogin', {
      studentId: studentId.value,
      password: password.value
    }, {
      headers: {
        'X-CSRF-TOKEN': csrfToken
      },
      withCredentials: true
    })
    if (res.data && res.data.success) {
      // 登录成功
      router.push('/Main')
    } else {
      errorMsg.value = res.data.message || '学号或密码错误'
    }
  } catch (e) {
    errorMsg.value = '网络错误或服务器异常'
  }
}
</script>

<style scoped>
.s-login-container {
  width: 100%;
  max-width: 320px;
  margin: 0 auto;
  padding: 24px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.06);
}
.form-group {
  margin-bottom: 18px;
}
label {
  display: block;
  margin-bottom: 6px;
  color: #333;
  font-weight: 500;
}
input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d0d7de;
  border-radius: 4px;
  font-size: 15px;
}
.login-btn {
  width: 100%;
  padding: 10px 0;
  background: #163479;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 8px;
}
.login-btn:hover {
  background: #1d4fa0;
}
.error-msg {
  color: #e74c3c;
  margin-top: 10px;
  text-align: center;
}
</style>
