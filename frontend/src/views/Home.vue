<template>
  <div class="login-container">
    <h1>首页</h1>
    <p>欢迎来到首页！</p>
    <button @click="loginWithGithub">使用 GitHub 登录</button>
    <button @click="goPersonal" style="margin-left:16px;">个人中心</button>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
function loginWithGithub() {
  window.location.href = 'http://localhost:8080/oauth2/authorization/github'
}
function goPersonal() {
  router.push('/personal')
}
</script>

