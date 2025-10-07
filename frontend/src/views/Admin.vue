<template>
  <div class="admin-wrapper">
    <header class="top-bar">
      <nav class="menu">
        <ul>
          <li class="brand">管理后台</li>
          <li class="user-label">{{ userDisplay }}</li>
        </ul>
      </nav>
      <div class="right-tools">
        <button class="back-btn" @click="goMain">返回主界面</button>
        <img :src="avatarUrl" alt="avatar" class="avatar" @error="onAvatarError" />
      </div>
    </header>
    <div class="admin-page">
      <nav class="menu admin-tabs">
        <ul>
          <li :class="{active:activeTab==='tasks'}" @click="activeTab='tasks'">任务统计</li>
          <li :class="{active:activeTab==='users'}" @click="activeTab='users'">用户列表</li>
          <li :class="{active:activeTab==='postTask'}" @click="activeTab='postTask'">发布任务</li>
        </ul>
      </nav>
      <main class="admin-main">
        <AdminTaskStats v-if="activeTab==='tasks'" />
        <AdminUserList v-else-if="activeTab==='users'" />
        <AdminPostTask v-else-if="activeTab==='postTask'" />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import avatarPlaceholder from '@/assets/avatar-placeholder.svg'
import AdminPostTask from '@/components/AdminPostTask.vue'
import AdminTaskStats from '@/components/AdminTaskStats.vue'
import AdminUserList from '@/components/AdminUserList.vue'

const router = useRouter()
const activeTab = ref('tasks')
const userInfo = ref(null)
const avatarUrl = ref(avatarPlaceholder)

const userDisplay = computed(()=>{
  if(!userInfo.value) return '未获取'
  return userInfo.value.name || userInfo.value.login || userInfo.value.studentNumber || userInfo.value.id || '未知'
})

async function fetchUser(){
  try {
    const { data } = await http.get('/api/user/me')
    if(!data) return
    userInfo.value=data
    if(data.avatarUrl || data.avatar_url){
      avatarUrl.value = data.avatarUrl || data.avatar_url
    } else {
      avatarUrl.value = avatarPlaceholder
    }
    if(!data.isAdmin){ router.replace('/Main') }
  } catch(e){}
}

onMounted(()=>{ fetchUser() })

function goMain(){ router.replace('/Main/assist') }
function onAvatarError(){ avatarUrl.value = avatarPlaceholder }
</script>

<style scoped>
.top-bar { display:flex; align-items:center; justify-content:space-between; background:#fff; box-shadow:0 2px 8px #f0f1f2; padding:0 32px; height:60px; position:sticky; top:0; z-index:10; }
.menu ul { display:flex; list-style:none; margin:0; padding:0; gap:26px; align-items:center; }
.menu .brand { font-weight:600; font-size:18px; }
.menu .user-label { font-size:14px; color:#555; }
.right-tools { display:flex; gap:16px; align-items:center; }
.avatar { width:40px; height:40px; border-radius:50%; object-fit:cover; border:2px solid #eee; }
.back-btn { padding:6px 14px; background:#2563eb; color:#fff; border:none; border-radius:6px; cursor:pointer; font-size:13px; }
.admin-page { max-width: 1120px; margin: 28px auto 80px; padding: 24px 32px 40px; background: #fff; border-radius: 14px; box-shadow: 0 4px 18px -2px rgba(0,0,0,0.08); font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto; }
.admin-tabs { margin: -4px 0 20px; }
.admin-tabs ul { display:flex; list-style:none; margin:0; padding:0; }
.admin-tabs li { margin-right:32px; cursor:pointer; font-size:16px; font-weight:500; color:#333; position:relative; transition:color .2s; }
.admin-tabs li:last-child { margin-right:0; }
.admin-tabs li.active, .admin-tabs li:hover { color:#2563eb; }
.admin-tabs li.active::after { content:""; position:absolute; left:0; right:0; bottom:-6px; height:2px; background:#2563eb; border-radius:2px; }
.admin-main { display:flex; flex-direction:column; gap:28px; }
</style>
