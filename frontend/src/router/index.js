import { createRouter, createWebHistory } from 'vue-router'

import Login from '../views/Login.vue'
import Main from '../views/Main.vue'

import Personal from '../components/Personal.vue'
import Assist from '../components/Assist.vue'
import ClassTasks from '../components/ClassTasks.vue'
import TaskQueue from '../components/TaskQueue.vue'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: Login },
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
router.beforeEach((to, from, next) => {
  // 只保护 /Main 相关路由
  if (to.path.startsWith('/Main')) {
    // 检查本地是否有登录状态（可根据实际情况调整）
    // 这里简单通过调用 /api/user/me 判断
    fetch('/api/user/me', { credentials: 'include' })
      .then(res => res.json())
      .then(data => {
        // 判断有无有效登录信息（id 或 userId 字段存在即可）
        if (data && (data.id || data.userId)) {
          next();
        } else {
          next('/login');
        }
      })
      .catch(() => {
        next('/login');
      });
  } else {
    next();
  }
});

export default router
