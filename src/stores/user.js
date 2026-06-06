import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
    const token = ref(localStorage.getItem('token') || '')

    const parseUserInfo = () => {
        try {
            const stored = localStorage.getItem('userInfo')
            if (stored && stored !== 'undefined' && stored !== 'null' && stored !== '{}') {
                const parsed = JSON.parse(stored)
                if (parsed && parsed.role) {
                    return parsed
                }
            }
        } catch (e) {
            console.error('解析 userInfo 失败:', e)
        }
        return null
    }

    const userInfo = ref(parseUserInfo() || {})

    const isLogin = computed(() => !!token.value && !!userInfo.value.role)
    const role = computed(() => userInfo.value?.role || '')

    const userId = computed(() => {
        const info = userInfo.value
        if (!info || !info.role) return null

        if (info.role === 'team') {
            return info.team_ID || null
        }
        if (info.role === 'teacher' || info.role === 'admin') {
            return info.teacher_ID || null
        }
        return null
    })

    const userName = computed(() => {
        const info = userInfo.value
        if (!info || !info.role) return '未知用户'

        if (info.role === 'team') {
            return info.team_name || info.teamName || '未知社团'
        }
        if (info.role === 'teacher' || info.role === 'admin') {
            return info.name || info.userName || '未知用户'
        }
        return '未知用户'
    })

    const setUserInfo = (data, authToken) => {
        const userData = { ...data }
        delete userData.password

        userInfo.value = userData
        token.value = authToken || ''

        localStorage.setItem('token', token.value)
        localStorage.setItem('userInfo', JSON.stringify(userData))
    }

    const updateUserInfo = (updates) => {
        const updated = { ...userInfo.value, ...updates }
        delete updated.password

        userInfo.value = updated
        localStorage.setItem('userInfo', JSON.stringify(updated))
    }

    const logout = () => {
        token.value = ''
        userInfo.value = {}
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
    }

    return {
        token,
        userInfo,
        isLogin,
        role,
        userId,
        userName,
        setUserInfo,
        updateUserInfo,
        logout
    }
})
