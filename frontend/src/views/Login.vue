<template>
  <div class="login-bg">
    <div class="login-container">
      <h1>登录</h1>
  <button class="github-btn" @click="loginWithGithub">
        <svg viewBox="0 0 16 16" width="18" height="18" fill="currentColor" style="vertical-align:middle;margin-right:8px;"><path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.01.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.11.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0 0 16 8c0-4.42-3.58-8-8-8z"/></svg>
        使用 GitHub 登录
      </button>
        <div style="width:100%;display:flex;align-items:center;justify-content:center;margin-top:12px;">
          <button class="password-btn" @click="loginWithPassword" v-if="!showSLogin">
            <img src="../assets/S_Logo.jpg" alt="S_Logo" style="height:22px;margin-right:8px;vertical-align:middle;" />
            使用密码登录
          </button>
        </div>
        <div v-if="showSLogin" style="width:100%;margin-top:16px;display:flex;justify-content:center;">
          <S_login />
        </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import S_login from '../components/S_login.vue'

const showSLogin = ref(false)

function loginWithGithub() {
  window.location.href = 'http://localhost:8080/oauth2/authorization/github'
}
function loginWithPassword() {
  showSLogin.value = true
}
</script>

<style scoped>
body, .login-bg {
  min-height: 100vh;
  min-width: 100vw;
  background: #f6f8fa;
}
.login-bg {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  min-height: 100vh;
  width: 100vw;
}
.login-container {
  margin-top: 10vh;
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 8px 24px rgba(149,157,165,0.2);
  padding: 32px 32px 24px 32px;
  min-width: 320px;
  max-width: 360px;
  display: flex;
  flex-direction: column;
  align-items: center;
  transition: all 0.2s;
}
.github-logo {
  margin-bottom: 16px;
  color: #24292f;
}
h1 {
  font-size: 1.4rem;
  font-weight: 600;
  color: #24292f;
  margin-bottom: 24px;
}
.github-btn {
  width: 100%;
  padding: 10px 0;
  font-size: 15px;
  background: #24292f;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  margin-top: 8px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}
.github-btn:hover {
  background: #444d56;
}

.password-btn {
  width: 100%;
  padding: 10px 0;
  font-size: 15px;
  background: #f6f8fa;
  color: #24292f;
  border: 1px solid #d0d7de;
  border-radius: 6px;
  cursor: pointer;
  margin-top: 12px;
  font-weight: 500;
  transition: background 0.2s, border 0.2s;
}
.password-btn:hover {
  background: #eaeef2;
  border-color: #b6bac0;
}
button {
  padding: 8px 24px;
  min-width: 80px;
  font-size: 15px;
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-top: 24px;
  transition: all 0.2s;
}
h1 {
  font-size: 1.6rem;
  margin-bottom: 24px;
}
</style>
