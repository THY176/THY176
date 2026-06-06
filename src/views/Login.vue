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
import { reactive, ref } from 'vue'
import request from '@/utils/request.js'
import { useUserStore } from '@/stores/user.js'
import { ElMessage } from 'element-plus'

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

const routeMap = {
  admin: '/admin',
  teacher: '/teacher',
  team: '/team'
}

const handleLogin = () => {
  formRef.value.validate((valid) => {
    if (!valid) return

    loading.value = true

    request.post('/auth/login', {
      account: Number(formData.account),
      password: formData.password,
      role: formData.role
    }).then(res => {
      if (res.code !== '200' || !res.data?.token || !res.data?.userInfo) {
        ElMessage.error(res.msg || '登录失败')
        return
      }

      userStore.setUserInfo(res.data.userInfo, res.data.token, Number(formData.account))
      ElMessage.success('登录成功')
      window.location.href = routeMap[formData.role]
    }).catch(err => {
      console.error('登录请求错误:', err)
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
