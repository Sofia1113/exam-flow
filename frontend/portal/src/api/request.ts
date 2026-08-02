import axios from 'axios'

/**
 * HTTP 客户端封装:统一响应 Result{ code, message, data }(见 TDD §5.1)。
 * TODO: 登录后注入 Bearer Token;code!=0 统一提示。
 */
const request = axios.create({
  baseURL: '/api/v1',
  timeout: 10_000
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number' && body.code !== 0) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body
  },
  (error) => Promise.reject(error)
)

export default request
