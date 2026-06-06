<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-form-wrapper">
        <el-form ref="formRef" :rules="rules" :model="formData" style="width: 400px">
          <div class="login-title">欢迎登录!</div>
          <div class="login-subtitle">高校社团经费审批与报销系统</div>

          <el-form-item prop="account">
            <el-input
                size="large"
                v-model="formData.account"
                placeholder="请输入账号(ID)"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
                show-password
                size="large"
                v-model="formData.password"
                placeholder="请输入密码"
            />
          </el-form-item>

          <el-form-item prop="role" style="margin-bottom: 25px">
            <el-radio-group v-model="formData.role" size="large" style="width: 100%">
              <el-radio label="admin" style="margin-right: 30px; font-weight: bold">管理员</el-radio>
              <el-radio label="teacher" style="margin-right: 30px; font-weight: bold">指导老师</el-radio>
              <el-radio label="team" style="font-weight: bold">社团</el-radio>
            </el-radio-group>
          </el-form-item>

          <div style="margin-bottom: 20px">
            <el-button
                @click="handleLogin"
                size="large"
                style="width: 100%"
                type="primary"
                :loading="loading"
            >
              登录
            </el-button>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, nextTick } from "vue"
import request from "@/utils/request.js"
import { useUserStore } from '@/stores/user.js'
import { ElMessage } from "element-plus"

const userStore = useUserStore()

const formData = reactive({
  account: '',
  password: '',
  role: 'team'
})

const rules = {
  account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const formRef = ref()
const loading = ref(false)

const handleLogin = () => {
  formRef.value.validate((valid) => {
    if (!valid) return

    loading.value = true

    const { account, password, role } = formData

    let apiUrl = ''
    if (role === 'team') {
      apiUrl = '/team/selectByteam_ID/' + account
    } else if (role === 'teacher') {
      apiUrl = '/teacher/selectByteacher_ID/' + account
    } else if (role === 'admin') {
      apiUrl = '/admin/selectByteacher_ID/' + account
    }

    request.get(apiUrl).then(res => {
      if (res.code !== '200' || !res.data) {
        ElMessage.error('账号不存在')
        return
      }

      const userData = res.data
      console.log('后端返回的用户数据:', userData)

      if (userData.password !== password) {
        ElMessage.error('密码错误')
        return
      }

      // 修复：确保 teacher_ID 有值
      if (role === 'teacher' && !userData.teacher_ID) {
        userData.teacher_ID = parseInt(account);
        console.log('手动设置 teacher_ID:', userData.teacher_ID);
      }

      // 修复：确保 admin 的 teacher_ID 有值
      if (role === 'admin' && !userData.teacher_ID) {
        userData.teacher_ID = parseInt(account);
        console.log('手动设置 admin teacher_ID:', userData.teacher_ID);
      }

      // 修复：确保 team 的 team_ID 有值
      if (role === 'team' && !userData.team_ID) {
        userData.team_ID = parseInt(account);
        console.log('手动设置 team_ID:', userData.team_ID);
      }

      // 保存用户信息
      userStore.setUserInfo(userData, role)

      // 保存用户信息
      userStore.setUserInfo(userData, role)

      // 等待一下确保数据保存完成
      setTimeout(() => {
        console.log('保存后的 userId:', userStore.userId)
        console.log('保存后的 userInfo:', userStore.userInfo)

        ElMessage.success('登录成功')

        // 跳转
        if (role === 'admin') {
          window.location.href = '/admin'
        } else if (role === 'teacher') {
          window.location.href = '/teacher'
        } else if (role === 'team') {
          window.location.href = '/team'
        }
      }, 100)

    }).catch(err => {
      console.error('登录请求错误:', err)
      ElMessage.error('登录请求失败')
    }).finally(() => {
      loading.value = false
    })
  })
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background-size: cover;
  background-position: center;
}
.login-box {
  position: absolute;
  width: 50%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  right: 0;
}
.login-form-wrapper {
  padding: 20px;
  background-color: #f4f4f4;
  border-radius: 5px;
}
.login-title {
  margin-bottom: 15px;
  font-size: 24px;
  text-align: center;
  color: #0742b1;
  font-weight: bold;
}
.login-subtitle {
  margin-bottom: 30px;
  font-size: 20px;
  text-align: center;
  color: #0742b1;
  font-weight: bold;
}
</style>
