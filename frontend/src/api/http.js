import axios from 'axios'

// Methods requiring CSRF token
const NEED_CSRF = /^(post|put|patch|delete)$/i

// Lazy cached CSRF token & single-flight guard
let _csrfToken = null
let _csrfPromise = null
async function ensureCsrfToken() {
  if (_csrfToken) return _csrfToken
  if (_csrfPromise) return _csrfPromise
  _csrfPromise = axios.get('/csrf', { withCredentials: true })
    .then(res => {
      const d = res.data || {}
      _csrfToken = d.token || d._csrf || d.csrf || d?.tokenValue || d?.value || null
      return _csrfToken
    })
    .catch(() => null)
    .finally(() => { _csrfPromise = null })
  return _csrfPromise
}

const http = axios.create({
  baseURL: '/',
  withCredentials: true,
  timeout: 60000
})

http.interceptors.request.use(async (config) => {
  try {
    if (NEED_CSRF.test(config.method || '')) {
      if (!config.headers['X-CSRF-TOKEN']) {
        const token = await ensureCsrfToken()
        if (token) config.headers['X-CSRF-TOKEN'] = token
      }
    }
  } catch (e) {
    // Silently ignore token errors; backend will reject if needed
  }
  return config
})

http.interceptors.response.use(r => r, err => {
  // Optional: place to add global error toast / retry on CSRF mismatch
  return Promise.reject(err)
})

export default http
