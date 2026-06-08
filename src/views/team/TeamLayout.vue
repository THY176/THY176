<template>
  <div>
    <CommonHeader />
    <div style="display: flex">
      <CommonAside />
      <div class="main-content">
        <router-view v-slot="{ Component }">
          <keep-alive :include="['TeamMember', 'ApplyHistory', 'DataStats']">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user.js'
import CommonHeader from '@/components/CommonHeader.vue'
import CommonAside from '@/components/CommonAside.vue'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const router = useRouter()

onMounted(() => {

  // 如果没有 userId 但 role 是 team，说明数据有问题
  if (!userStore.userId && userStore.role === 'team') {
    console.error('未获取到社团信息')
    ElMessage.error('未获取到社团信息，请重新登录')
    router.push('/')
  }
})
</script>

<style scoped>
.main-content {
  min-height: calc(100vh - 80px);
  width: calc(100vw - 200px);
  background-color: #f5f7fa;
  padding: 20px;
  overflow-y: auto;
}
</style>