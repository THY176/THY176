import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
    const token = ref(localStorage.getItem('token') || '')

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
            const stored = localStorage.getItem('userInfo')
            if (stored && stored !== 'undefined' && stored !== 'null' && stored !== '{}') {
                const parsed = JSON.parse(stored)
                if (parsed && parsed.role) {
                    const normalized = normalizeUserInfo(parsed)
                    localStorage.setItem('userInfo', JSON.stringify(normalized))
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

        localStorage.setItem('token', token.value)
        localStorage.setItem('userInfo', JSON.stringify(userData))
    }

    const updateUserInfo = (updates) => {
        const updated = normalizeUserInfo({ ...userInfo.value, ...updates })

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
