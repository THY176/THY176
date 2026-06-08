<template>
  <div>
    <el-card style="max-width: 500px">
      <template #header>个人信息</template>
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
            <el-radio label="男">男</el-radio>
            <el-radio label="女">女</el-radio>
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
import { useUserStore } from '@/stores/user.js'
import request from '@/utils/request.js'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
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

onMounted(() => {
  // 直接从 userStore 获取当前登录用户的 ID 和角色
  const userId = userStore.userId
  const userRole = userStore.role


  if (!userId) {
    ElMessage.error('未获取到用户信息，请重新登录')
    return
  }

  // 根据角色选择不同的接口
  let apiUrl = ''
  if (userRole === 'teacher') {
    apiUrl = `/teacher/selectByteacher_ID/${userId}`
  } else if (userRole === 'admin') {
    apiUrl = `/admin/selectByteacher_ID/${userId}`
  } else {
    ElMessage.error('未知角色')
    return
  }


  request.get(apiUrl).then(res => {
    if (res.code === '200' && res.data) {
      formData.teacher_ID = res.data.teacher_ID || userId
      formData.name = res.data.name || ''
      formData.tele = res.data.tele || ''
      formData.gender = res.data.gender || '男'
      formData.age = res.data.age || 30
      formData.password = res.data.password || ''
    } else {
      ElMessage.error('获取用户信息失败')
    }
  }).catch(err => {
    console.error('加载用户信息失败:', err)
    ElMessage.error('加载用户信息失败')
  })
})

const handleUpdate = () => {
  const userId = userStore.userId
  const userRole = userStore.role

  if (!userId) {
    ElMessage.error('未获取到用户工号，请重新登录')
    return
  }

  const updateData = {
    teacher_ID: formData.teacher_ID || userId,
    name: formData.name,
    tele: formData.tele,
    gender: formData.gender,
    age: formData.age
  }

  if (changingPassword.value && newPassword.value) {
    updateData.password = newPassword.value
  }

  // 根据角色选择不同的接口
  let apiUrl = ''
  if (userRole === 'teacher') {
    apiUrl = '/teacher/update'
  } else if (userRole === 'admin') {
    apiUrl = '/admin/update'
  } else {
    ElMessage.error('未知角色')
    return
  }

  request.put(apiUrl, updateData).then(res => {
    if (res.code === '200') {
      ElMessage.success('保存成功')
      userStore.updateUserInfo({ name: formData.name })
      changingPassword.value = false
      newPassword.value = ''
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  }).catch(err => {
    console.error('保存失败:', err)
    ElMessage.error('保存失败')
  })
}
</script>
