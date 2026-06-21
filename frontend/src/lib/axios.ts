import axios from 'axios'

const api = axios.create()

// Attach JWT on every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// On 401 — try refresh, then retry original request
let isRefreshing = false
let failedQueue: Array<{ resolve: (token: string) => void; reject: (err: unknown) => void }> = []

const processQueue = (err: unknown, token: string | null) => {
  failedQueue.forEach((p) => (err ? p.reject(err) : p.resolve(token!)))
  failedQueue = []
}

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config
    if (error.response?.status !== 401 || original._retry) return Promise.reject(error)

    if (isRefreshing) {
      return new Promise<string>((resolve, reject) => {
        failedQueue.push({ resolve, reject })
      }).then((token) => {
        original.headers.Authorization = `Bearer ${token}`
        return api(original)
      })
    }

    original._retry = true
    isRefreshing = true
    const refreshToken = localStorage.getItem('refreshToken')

    if (!refreshToken) {
      localStorage.clear()
      window.location.href = '/login'
      return Promise.reject(error)
    }

    try {
      const { data } = await axios.post('/api/v1/auth/refreshToken', { refreshToken })
      const newToken: string = data.message
      localStorage.setItem('accessToken', newToken)
      processQueue(null, newToken)
      original.headers.Authorization = `Bearer ${newToken}`
      return api(original)
    } catch (err) {
      processQueue(err, null)
      localStorage.clear()
      window.location.href = '/login'
      return Promise.reject(err)
    } finally {
      isRefreshing = false
    }
  },
)

export default api
