<template>
	<div class="main-page">
		<header class="top-bar">
			<nav class="menu">
				<ul>
					<li><router-link to="/Main/personal">个人信息页面</router-link></li>
					<li><router-link to="/Main/assist">自助辅助编写</router-link></li>
					<li><router-link to="/Main/class-tasks">当前班级任务</router-link></li>
					<li><router-link to="/Main/task-queue">任务队列</router-link></li>
				</ul>
			</nav>
			<div class="avatar-box">
				<img 
					:src="avatarUrl" 
					alt="avatar" 
					class="avatar" 
					@error="handleImageError"
				/>
			</div>
		</header>
			<div class="main-content">
				<router-view />
			</div>
	</div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

// 默认头像
const defaultAvatarUrl = 'https://cdn.jsdelivr.net/gh/stevenjoezhang/live2d-widget@latest/autoload.png'
const avatarUrl = ref(defaultAvatarUrl)
const router = useRouter()

onMounted(async () => {
	try {
		const res = await axios.get('/api/user/me', { withCredentials: true })
		if (res.data) {
			// 优先使用GitHub头像，如果没有则使用默认头像
			if (res.data.avatarUrl || res.data.avatar_url) {
				avatarUrl.value = res.data.avatarUrl || res.data.avatar_url
			} else {
				avatarUrl.value = defaultAvatarUrl
			}
		}
	} catch (e) {
		// 未登录，跳转到登录页
		router.replace('/login')
	}
})

// 头像加载失败时使用默认头像
const handleImageError = () => {
	avatarUrl.value = defaultAvatarUrl
}
</script>

<style scoped>
.top-bar {
	display: flex;
	align-items: center;
	justify-content: space-between;
	background: #fff;
	box-shadow: 0 2px 8px #f0f1f2;
	padding: 0 32px;
	height: 60px;
}
.menu ul {
	display: flex;
	list-style: none;
	margin: 0;
	padding: 0;
}
.menu li {
	margin-right: 32px;
}
.menu li:last-child {
	margin-right: 0;
}
.menu a {
	color: #333;
	text-decoration: none;
	font-size: 16px;
	font-weight: 500;
	transition: color 0.2s;
}
.menu a.router-link-active {
	color: #409eff;
}
.avatar-box {
	display: flex;
	align-items: center;
}
.avatar {
	width: 40px;
	height: 40px;
	border-radius: 50%;
	object-fit: cover;
	border: 2px solid #eee;
}
.main-content {
	padding: 32px;
}
</style>
