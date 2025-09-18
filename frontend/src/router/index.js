import { createRouter, createWebHistory } from 'vue-router'

import Home from '../views/Home.vue'
import About from '../views/About.vue'
import Personal from '../views/Personal.vue'

const routes = [
  { path: '/', redirect: '/home' },
  { path: '/home', component: Home },
  { path: '/about', component: About },
  { path: '/personal', component: Personal }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
