// 获取 CSRF token 并缓存
import axios from 'axios'

let csrfToken = null

export async function getCsrfToken() {
  if (csrfToken) return csrfToken
  // Spring Security 默认暴露 /csrf 端点
  const res = await axios.get('/csrf', { withCredentials: true })
  csrfToken = res.data && (res.data.token || res.data._csrf || res.data.csrf || res.data.parameterName && res.data.headerName && res.data.token)
  // 兼容多种格式
  if (res.data && res.data.token) csrfToken = res.data.token
  else if (res.data && res.data._csrf) csrfToken = res.data._csrf
  else if (res.data && res.data.csrf) csrfToken = res.data.csrf
  return csrfToken
}
