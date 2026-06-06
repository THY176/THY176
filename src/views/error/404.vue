<<template>
  <div class="error-container">
    <div class="error-content">
      <el-icon class="error-icon"><CircleCloseFilled /></el-icon>
      <h1>404</h1>
      <p>页面不存在</p>
      <el-button type="primary" @click="goHome">返回首页</el-button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { CircleCloseFilled } from '@element-plus/icons-vue'

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
  color: #f56c6c;
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
