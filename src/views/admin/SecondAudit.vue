<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>二级审核（第一次管理员审核）</span>
          <el-tag type="warning">待二次审核的申请</el-tag>
        </div>
      </template>

      <CommonTable :data="tableData" :columns="columns" :total="total" v-model:page-num="pageNum" v-model:page-size="pageSize" @page-change="load">
        <template #action="{ row }">
          <el-button type="primary" size="small" @click="openAuditDialog(row)">审核</el-button>
          <el-button type="info" size="small" @click="viewDetail(row)">详情</el-button>
        </template>
      </CommonTable>
    </el-card>

    <CommonDialog title="二级审核" v-model:visible="auditVisible" :form-data="auditForm" :rules="auditRules" @submit="handleAudit">
      <template #form-items>
        <el-form-item label="申请社团">
          <el-input :model-value="currentApply?.team_name" disabled />
        </el-form-item>
        <el-form-item label="申请金额">
          <el-input :model-value="currentApply?.apply_money + ' 元'" disabled />
        </el-form-item>
        <el-form-item label="指导老师审核">
          <el-tag :type="firstAudit?.status === '审核通过' ? 'success' : 'danger'">
            {{ firstAudit?.status || '未知' }}
          </el-tag>
        </el-form-item>
        <el-form-item label="指导老师意见">
          <el-input :model-value="firstAudit?.opinion" type="textarea" disabled />
        </el-form-item>
        <el-form-item label="审核结果" prop="result">
          <el-radio-group v-model="auditForm.result">
            <el-radio label="审核通过">通过，进入三级审核</el-radio>
            <el-radio label="审核驳回">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见" prop="opinion">
          <el-input v-model="auditForm.opinion" type="textarea" :rows="3" placeholder="请输入审核意见" />
        </el-form-item>
      </template>
    </CommonDialog>

    <el-dialog title="申请详情" v-model="detailVisible" width="600">
      <el-descriptions :column="2" border v-if="currentApply">
        <el-descriptions-item label="申请编号">{{ currentApply.apply_ID }}</el-descriptions-item>
        <el-descriptions-item label="申请金额">{{ currentApply.apply_money }} 元</el-descriptions-item>
        <el-descriptions-item label="申请类型">{{ currentApply.apply_type }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ currentApply.apply_time }}</el-descriptions-item>
        <el-descriptions-item label="申请原因" :span="2">{{ currentApply.reason }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user.js'
import request from '@/utils/request.js'
import { ElMessage } from 'element-plus'
import CommonTable from '@/components/CommonTable.vue'
import CommonDialog from '@/components/CommonDialog.vue'

const userStore = useUserStore()

const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const clubList = ref([])

const columns = [
  { prop: 'apply_ID', label: '申请编号', width: 100 },
  { prop: 'team_name', label: '社团', width: 150 },
  { prop: 'apply_money', label: '金额(元)', width: 120 },
  { prop: 'apply_type', label: '类型', width: 100 },
  { prop: 'apply_time', label: '申请时间', width: 160 },
  { prop: 'tags', label: '事项分类', width: 120 }
]

const auditVisible = ref(false)
const detailVisible = ref(false)
const currentApply = ref(null)
const firstAudit = ref(null)

const auditForm = reactive({
  result: '审核通过',
  opinion: ''
})

const auditRules = {
  result: [{ required: true, message: '请选择结果', trigger: 'change' }],
  opinion: [{ required: true, message: '请输入意见', trigger: 'blur' }]
}

// 加载社团列表
const loadClubs = async () => {
  const res = await request.get('/team/selectAll')
  if (res.code === '200') {
    clubList.value = res.data || []
  }
}

// 加载待审核任务（直接查询数据库）
const load = async () => {
  if (!userStore.userId) {
    return
  }

  try {
    // 直接查询待二次审核的申请
    const res = await request.get('/apply/selectByStatus/待二次审核')
    console.log('待二次审核申请:', res.data)

    if (res.code === '200') {
      let list = res.data || []

      // 补充社团名称
      for (let item of list) {
        const club = clubList.value.find(c => c.team_ID === item.team_ID)
        item.team_name = club?.team_name || '未知社团'
      }

      tableData.value = list
      total.value = list.length
      console.log('表格数据:', tableData.value)
    }
  } catch (error) {
    console.error('加载待审核列表失败:', error)
  }
}

const openAuditDialog = async (row) => {
  currentApply.value = row
  auditForm.result = '审核通过'
  auditForm.opinion = ''

  // 获取一级审核记录（只取最新的一条）
  try {
    const res = await request.get(`/approve/selectByApply_IDAndSequence`, {
      params: { apply_ID: row.apply_ID, sequence: 1 }
    })
    if (res.code === '200' && res.data?.length > 0) {
      // 取最新的一条（approve_time 最大的）
      let audits = res.data
      audits.sort((a, b) => new Date(b.approve_time) - new Date(a.approve_time))
      firstAudit.value = audits[0]
      console.log('最新的一级审核记录:', firstAudit.value)
    } else {
      firstAudit.value = null
    }
  } catch (error) {
    console.error('获取审核记录失败:', error)
  }

  auditVisible.value = true
}

const viewDetail = (row) => {
  currentApply.value = row
  detailVisible.value = true
}

const handleAudit = async () => {
  if (!currentApply.value) return

  const userInfo = userStore.userInfo
  const adminName = userInfo.name || '管理员'
  const approved = auditForm.result === '审核通过'

  try {
    // 直接更新申请状态（不通过工作流）
    const newStatus = approved ? '待三次审核' : '审核驳回'

    // 1. 添加审核记录
    const approveRes = await request.post('/approve/add', {
      apply_ID: currentApply.value.apply_ID,
      teacher_ID: userStore.userId,
      teacher_name: adminName,
      role: '管理员',
      sequence: 2,
      opinion: auditForm.opinion,
      status: auditForm.result,
      approve_time: new Date().toISOString().slice(0, 19).replace('T', ' ')
    })

    if (approveRes.code !== '200') {
      ElMessage.error('审核记录保存失败')
      return
    }

    // 2. 更新申请状态
    await request.put('/apply/update', {
      apply_ID: currentApply.value.apply_ID,
      status: newStatus
    })

    ElMessage.success('审核完成：' + auditForm.result)
    auditVisible.value = false
    load()
  } catch (error) {
    console.error('审核失败:', error)
    ElMessage.error('审核失败')
  }
}

onMounted(() => {
  loadClubs().then(() => load())
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>