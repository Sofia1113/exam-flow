import axios, { type AxiosRequestConfig } from 'axios'

/**
 * HTTP 客户端:注入 Bearer 令牌;code!=0 统一提示;401 跳登录页。
 */
const request = axios.create({
  baseURL: '/api/v1',
  timeout: 10_000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('examflow_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number') {
      if (body.code === 0) return body.data
      if (body.code === 11001) {
        localStorage.removeItem('examflow_token')
        window.location.href = '/login'
      }
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('examflow_token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

/** 类型化包装:返回已解包的 data(与拦截器行为一致)。 */
export default {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return request.get(url, config) as Promise<T>
  },
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return request.post(url, data, config) as Promise<T>
  },
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return request.put(url, data, config) as Promise<T>
  }
}
