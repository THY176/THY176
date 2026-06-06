<<template>
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
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user.js'
import { ElMessage } from 'element-plus'
import CommonHeader from '@/components/CommonHeader.vue'
import CommonAside from '@/components/CommonAside.vue'

const userStore = useUserStore()
const router = useRouter()

onMounted(() => {
  console.log('AdminLayout - userStore.userInfo:', userStore.userInfo)
  console.log('AdminLayout - userStore.userId:', userStore.userId)
  console.log('AdminLayout - localStorage:', localStorage.getItem('userInfo'))

  // 检查登录状态
  const token = localStorage.getItem('token')
  const userInfo = localStorage.getItem('userInfo')

  if (!token || !userInfo) {
    ElMessage.error('请先登录')
    router.push('/')
    return
  }

  try {
    const parsed = JSON.parse(userInfo)
    if (parsed.role !== 'admin') {
      ElMessage.error('无权访问管理员页面')
      router.push('/')
    }
  } catch (e) {
    console.error('解析用户信息失败:', e)
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
}
</style>