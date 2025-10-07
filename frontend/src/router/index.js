import { createRouter, createWebHistory } from 'vue-router'

import Login from '../views/Login.vue'
import Main from '../views/Main.vue'
import Admin from '../views/Admin.vue'

import Personal from '../components/Personal.vue'
import Assist from '../components/Assist.vue'
import ClassTasks from '../components/ClassTasks.vue'
import TaskQueue from '../components/TaskQueue.vue'
import http from '@/api/http'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: Login },
  { path: '/admin', component: Admin },
  {
    path: '/Main',
    component: Main,
    children: [
      { path: 'personal', component: Personal },
      { path: 'assist', component: Assist },
      { path: 'class-tasks', component: ClassTasks },
      { path: 'task-queue', component: TaskQueue }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})


// 路由守卫：未登录用户不能访问 /Main 及其子路由
router.beforeEach(async (to, from, next) => {
  const needAuth = to.path.startsWith('/Main') || to.path.startsWith('/admin')
  if (!needAuth) return next()
  try {
    const { data } = await http.get('/api/user/me')
    const loggedIn = data && (data.id || data.userId)
    if(!loggedIn) return next('/login')
    if(to.path.startsWith('/admin') && !data.isAdmin) return next('/Main')
    next()
  } catch(e){
    return next('/login')
  }
})

export default router
