<<template>
  <div class="error-container">
    <div class="error-content">
      <el-icon class="error-icon"><WarningFilled /></el-icon>
      <h1>403</h1>
      <p>无权访问该页面</p>
      <el-button type="primary" @click="goHome">返回首页</el-button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { WarningFilled } from '@element-plus/icons-vue'

const router = useRouter()

const goHome = () => {
  router.push('/')
}
</script>

<style scoped>
.error-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f5f7fa;
}
.error-content {
  text-align: center;
}
.error-icon {
  font-size: 80px;
  color: #e6a23c;
}
h1 {
  font-size: 72px;
  color: #303133;
  margin: 20px 0;
}
p {
  font-size: 18px;
  color: #909399;
  margin-bottom: 30px;
}
</style>