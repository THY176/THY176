import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
    // ========== State ==========
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
            console.error('瑙ｆ瀽 userInfo 澶辫触:', e)
        }
        return null
    }

    const userInfo = ref(parseUserInfo() || {})

    // ========== Getters ==========
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
        if (!info || !info.role) return '鏈煡鐢ㄦ埛'

        if (info.role === 'team') {
            return info.team_name || info.teamName || '鏈煡绀惧洟'
        }
        if (info.role === 'teacher' || info.role === 'admin') {
            // 纭繚杩斿洖鐨勬槸濮撳悕
            return info.name || info.userName || '鏈煡鐢ㄦ埛'
        }
        return '鏈煡鐢ㄦ埛'
    })

    // ========== Actions ==========
    const setUserInfo = (data, loginRole) => {
        console.log('setUserInfo 鍘熷鏁版嵁:', data)
        console.log('loginRole:', loginRole)

        let userData = {}

        if (loginRole === 'team') {
            userData = {
                role: 'team',
                team_ID: data.team_ID,
                team_name: data.team_name || '鏈煡绀惧洟',
                teacher_ID: data.teacher_ID,
                number: data.number,
                time: data.time,
                password: data.password
            }
        }  else if (loginRole === 'teacher') {
        console.log('鏁欏笀鏁版嵁 - teacher_ID:', data.teacher_ID);
        console.log('鏁欏笀鏁版嵁 - name:', data.name);
        userData = {
            role: 'teacher',
            teacher_ID: data.teacher_ID,
            name: data.name,
            team_ID: data.team_ID,
            tele: data.tele,
            gender: data.gender,
            age: data.age,
            password: data.password
        }
    }
         else if (loginRole === 'admin') {
            userData = {
                role: 'admin',
                teacher_ID: data.teacher_ID,
                name: data.name,
                tele: data.tele,
                gender: data.gender,
                age: data.age,
                password: data.password
            }
        }

        console.log('setUserInfo 澶勭悊鍚?', userData)

        userInfo.value = userData
        token.value = 'Bearer_' + Date.now()

        // 鍚屾淇濆瓨鍒?localStorage
        localStorage.setItem('token', token.value)
        localStorage.setItem('userInfo', JSON.stringify(userData))

        // 楠岃瘉淇濆瓨鏄惁鎴愬姛
        console.log('localStorage 淇濆瓨鍚?', localStorage.getItem('userInfo'))
    }

    const updateUserInfo = (updates) => {
        const current = userInfo.value
        const updated = { ...current, ...updates }
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
