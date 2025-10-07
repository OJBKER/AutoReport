<template>
  <section class="panel">
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
  <button class="btn btn-primary" style="margin-top:12px" @click="loadClassUsers" :disabled="usersLoading">{{ usersLoading? '刷新中...' : '重新加载' }}</button>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import http from '@/api/http'

const classUsers = ref({ list: [], count: 0, classId: null, error: null })
const usersLoading = ref(false)

async function loadClassUsers(){
  usersLoading.value = true
  classUsers.value.error = null
  try {
    const { data } = await http.get('/api/admin/class-users')
    if(!data.success){ classUsers.value.error = data.error || '无权限或未登录'; return }
    classUsers.value = { list: data.data || [], count: data.count || 0, classId: data.classId, error: null }
  } catch(e){
    classUsers.value.error = '异常: '+ e
  } finally {
    usersLoading.value = false
  }
}

onMounted(()=>{ loadClassUsers() })
</script>

<style scoped></style>
