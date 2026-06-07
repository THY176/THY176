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
import { useUserStore } from '@/stores/user.js'
import CommonHeader from '@/components/CommonHeader.vue'
import CommonAside from '@/components/CommonAside.vue'

const userStore = useUserStore()

onMounted(() => {
  console.log('AdminLayout - userStore.userInfo:', userStore.userInfo)
  console.log('AdminLayout - userStore.userId:', userStore.userId)
  console.log('AdminLayout - role:', userStore.role)
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
