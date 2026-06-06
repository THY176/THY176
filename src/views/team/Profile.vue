<template>
  <div>
    <el-card style="max-width: 600px">
      <template #header>
        <div class="card-header">
          <span>社团信息维护</span>
          <el-button type="primary" link @click="loadInfo">刷新信息</el-button>
        </div>
      </template>
      <el-form :model="formData" label-width="120px">
        <el-form-item label="社团编号">
          <el-input v-model="formData.team_ID" disabled />
        </el-form-item>
        <el-form-item label="社团名称">
          <el-input v-model="formData.team_name" />
        </el-form-item>
        <el-form-item label="当前人数">
          <el-input v-model="formData.number" disabled />
        </el-form-item>
        <el-form-item label="成立时间">
          <el-input v-model="formData.time" disabled />
        </el-form-item>
        <el-form-item label="新密码" v-if="changingPassword">
          <el-input v-model="newPassword" type="password" placeholder="不修改请留空" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleUpdate">保存修改</el-button>
          <el-button @click="changingPassword = !changingPassword">
            {{ changingPassword ? '取消改密' : '修改密码' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/user.js'
import request from '@/utils/request.js'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const changingPassword = ref(false)
const newPassword = ref('')

const formData = reactive({
  team_ID: '',
  team_name: '',
  number: 0,
  time: '',
  password: ''
})

// 从数据库加载社团信息
const loadInfo = () => {
  const teamId = userStore.userId
  if (!teamId) {
    ElMessage.error('未获取到社团信息')
    return
  }

  console.log('从数据库加载社团信息，teamId:', teamId)

  request.get(`/team/selectByteam_ID/${teamId}`).then(res => {
    console.log('社团信息返回:', res)
    if (res.code === '200' && res.data) {
      Object.assign(formData, res.data)
      console.log('更新后的人数:', formData.number)
      // 同步更新 store 中的社团名称
      if (formData.team_name) {
        userStore.updateUserInfo({ team_name: formData.team_name })
      }
    }
  }).catch(err => {
    console.error('加载社团信息失败:', err)
    ElMessage.error('加载社团信息失败')
  })
}

const handleUpdate = () => {
  const updateData = { ...formData }
  if (changingPassword.value && newPassword.value) {
    updateData.password = newPassword.value
  }

  request.put('/team/update', updateData).then(res => {
    if (res.code === '200') {
      ElMessage.success('修改成功')
      loadInfo()
      changingPassword.value = false
      newPassword.value = ''
    }
  })
}

// 监听成员删除事件
const handleMemberDeleted = () => {
  console.log('收到成员删除事件，刷新社团信息')
  loadInfo()
}

onMounted(() => {
  loadInfo()
  // 监听自定义事件
  window.addEventListener('member-deleted', handleMemberDeleted)
})

onUnmounted(() => {
  // 移除事件监听
  window.removeEventListener('member-deleted', handleMemberDeleted)
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>