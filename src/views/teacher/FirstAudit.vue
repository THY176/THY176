<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>待审核申请（一级审核）</span>
          <el-tag type="warning">您负责社团的经费申请首次审核</el-tag>
        </div>
      </template>

      <CommonTable :data="tableData" :columns="columns" :total="total" v-model:page-num="pageNum" v-model:page-size="pageSize" @page-change="load">
        <template #action="{ row }">
          <el-button type="primary" size="small" @click="openAuditDialog(row)">审核</el-button>
          <el-button type="info" size="small" @click="viewDetail(row)">详情</el-button>
        </template>
      </CommonTable>
    </el-card>

    <!-- 审核弹窗 -->
    <CommonDialog
        title="经费申请审核"
        v-model:visible="auditVisible"
        :form-data="auditForm"
        :rules="auditRules"
        @submit="handleAudit"
    >
      <template #form-items>
        <el-form-item label="申请社团">
          <el-input :model-value="currentApply?.team_name" disabled />
        </el-form-item>
        <el-form-item label="申请金额">
          <el-input :model-value="currentApply?.apply_money + ' 元'" disabled />
        </el-form-item>
        <el-form-item label="申请类型">
          <el-input :model-value="currentApply?.apply_type" disabled />
        </el-form-item>
        <el-form-item label="申请原因">
          <el-input :model-value="currentApply?.reason" type="textarea" disabled />
        </el-form-item>
        <el-form-item label="审核结果" prop="result">
          <el-radio-group v-model="auditForm.result">
            <el-radio label="审核通过">通过</el-radio>
            <el-radio label="审核驳回">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见" prop="opinion">
          <el-input v-model="auditForm.opinion" type="textarea" :rows="3" placeholder="请输入审核意见" />
        </el-form-item>
      </template>
    </CommonDialog>

    <!-- 详情弹窗 -->
    <el-dialog title="申请详情" v-model="detailVisible" width="600">
      <el-descriptions :column="2" border v-if="currentApply">
        <el-descriptions-item label="申请编号">{{ currentApply.apply_ID }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ currentApply.apply_time }}</el-descriptions-item>
        <el-descriptions-item label="申请金额">{{ currentApply.apply_money }} 元</el-descriptions-item>
        <el-descriptions-item label="申请类型">{{ currentApply.apply_type }}</el-descriptions-item>
        <el-descriptions-item label="收款账户">{{ currentApply.account_name }}</el-descriptions-item>
        <el-descriptions-item label="开户银行">{{ currentApply.bank }}</el-descriptions-item>
        <el-descriptions-item label="银行账号">{{ currentApply.account }}</el-descriptions-item>
        <el-descriptions-item label="事项分类">{{ currentApply.tags || '无' }}</el-descriptions-item>
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

const columns = [
  { prop: 'apply_ID', label: '申请编号', width: 100 },
  { prop: 'team_name', label: '社团名称', width: 150 },
  { prop: 'apply_money', label: '金额(元)', width: 120 },
  { prop: 'apply_type', label: '类型', width: 100 },
  { prop: 'apply_time', label: '申请时间', width: 160 },
  { prop: 'tags', label: '事项分类', width: 120 }
]

const auditVisible = ref(false)
const detailVisible = ref(false)
const currentApply = ref(null)
const currentTask = ref(null)

const auditForm = reactive({
  result: '审核通过',
  opinion: ''
})

const auditRules = {
  result: [{ required: true, message: '请选择审核结果', trigger: 'change' }],
  opinion: [{ required: true, message: '请输入审核意见', trigger: 'blur' }]
}

// 加载待审核任务（通过工作流接口）
const load = async () => {
  if (!userStore.userId) {
    console.log('用户未登录')
    return
  }

  try {
    const res = await request.get(`/workflow/teacher/tasks/${userStore.userId}`)

    if (res.code === '200') {
      let tasks = res.data || []

      // 过滤掉 apply 为 null 的任务
      tasks = tasks.filter(task => task.apply !== null && task.apply !== undefined)

      // 关键：只保留状态为"待审核"的申请
      tasks = tasks.filter(task => task.apply && task.apply.status === '待审核')

      // 去重
      const uniqueMap = new Map()
      tasks.forEach(task => {
        const applyId = task.applyId
        if (!uniqueMap.has(applyId) || new Date(task.createTime) > new Date(uniqueMap.get(applyId).createTime)) {
          uniqueMap.set(applyId, task)
        }
      })
      tasks = Array.from(uniqueMap.values())

      console.log('过滤后的任务列表:', tasks)

      tableData.value = tasks.map(task => {
        const applyInfo = task.apply || {}
        return {
          ...applyInfo,
          taskId: task.taskId,
          taskName: task.taskName,
          taskDefinitionKey: task.taskDefinitionKey,
          team_name: task.teamName || '未知社团'
        }
      })
      total.value = tableData.value.length
    }
  } catch (error) {
    console.error('加载待审核列表失败:', error)
  }
}

// 打开审核弹窗
const openAuditDialog = (row) => {
  currentApply.value = row
  currentTask.value = row
  auditForm.result = '审核通过'
  auditForm.opinion = ''
  auditVisible.value = true
}

// 查看详情
const viewDetail = (row) => {
  currentApply.value = row
  detailVisible.value = true
}

// 提交审核
const handleAudit = async () => {
  if (!currentApply.value) return

  const userInfo = userStore.userInfo
  const teacherName = userInfo.name || userInfo.teacher_name || '审核员'

  try {
    const res = await request.post('/workflow/firstAudit', null, {
      params: {
        taskId: currentTask.value.taskId,
        applyId: currentApply.value.apply_ID,
        teacherId: userStore.userId,
        teacherName: teacherName,
        opinion: auditForm.opinion,
        approved: auditForm.result === '审核通过'
      }
    })

    if (res.code === '200') {
      ElMessage.success('审核完成：' + auditForm.result)
      auditVisible.value = false
      load()
    } else {
      ElMessage.error(res.msg || '审核失败')
    }
  } catch (error) {
    console.error('审核失败:', error)
    ElMessage.error('审核失败')
  }
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>