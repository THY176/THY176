import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
    baseURL: 'http://localhost:2026',
    timeout: 10000
})

request.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers.Authorization = token.startsWith('Bearer ') ? token : `Bearer ${token}`
        }
        return config
    },
    error => {
        return Promise.reject(error)
    }
)

request.interceptors.response.use(
    response => {
        const res = response.data

        if (res && typeof res === 'object' && !res.code) {
            return { code: '200', data: res }
        }

        if (res.code === '200') {
            return res
        }

        if (res.code === '401' || res.code === '403') {
            localStorage.removeItem('token')
            localStorage.removeItem('userInfo')
        }

        if (res.code && res.code !== '200') {
            ElMessage.error(res.msg || '请求失败')
            return Promise.reject(new Error(res.msg || '请求失败'))
        }

        return res
    },
    error => {
        if (error.response?.status === 401 || error.response?.status === 403) {
            localStorage.removeItem('token')
            localStorage.removeItem('userInfo')
            if (window.location.pathname !== '/') {
                window.location.href = '/'
            }
        }
        ElMessage.error(error.response?.data?.msg || error.message || '网络错误')
        return Promise.reject(error)
    }
)

export default request
