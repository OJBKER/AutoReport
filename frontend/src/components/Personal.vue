<template>
  <div class="personal-container">
    <h1>个人中心</h1>
    <div v-if="user">
      <img :src="user.avatar_url || avatarPlaceholder" @error="onAvatarError" alt="avatar" class="avatar" />
      
      <!-- 基本信息 -->
      <div class="info-section">
        <h3>基本信息</h3>
        <div v-if="user.loginType === 'github'">
          <p><strong>用户ID：</strong>{{ user.id }}</p>
          <p><strong>用户名：</strong>{{ user.login }}</p>
          <p v-if="user.githubId"><strong>GitHub ID：</strong>{{ user.githubId }}</p>
        </div>
        <div v-else-if="user.loginType === 'school'">
          <p><strong>学号：</strong>{{ user.studentId }}</p>
          <p v-if="user.name"><strong>姓名：</strong>{{ user.name }}</p>
          <p v-if="user.userId"><strong>用户ID：</strong>{{ user.userId }}</p>
          <p v-if="user.githubId"><strong>GitHub ID：</strong>{{ user.githubId }}</p>
          <div class="github-bind-area">
            <template v-if="user.canBindGithub">
              <button
                class="github-bind-button"
                @click="startGithubBind"
                :disabled="githubBindLoading"
              >
                {{ githubBindLoading ? '跳转 GitHub 授权中...' : '绑定 GitHub 账号' }}
              </button>
              <p v-if="githubBindMessage" class="github-bind-message">{{ githubBindMessage }}</p>
            </template>
            <template v-else-if="user.githubId">
              <span class="github-bind-message success">GitHub 账号已绑定</span>
            </template>
          </div>
        </div>
        <div v-else>
          <p><strong>用户ID：</strong>{{ user.id || user.userId }}</p>
          <p><strong>用户名：</strong>{{ user.login || user.studentId }}</p>
          <p v-if="user.name"><strong>姓名：</strong>{{ user.name }}</p>
        </div>
      </div>

      <!-- 班级信息 -->
      <div class="info-section" v-if="user.classes">
        <h3>班级信息</h3>
        <p><strong>班级ID：</strong>{{ user.classes.classId }}</p>
        <p><strong>班级名称：</strong>{{ user.classes.name }}</p>
      </div>

      <!-- GitHub用户绑定学号和班级 -->
      <div class="info-section" v-if="user.loginType === 'github' && !user.classes">
        <h3>绑定学号和班级</h3>
  <p class="bind-hint">
          <strong>提示：</strong>请绑定您的学号和班级以享受完整功能
        </p>
        
        <div class="bind-form">
          <div class="form-group">
            <label for="studentNumber">学号：</label>
            <input 
              type="text" 
              id="studentNumber"
              v-model="bindForm.studentNumber" 
              placeholder="请输入学号"
              class="form-input"
              :disabled="binding"
            />
          </div>
          
          <div class="form-group">
            <label for="classId">班级：</label>
            <select 
              id="classId"
              v-model="bindForm.classId" 
              class="form-select"
              :disabled="binding"
            >
              <option value="">请选择班级</option>
              <option 
                v-for="clazz in availableClasses" 
                :key="clazz.classId" 
                :value="clazz.classId"
              >
                {{ clazz.name }}
              </option>
            </select>
          </div>
          
          <div class="form-actions">
            <button 
              @click="bindStudentInfo" 
              class="bind-button"
              :disabled="binding || !bindForm.studentNumber || !bindForm.classId"
            >
              {{ binding ? '绑定中...' : '绑定信息' }}
            </button>
          </div>
          
          <div v-if="bindMessage" class="bind-message" :class="bindSuccess ? 'success' : 'error'">
            {{ bindMessage }}
          </div>
        </div>
      </div>

      <!-- 登录状态 -->
      <div class="info-section">
        <h3>登录状态</h3>
        <div class="login-info">
          <span class="login-method-text">
            当前登录方式：
            <template v-if="user.loginType === 'github'">GitHub 登录</template>
            <template v-else-if="user.loginType === 'school'">学校账号登录</template>
            <template v-else>未知</template>
          </span>
          <br v-if="user.loginType === 'github'" />
          <div v-if="user.loginType === 'github'" class="github-bind-status">
            <span v-if="user.classes" class="status-text success">GitHub账号已绑定本地账号</span>
            <span v-else-if="user.isNewUser" class="status-text warning">新用户，请绑定学号和班级</span>
            <span v-else class="status-text warning">GitHub账号尚未绑定本地账号</span>
          </div>
          <div v-if="user.dbUser" class="db-status-text">
            数据库状态：{{ user.dbUser }}
          </div>
        </div>
      </div>
    </div>
    <div v-else>
      <p>未获取到用户信息，请先登录。</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import http from '@/api/http'
import avatarPlaceholder from '@/assets/avatar-placeholder.svg'

const user = ref(null)
function onAvatarError(e){ e.target.src = avatarPlaceholder }
const binding = ref(false)
const bindMessage = ref('')
const bindSuccess = ref(false)
const availableClasses = ref([])
const githubBindLoading = ref(false)
const githubBindMessage = ref('')

const resolveRedirectUrl = (url) => {
  if (!url) return null
  if (/^https?:\/\//i.test(url)) return url
  const origin = window.location.origin
  const isDev = origin.includes(':5173')
  const backendOrigin = isDev ? origin.replace(':5173', ':8080') : origin
  if (url.startsWith('/')) return `${backendOrigin}${url}`
  return `${backendOrigin}/${url}`
}

// 绑定表单数据
const bindForm = ref({
  studentNumber: '',
  classId: ''
})

onMounted(async () => {
  try {
    const { data } = await http.get('/api/user/me')
    user.value = data
    githubBindMessage.value = data.githubBindMessage || ''
    await loadAvailableClasses()
  } catch (e) {
    console.error('获取用户信息失败:', e)
    user.value = null
  }
})

// 获取可用班级列表
const loadAvailableClasses = async () => {
  try {
    const { data } = await http.get('/api/classes')
    availableClasses.value = Array.isArray(data)? data : []
  } catch (e) {
    console.error('获取班级列表失败:', e)
    availableClasses.value = []
  }
}

// 绑定学号和班级信息
const bindStudentInfo = async () => {
  if (!bindForm.value.studentNumber || !bindForm.value.classId) {
    bindMessage.value = '请填写完整的学号和班级信息'
    bindSuccess.value = false
    return
  }

  binding.value = true
  bindMessage.value = ''

  try {
    const { data: result } = await http.put('/api/user/bind-student-info', {
      studentNumber: bindForm.value.studentNumber,
      classId: bindForm.value.classId
    })

    if (result.success) {
      bindMessage.value = result.message
      bindSuccess.value = true
      setTimeout(async () => {
  const { data: refreshed } = await http.get('/api/user/me')
  user.value = refreshed
        bindMessage.value = ''
      }, 2000)
    } else {
      bindMessage.value = result.message || '绑定失败'
      bindSuccess.value = false
    }
  } catch (e) {
    console.error('绑定失败:', e)
    bindMessage.value = '网络错误，请稍后重试'
    bindSuccess.value = false
  } finally {
    binding.value = false
  }
}

const startGithubBind = async () => {
  githubBindMessage.value = ''
  githubBindLoading.value = true
  try {
    const { data } = await http.post('/api/user/request-github-bind')
    if (data.success && data.redirectUrl) {
      githubBindMessage.value = '正在跳转至 GitHub 授权...'
      const targetUrl = resolveRedirectUrl(data.redirectUrl)
      if (targetUrl) {
        window.location.href = targetUrl
      } else {
        githubBindMessage.value = '无法解析跳转地址'
      }
    } else {
      githubBindMessage.value = data.message || '无法发起 GitHub 绑定'
    }
  } catch (e) {
    console.error('发起 GitHub 绑定失败:', e)
    githubBindMessage.value = '网络错误，请稍后重试'
  } finally {
    githubBindLoading.value = false
  }
}
</script>

<style scoped>
.personal-container { max-width:600px; margin:60px auto; padding: var(--space-8); background: var(--color-surface); border-radius: var(--radius-md); box-shadow: var(--shadow-lg); text-align:left; }
.avatar { width:80px; height:80px; border-radius:50%; margin-bottom: var(--space-4); display:block; margin-left:auto; margin-right:auto; }
.info-section { margin-bottom: var(--space-6); padding: var(--space-4); background: var(--color-gray-75); border-radius: var(--radius-sm); border-left:4px solid var(--color-primary); }
.info-section h3 { margin-top:0; margin-bottom: var(--space-3); color: var(--color-primary); }
.bind-hint { color: var(--color-yellow-500); margin-bottom: var(--space-4); font-size: var(--font-size-sm); }
.login-method-text { color: var(--color-primary); font-size: var(--font-size-md); }
.github-bind-status { margin-top: var(--space-2); }
.status-text { font-size: var(--font-size-sm); }
.status-text.success { color: var(--color-green-550); }
.status-text.warning { color: var(--color-yellow-500); }
.db-status-text { margin-top: var(--space-2); font-size: var(--font-size-sm); color: var(--color-text-secondary); }
/* 绑定表单样式 */
.bind-form { margin-top: var(--space-4); }
.form-group { margin-bottom: var(--space-4); }
.form-group label { display:block; margin-bottom:6px; font-weight: var(--font-weight-medium); color: var(--color-text-primary); font-size: var(--font-size-sm); }
.form-input, .form-select { width:100%; padding:10px 12px; border:1px solid var(--color-border-strong); border-radius: var(--radius-xs); font-size: var(--font-size-sm); transition: border-color var(--transition-base), box-shadow var(--transition-fast); background: var(--color-surface); box-sizing:border-box; }
.form-input:focus, .form-select:focus { outline:none; border-color: var(--color-primary); box-shadow: 0 0 0 1px var(--color-primary), var(--shadow-focus-blue); }
.form-input:disabled, .form-select:disabled { background: var(--color-gray-100); color: var(--color-text-muted); cursor:not-allowed; }
.form-input::placeholder { color: var(--color-text-muted); }
.form-actions { margin-top:20px; text-align:center; }
.bind-button { background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-hover) 100%); color:#fff; border:none; padding:12px 32px; border-radius: var(--radius-sm); font-size:16px; font-weight: var(--font-weight-medium); cursor:pointer; transition: all var(--transition-base); box-shadow: 0 2px 8px rgba(64,158,255,0.3); }
.bind-button:hover:not(:disabled) { background: linear-gradient(135deg, var(--color-primary-hover) 0%, var(--color-primary-alt-hover) 100%); box-shadow: 0 4px 12px rgba(64,158,255,0.4); transform: translateY(-2px); }
.bind-button:disabled { background: var(--color-gray-400); cursor:not-allowed; box-shadow:none; transform:none; }
.bind-message { margin-top: var(--space-4); padding: var(--space-3); border-radius: var(--radius-xs); font-size: var(--font-size-sm); text-align:center; font-weight: var(--font-weight-medium); }
.bind-message.success { background: var(--color-blue-50); color: var(--color-green-550); border:1px solid var(--color-green-border); }
.bind-message.error { background: var(--color-red-bg); color: var(--color-red-550); border:1px solid var(--color-red-border); }
.github-bind-area { margin-top: var(--space-3); }
.github-bind-button { background: var(--color-surface); border:1px solid var(--color-primary); color: var(--color-primary); padding:10px 16px; border-radius: var(--radius-xs); font-size: var(--font-size-sm); cursor:pointer; transition: all var(--transition-fast); }
.github-bind-button:hover:not(:disabled) { background: var(--color-primary); color:#fff; }
.github-bind-button:disabled { opacity:0.6; cursor:not-allowed; }
.github-bind-message { margin-top: var(--space-2); font-size: var(--font-size-sm); color: var(--color-text-secondary); }
.github-bind-message.success { color: var(--color-green-550); }
</style>
