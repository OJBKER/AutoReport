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
      <div class="admin-header-lite">
        <nav class="admin-nav">
          <button @click="activeTab='overview'" :class="{active:activeTab==='overview'}">概览</button>
          <button @click="activeTab='tasks'" :class="{active:activeTab==='tasks'}">任务统计</button>
          <button @click="activeTab='users'" :class="{active:activeTab==='users'}">用户列表</button>
        </nav>
      </div>
      <main class="admin-main">
        <section v-if="activeTab==='overview'" class="panel">
          <h2>系统概览</h2>
          <ul class="kv-list">
            <li><strong>用户数：</strong>{{ metrics.userCount ?? '-' }}</li>
            <li><strong>任务数：</strong>{{ metrics.taskCount ?? '-' }}</li>
            <li><strong>提交数：</strong>{{ metrics.submissionCount ?? '-' }}</li>
          </ul>
          <button class="refresh-btn" @click="loadMetrics" :disabled="loadingMetrics">{{ loadingMetrics? '加载中...' : '刷新概览' }}</button>
        </section>
        <section v-else-if="activeTab==='tasks'" class="panel">
          <h2>任务统计 (示例占位)</h2>
          <p class="placeholder">此区域可扩展为任务完成率、模板使用分布等可视化。</p>
        </section>
        <section v-else-if="activeTab==='users'" class="panel">
          <h2>同班级学生 ({{ classUsers.classId || '—' }})</h2>
          <div v-if="usersLoading" class="placeholder">加载中...</div>
          <div v-else>
            <div v-if="classUsers.error" class="placeholder" style="color:#b91c1c;">{{ classUsers.error }}</div>
            <table v-else class="user-table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>姓名</th>
                  <th>学号</th>
                  <th>GitHub</th>
                  <th>管理员</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(u,i) in classUsers.list" :key="u.id">
                  <td>{{ i+1 }}</td>
                  <td>{{ u.name || '—' }}</td>
                  <td>{{ u.studentNumber || '—' }}</td>
                  <td>{{ u.githubId || '—' }}</td>
                  <td>{{ u.isAdmin? '是':'否' }}</td>
                </tr>
                <tr v-if="!classUsers.list.length">
                  <td colspan="5" style="text-align:center;color:#666;">暂无数据</td>
                </tr>
              </tbody>
            </table>
            <div class="table-footer">共 {{ classUsers.count || 0 }} 人</div>
            <button class="refresh-btn" style="margin-top:12px" @click="loadClassUsers" :disabled="usersLoading">{{ usersLoading? '刷新中...' : '重新加载' }}</button>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const activeTab = ref('overview')
const userInfo = ref(null)
const metrics = ref({})
const loadingMetrics = ref(false)
const classUsers = ref({ list: [], count: 0, classId: null, error: null })
const usersLoading = ref(false)
const avatarUrl = ref('https://cdn.jsdelivr.net/gh/stevenjoezhang/live2d-widget@latest/autoload.png')

const userDisplay = computed(()=>{
  if(!userInfo.value) return '未获取';
  return userInfo.value.name || userInfo.value.login || userInfo.value.studentNumber || userInfo.value.id || '未知';
})

async function fetchUser(){
  try{ const r=await fetch('/api/user/me',{credentials:'include'}); if(!r.ok) return; const d=await r.json(); userInfo.value=d; if(d.avatarUrl || d.avatar_url){ avatarUrl.value = d.avatarUrl || d.avatar_url } if(!d.isAdmin){ router.replace('/Main'); } }catch(e){}
}

async function loadMetrics(){
  loadingMetrics.value=true;
  try { metrics.value = { userCount: '—', taskCount: '—', submissionCount: '—' } } finally { loadingMetrics.value=false }
}

onMounted(()=>{ fetchUser(); loadMetrics(); })

function goMain(){ router.replace('/Main/assist') }
function onAvatarError(){ avatarUrl.value='https://cdn.jsdelivr.net/gh/stevenjoezhang/live2d-widget@latest/autoload.png' }

async function loadClassUsers(){
  usersLoading.value = true
  classUsers.value.error = null
  try {
    const r = await fetch('/api/admin/class-users', { credentials: 'include' })
    if(!r.ok){ classUsers.value.error = '请求失败'; return }
    const d = await r.json()
    if(!d.success){ classUsers.value.error = d.error || '无权限或未登录'; return }
    classUsers.value = { list: d.data || [], count: d.count || 0, classId: d.classId, error: null }
  } catch(e){
    classUsers.value.error = '异常: '+ e
  } finally {
    usersLoading.value = false
  }
}

// 当切换到用户标签时自动加载
watch(()=>activeTab.value, v=>{ if(v==='users' && classUsers.value.list.length===0 && !usersLoading.value){ loadClassUsers() } })
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
.admin-header-lite { display:flex; justify-content:flex-start; margin: -8px 0 18px; }
.admin-nav { display:flex; gap:10px; flex-wrap:wrap; }
.admin-nav button { background:#fff; border:1px solid #c9d5e4; padding:6px 14px; font-size:.85rem; border-radius:20px; cursor:pointer; transition:.18s; }
.admin-nav button.active, .admin-nav button:hover { background:#2563eb; color:#fff; border-color:#2563eb; }
.admin-main { display:flex; flex-direction:column; gap:28px; }
.panel { background:#f8fafc; border:1px solid #dde5ef; padding:20px 22px 24px; border-radius:12px; box-shadow:0 2px 6px -2px rgba(0,0,0,0.04); }
.panel h2 { margin:0 0 14px; font-size:1.1rem; }
.kv-list { list-style:none; padding:0; margin:0 0 16px; display:grid; grid-template-columns:repeat(auto-fit,minmax(160px,1fr)); gap:10px 18px; font-size:.88rem; }
.placeholder { font-size:.85rem; color:#6b7280; background:#fff; padding:12px 14px; border:1px dashed #d1dae4; border-radius:8px; }
.refresh-btn { background:#2563eb; color:#fff; border:none; padding:8px 18px; font-size:.8rem; border-radius:6px; cursor:pointer; box-shadow:0 2px 6px rgba(0,0,0,0.08); }
.refresh-btn:disabled { opacity:.55; cursor:progress; }
.user-table { width:100%; border-collapse:collapse; background:#fff; font-size:.8rem; }
.user-table th, .user-table td { padding:8px 10px; border:1px solid #e2e8f0; }
.user-table thead th { background:#f1f5f9; font-weight:600; }
.table-footer { margin-top:6px; font-size:.75rem; color:#475569; }
</style>
