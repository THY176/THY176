import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
    baseURL: 'http://localhost:2026',
    timeout: 10000
})

// 璇锋眰鎷︽埅鍣?
request.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers.Authorization = token
        }
        return config
    },
    error => {
        return Promise.reject(error)
    }
)

// 鍝嶅簲鎷︽埅鍣?
request.interceptors.response.use(
    response => {
        const res = response.data
        console.log('鍝嶅簲鎷︽埅鍣ㄦ敹鍒?', res)

        // 濡傛灉鍚庣杩斿洖鐨勬槸瀵硅薄浣嗘病鏈?code 瀛楁锛堢洿鎺ヨ繑鍥炲疄浣擄級
        if (res && typeof res === 'object' && !res.code) {
            return { code: '200', data: res }
        }

        // 姝ｅ父杩斿洖 code="200" 鐨勬儏鍐?
        if (res.code === '200') {
            return res
        }

        // 鍏朵粬鎯呭喌锛坈ode 瀛樺湪浣嗕笉鏄?200锛?
        if (res.code && res.code !== '200') {
            ElMessage.error(res.msg || '璇锋眰澶辫触')
            return Promise.reject(new Error(res.msg || '璇锋眰澶辫触'))
        }

        // 榛樿杩斿洖
        return res
    },
    error => {
        console.error('璇锋眰閿欒:', error)
        ElMessage.error(error.message || '缃戠粶閿欒')
        return Promise.reject(error)
    }
)

export default request
