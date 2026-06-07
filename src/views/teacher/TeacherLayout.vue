<template>
  <div>
    <CommonHeader />
    <div style="display: flex">
      <CommonAside />
      <div class="main-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user.js'
import CommonHeader from '@/components/CommonHeader.vue'
import CommonAside from '@/components/CommonAside.vue'

const userStore = useUserStore()

onMounted(() => {
  console.log('TeacherLayout - userInfo:', userStore.userInfo)
  console.log('TeacherLayout - userId:', userStore.userId)
  console.log('TeacherLayout - role:', userStore.role)
  console.log('TeacherLayout - token:', userStore.token)
})
</script>

<style scoped>
.main-content {
  min-height: calc(100vh - 80px);
  width: calc(100vw - 200px);
  background-color: #f5f7fa;
  padding: 20px;
}
</style>
