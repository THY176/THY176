import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
    const getStoredToken = () => sessionStorage.getItem('token') || ''
    const getStoredUserInfo = () => sessionStorage.getItem('userInfo')
    const setStoredAuth = (authToken, userData) => {
        sessionStorage.setItem('token', authToken || '')
        sessionStorage.setItem('userInfo', JSON.stringify(userData))
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
    }
    const clearStoredAuth = () => {
        sessionStorage.removeItem('token')
        sessionStorage.removeItem('userInfo')
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
    }

    const token = ref(getStoredToken())

    const normalizeUserInfo = (data, fallbackAccount) => {
        const userData = { ...(data || {}) }
        delete userData.password

        if (userData.role === 'team') {
            userData.team_ID = userData.team_ID || userData.teamId || userData.id || fallbackAccount || null
            userData.id = userData.id || userData.team_ID
        }

        if (userData.role === 'teacher' || userData.role === 'admin') {
            userData.teacher_ID = userData.teacher_ID || userData.teacherId || userData.id || fallbackAccount || null
            userData.id = userData.id || userData.teacher_ID
        }

        return userData
    }

    const parseUserInfo = () => {
        try {
            const stored = getStoredUserInfo()
            if (stored && stored !== 'undefined' && stored !== 'null' && stored !== '{}') {
                const parsed = JSON.parse(stored)
                if (parsed && parsed.role) {
                    const normalized = normalizeUserInfo(parsed)
                    sessionStorage.setItem('userInfo', JSON.stringify(normalized))
                    return normalized
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
            return info.team_ID || info.teamId || info.id || null
        }
        if (info.role === 'teacher' || info.role === 'admin') {
            return info.teacher_ID || info.teacherId || info.id || null
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

    const setUserInfo = (data, authToken, fallbackAccount) => {
        const userData = normalizeUserInfo(data, fallbackAccount)

        userInfo.value = userData
        token.value = authToken || ''

        setStoredAuth(token.value, userData)
    }

    const updateUserInfo = (updates) => {
        const updated = normalizeUserInfo({ ...userInfo.value, ...updates })

        userInfo.value = updated
        sessionStorage.setItem('userInfo', JSON.stringify(updated))
        localStorage.removeItem('userInfo')
    }

    const logout = () => {
        token.value = ''
        userInfo.value = {}
        clearStoredAuth()
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
