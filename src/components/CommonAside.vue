<<template>
  <div class="aside-container">
    <el-menu
        :default-active="activePath"
        :collapse="isCollapse"
        :collapse-transition="false"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
    >
      <div class="menu-title">功能菜单</div>
      <el-menu-item v-for="item in menuList" :key="item.path" :index="item.path">
        <el-icon>
          <component :is="item.icon" />
        </el-icon>
        <template #title>{{ item.title }}</template>
      </el-menu-item>

      <el-menu-item index="/" @click="handleLogout">
        <el-icon><SwitchButton /></el-icon>
        <template #title>退出登录</template>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user.js'
import {
  User, Money, Clock, TrendCharts, OfficeBuilding,
  UserFilled, DocumentChecked, Stamp, Wallet, DataLine,
  SetUp, CircleCheck, Document, SwitchButton
} from '@element-plus/icons-vue'

const route = useRoute()
const userStore = useUserStore()

const activePath = computed(() => route.path)

// 关键修复：添加 isCollapse
const isCollapse = ref(false)

const menuConfig = {
  'team': [
    { path: '/team/member', title: '成员管理', icon: 'User' },
    { path: '/team/apply', title: '经费申请', icon: 'Money' },
    { path: '/team/history', title: '申请历史', icon: 'Clock' },
    { path: '/team/stats', title: '数据统计', icon: 'TrendCharts' }
  ],
  'teacher': [
    { path: '/teacher/first-audit', title: '一级审核', icon: 'CircleCheck' },
    { path: '/teacher/history', title: '审核历史', icon: 'Document' }
  ],
  'admin': [
    { path: '/admin/team', title: '社团管理', icon: 'OfficeBuilding' },
    { path: '/admin/member', title: '成员管理', icon: 'UserFilled' },
    { path: '/admin/teacher', title: '教师管理', icon: 'User' },
    { path: '/admin/admin-manage', title: '管理员管理', icon: 'UserFilled' },
    { path: '/admin/apply', title: '申请管理', icon: 'DocumentChecked' },
    { path: '/admin/second-audit', title: '二级审核', icon: 'Stamp' },
    { path: '/admin/third-audit', title: '三级审核', icon: 'Stamp' },
    { path: '/admin/reimburse', title: '报销管理', icon: 'Wallet' },
    { path: '/admin/stats', title: '数据统计', icon: 'DataLine' },
    { path: '/admin/workflow', title: '工作流配置', icon: 'SetUp' }
  ]
}

const menuList = computed(() => menuConfig[userStore.role] || [])

const handleLogout = () => {
  userStore.logout()
  window.location.href = '/'
}
</script>

<style scoped>
.aside-container {
  min-height: calc(100vh - 80px);
  background-color: #304156;
}
.menu-title {
  height: 50px;
  line-height: 50px;
  text-align: center;
  color: #ffffff;
  font-size: 16px;
  font-weight: bold;
  border-bottom: 1px solid #1f2d3d;
}
.el-menu {
  border-right: none;
}
</style>