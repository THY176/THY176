import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user.js'

const routes = [
  {
    path: '/',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true }
  },

  // ========== 社团身份模块（team） ==========
  {
    path: '/team',
    component: () => import('@/views/team/TeamLayout.vue'),
    meta: { role: 'team' },
    children: [
      { path: '', redirect: '/team/member' },
      { path: 'member', name: 'TeamMember', component: () => import('@/views/team/MemberManage.vue') },
      { path: 'apply', name: 'TeamApply', component: () => import('@/views/team/ApplyCreate.vue') },
      { path: 'history', name: 'TeamHistory', component: () => import('@/views/team/ApplyHistory.vue') },
      { path: 'edit/:apply_ID', name: 'TeamEdit', component: () => import('@/views/team/ApplyEdit.vue'), props: true },
      { path: 'stats', name: 'TeamStats', component: () => import('@/views/team/DataStats.vue') },
      { path: 'profile', name: 'TeamProfile', component: () => import('@/views/team/Profile.vue') }
    ]
  },

  // ========== 指导老师模块 ==========
  {
    path: '/teacher',
    component: () => import('@/views/teacher/TeacherLayout.vue'),
    meta: { role: 'teacher' },
    children: [
      { path: '', redirect: '/teacher/first-audit' },
      { path: 'first-audit', name: 'FirstAudit', component: () => import('@/views/teacher/FirstAudit.vue') },
      { path: 'history', name: 'TeacherHistory', component: () => import('@/views/teacher/AuditHistory.vue') },
      { path: 'profile', name: 'TeacherProfile', component: () => import('@/views/teacher/Profile.vue') }
    ]
  },

  // ========== 管理员模块 ==========
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { role: 'admin' },
    children: [
      { path: '', redirect: '/admin/team' },
      { path: 'team', name: 'AdminTeam', component: () => import('@/views/admin/TeamManage.vue') },  // 原ClubManage
      { path: 'member', name: 'AdminMember', component: () => import('@/views/admin/MemberManage.vue') },
      { path: 'teacher', name: 'AdminTeacher', component: () => import('@/views/admin/TeacherManage.vue') },
      { path: 'admin-manage', name: 'AdminManage', component: () => import('@/views/admin/AdminManage.vue') },
      { path: 'apply', name: 'AdminApply', component: () => import('@/views/admin/ApplyManage.vue') },
      { path: 'second-audit', name: 'SecondAudit', component: () => import('@/views/admin/SecondAudit.vue') },
      { path: 'third-audit', name: 'ThirdAudit', component: () => import('@/views/admin/ThirdAudit.vue') },
      { path: 'reimburse', name: 'AdminReimburse', component: () => import('@/views/admin/ReimburseManage.vue') },
      { path: 'stats', name: 'AdminStats', component: () => import('@/views/admin/DataStats.vue') },
      { path: 'workflow', name: 'AdminWorkFlow', component: () => import('@/views/admin/WorkFlow.vue') },
      { path: 'profile', name: 'AdminProfile', component: () => import('@/views/admin/Profile.vue') }
    ]
  },

  // ========== 异常页面 ==========
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { public: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { public: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.meta.public) {
    next()
    return
  }

  if (!userStore.token) {
    next('/')
    return
  }

  const userRole = userStore.role
  const requiredRole = to.meta.role

  if (requiredRole && userRole !== requiredRole) {
    next('/403')
    return
  }

  next()
})

export default router