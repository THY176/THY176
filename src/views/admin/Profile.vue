<template>
  <div>
    <el-card style="max-width: 500px">
      <template #header>管理员信息</template>
      <el-form :model="formData" label-width="100px">
        <el-form-item label="工号">
          <el-input v-model="formData.teacher_ID" disabled />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="formData.name" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="formData.tele" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="formData.gender">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="年龄">
          <el-input-number v-model="formData.age" :min="20" :max="80" />
        </el-form-item>
        <el-form-item label="新密码" v-if="changingPassword">
          <el-input v-model="newPassword" type="password" placeholder="不修改请留空" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleUpdate">保存</el-button>
          <el-button @click="changingPassword = !changingPassword">
            {{ changingPassword ? '取消' : '修改密码' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request.js'
import { ElMessage } from 'element-plus'

const router = useRouter()
const changingPassword = ref(false)
const newPassword = ref('')

const formData = reactive({
  teacher_ID: '',
  name: '',
  tele: '',
  gender: '男',
  age: 30,
  password: ''
})

const loadAdminInfo = () => {
  // 直接从 localStorage 获取用户信息
  const userInfoStr = localStorage.getItem('userInfo')
  console.log('localStorage userInfo:', userInfoStr)

  if (!userInfoStr) {
    ElMessage.error('未获取到管理员信息，请重新登录')
    setTimeout(() => {
      router.push('/')
    }, 1500)
    return
  }

  try {
    const userInfo = JSON.parse(userInfoStr)
    const teacherId = userInfo.teacher_ID
    console.log('从localStorage获取的teacher_ID:', teacherId)

    if (!teacherId) {
      ElMessage.error('未获取到管理员工号，请重新登录')
      setTimeout(() => {
        router.push('/')
      }, 1500)
      return
    }

    // 加载管理员信息
    request.get(`/admin/selectByteacher_ID/${teacherId}`).then(res => {
      console.log('管理员信息返回:', res)
      if (res.code === '200' && res.data) {
        Object.assign(formData, res.data)
        console.log('加载后的formData:', formData)
      } else {
        ElMessage.error('加载管理员信息失败')
      }
    }).catch(err => {
      console.error('加载管理员信息失败:', err)
      ElMessage.error('加载管理员信息失败')
    })

  } catch (e) {
    console.error('解析用户信息失败:', e)
    ElMessage.error('解析用户信息失败')
    router.push('/')
  }
}

const handleUpdate = () => {
  const updateData = { ...formData }
  if (changingPassword.value && newPassword.value) {
    updateData.password = newPassword.value
  }

  request.put('/admin/update', updateData).then(res => {
    if (res.code === '200') {
      ElMessage.success('保存成功')
      // 更新 localStorage 中的姓名
      const userInfoStr = localStorage.getItem('userInfo')
      if (userInfoStr) {
        const userInfo = JSON.parse(userInfoStr)
        userInfo.name = formData.name
        localStorage.setItem('userInfo', JSON.stringify(userInfo))
      }
      changingPassword.value = false
      newPassword.value = ''
      // 重新加载
      loadAdminInfo()
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  }).catch(err => {
    console.error('保存失败:', err)
    ElMessage.error('保存失败')
  })
}

onMounted(() => {
  loadAdminInfo()
})
</script>