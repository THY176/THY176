<template>
  <div class="header-container">
    <div class="header-content">
      <img src="@/assets/logo.png" class="logo" alt="logo">
      <div class="system-title">高校社团经费审批与报销系统</div>
      <div class="user-info">
        <el-dropdown @command="handleCommand">
          <span class="user-name">
            <el-icon><User /></el-icon>
            {{ displayName }}
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人信息</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user.js'
import { useRouter } from 'vue-router'
import { User, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const router = useRouter()

// 计算显示名称
const displayName = computed(() => {
  const info = userStore.userInfo

  if (info.role === 'team') {
    return info.team_name || info.teamName || '未知社团'
  }
  return userStore.userName || '未知用户'
})

const handleCommand = (command) => {
  if (command === 'profile') {
    const routeMap = {
      'team': '/team/profile',
      'teacher': '/teacher/profile',
      'admin': '/admin/profile'
    }
    router.push(routeMap[userStore.role])
  } else if (command === 'logout') {
    userStore.logout()
    ElMessage.success('退出成功')
    router.push('/')
  }
}
</script>

<style scoped>
.header-container {
  height: 80px;
  background-color: #2d8cf0;
  display: flex;
  align-items: center;
  padding: 0 30px;
}
.header-content {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.logo {
  height: 50px;
  width: 50px;
}
.system-title {
  font-size: 28px;
  color: #ffffff;
  font-weight: bold;
  flex: 1;
  margin-left: 20px;
}
.user-info {
  color: #ffffff;
}
.user-name {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
}
</style>