import { createRouter, createWebHistory } from 'vue-router'

import Login from '../views/Login.vue'
import Main from '../views/Main.vue'
import Admin from '../views/Admin.vue'

import Personal from '../components/Personal.vue'
import Assist from '../components/Assist.vue'
import ClassTasks from '../components/ClassTasks.vue'
import TaskQueue from '../components/TaskQueue.vue'

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
router.beforeEach((to, from, next) => {
  const needAuth = to.path.startsWith('/Main') || to.path.startsWith('/admin');
  if (!needAuth) { return next(); }
  fetch('/api/user/me', { credentials: 'include' })
    .then(res => res.json())
    .then(data => {
      const loggedIn = data && (data.id || data.userId);
      if (!loggedIn) { return next('/login'); }
      if (to.path.startsWith('/admin')) {
        if (!data.isAdmin) { return next('/Main'); }
      } else {
        // 已登录且是管理员：如果访问 /Main 且还没跳去 admin，可在 Main.vue 自己做一次自动跳转，避免这里强制。
      }
      next();
    })
    .catch(() => next('/login'));
});

export default router
