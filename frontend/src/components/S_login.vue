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
import http from '@/api/http'
import { useRouter } from 'vue-router'

const router = useRouter()

const studentId = ref('')
const password = ref('')
const errorMsg = ref('')

async function handleLogin() {
  errorMsg.value = ''
  try {
    // 获取 CSRF token
    const res = await http.post('/api/slogin', {
      studentId: studentId.value,
      password: password.value
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
.s-login-container { width:100%; max-width:320px; margin:0 auto; padding: var(--space-6); background: var(--color-surface); border-radius: var(--radius-md); box-shadow: var(--shadow-pop); }
.form-group { margin-bottom: 18px; }
label { display:block; margin-bottom:6px; color: var(--color-text-primary); font-weight: var(--font-weight-medium); }
input { width:100%; padding:8px 12px; border:1px solid var(--color-border-strong); border-radius: var(--radius-xs); font-size: var(--font-size-md); background: var(--color-surface); transition: border-color var(--transition-base), box-shadow var(--transition-fast); }
input:focus { outline:none; border-color: var(--color-primary); box-shadow: 0 0 0 1px var(--color-primary), var(--shadow-focus-blue); }
.login-btn { width:100%; padding:10px 0; background: var(--color-primary-alt); color:#fff; border:none; border-radius: var(--radius-xs); font-size: var(--font-size-lg); font-weight: var(--font-weight-semibold); cursor:pointer; margin-top: var(--space-2); transition: background var(--transition-base), transform var(--transition-fast); }
.login-btn:hover { background: var(--color-primary-alt-hover); }
.error-msg { color: var(--color-red-400); margin-top: var(--space-3); text-align:center; font-size: var(--font-size-sm); }
</style>
