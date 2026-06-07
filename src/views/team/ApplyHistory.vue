<template>
  <div>
    <el-card>
      <div style="margin-bottom: 15px">
        <el-select
            v-model="searchForm.status"
            placeholder="状态筛选"
            clearable
            style="width: 150px; margin-right: 10px"
            @clear="handleSearch"
            @change="handleSearch"
        >
          <el-option label="待提交" value="待提交" />
          <el-option label="待审核" value="待审核" />
          <el-option label="审核驳回" value="审核驳回" />
          <el-option label="待二次审核" value="待二次审核" />
          <el-option label="待三次审核" value="待三次审核" />
          <el-option label="审核通过" value="审核通过" />
          <el-option label="已报销" value="已报销" />
        </el-select>

        <el-select
            v-model="searchForm.apply_type"
            placeholder="申请类型"
            clearable
            style="width: 150px; margin-right: 10px"
            @clear="handleSearch"
            @change="handleSearch"
        >
          <el-option label="活动费" value="活动费" />
          <el-option label="比赛费" value="比赛费" />
          <el-option label="采购费" value="采购费" />
          <el-option label="设备费" value="设备费" />
          <el-option label="培训费" value="培训费" />
          <el-option label="其他" value="其他" />
        </el-select>
      </div>

      <CommonTable :data="tableData" :columns="columns" :total="total" v-model:page-num="pageNum" v-model:page-size="pageSize" :loading="loading" @page-change="load">
        <template #status="{ row }">
          <el-tag :type="statusTypeMap[row.status] || 'info'">
            {{ row.status }}
          </el-tag>
        </template>
        <template #tags="{ row }">
          <el-tag v-for="tag in (row.tags || '').split(',').filter(Boolean)" :key="tag" size="small" style="margin-right: 5px">
            {{ tag }}
          </el-tag>
        </template>
        <template #action="{ row }">
          <el-button type="primary" size="small" @click="viewDetail(row)">详情</el-button>
          <el-button
              v-if="row.status === '待提交' || row.status === '审核驳回'"
              type="warning"
              size="small"
              @click="editApply(row)"
          >
            编辑
          </el-button>
          <el-button
              v-if="row.status === '待提交'"
              type="success"
              size="small"
              @click="submitApply(row)"
          >
            提交
          </el-button>
          <el-button
              v-if="row.status === '待提交'"
              type="danger"
              size="small"
              @click="deleteApply(row)"
          >
            删除
          </el-button>
        </template>
      </CommonTable>
    </el-card>

    <el-dialog title="申请详情" v-model="detailVisible" width="700">
      <div v-if="currentApply">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请编号">{{ currentApply.apply_ID }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ currentApply.apply_time }}</el-descriptions-item>
          <el-descriptions-item label="申请金额">{{ currentApply.apply_money }} 元</el-descriptions-item>
          <el-descriptions-item label="申请类型">{{ currentApply.apply_type }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="statusTypeMap[currentApply.status]">
              {{ currentApply.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="收款账户">{{ currentApply.account_name }}</el-descriptions-item>
          <el-descriptions-item label="开户银行">{{ currentApply.bank }}</el-descriptions-item>
          <el-descriptions-item label="银行账号">{{ currentApply.account }}</el-descriptions-item>
          <el-descriptions-item label="事项分类">{{ currentApply.tags || '无' }}</el-descriptions-item>
          <el-descriptions-item label="申请原因" :span="2">{{ currentApply.reason }}</el-descriptions-item>
        </el-descriptions>

        <div style="margin-top: 20px">
          <h4>审核流程</h4>
          <el-timeline>
            <el-timeline-item
                v-for="(item, index) in auditList"
                :key="index"
                :type="item.status === '审核通过' ? 'success' : item.status === '审核驳回' ? 'danger' : 'primary'"
            >
              <p><strong>{{ item.role }}审核</strong> - {{ item.approve_time || '待审核' }}</p>
              <p>审核人：{{ item.teacher_name || item.teacher_ID || '待定' }}</p>
              <p>意见：{{ item.opinion || '暂无' }}</p>
              <p>结果：{{ item.status }}</p>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onActivated, onMounted } from 'vue'
import { useUserStore } from '@/stores/user.js'
import request from '@/utils/request.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import CommonTable from '@/components/CommonTable.vue'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

const searchForm = reactive({ status: '', apply_type: '' })
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const loading = ref(false)

const statusTypeMap = {
  '待提交': 'info',
  '待审核': 'warning',
  '审核驳回': 'danger',
  '待二次审核': 'warning',
  '待三次审核': 'warning',
  '审核通过': 'success',
  '已报销': 'success'
}

const columns = [
  { prop: 'apply_ID', label: '申请编号', width: 100 },
  { prop: 'apply_time', label: '申请时间', width: 160 },
  { prop: 'apply_money', label: '金额(元)', width: 120 },
  { prop: 'apply_type', label: '类型', width: 100 },
  { prop: 'status', label: '状态', width: 120, slot: true },
  { prop: 'tags', label: '事项分类', width: 120, slot: true }
]

const detailVisible = ref(false)
const currentApply = ref(null)
const auditList = ref([])

const load = async () => {
  const params = {
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    team_ID: userStore.userId,
    _t: Date.now()
  }

  if (searchForm.status && searchForm.status !== '') {
    params.status = searchForm.status
  }
  if (searchForm.apply_type && searchForm.apply_type !== '') {
    params.apply_type = searchForm.apply_type
  }

  loading.value = true
  try {
    const res = await request.get('/apply/selectPage', { params })
    if (res.code === '200') {
      tableData.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  load()
}

const auditRoleMap = {
  1: '指导老师',
  2: '二级管理员',
  3: '三级管理员',
  99: '管理员强制驳回'
}

const auditTime = (audit) => audit?.approve_time || ''

const compareAudit = (a, b) => {
  const timeCompare = auditTime(a).localeCompare(auditTime(b))
  if (timeCompare !== 0) {
    return timeCompare
  }
  return (a?.approve_ID || 0) - (b?.approve_ID || 0)
}

const latestAudit = (audits, filterFn = () => true) => {
  return audits
      .filter(audit => audit && auditTime(audit) && filterFn(audit))
      .sort((a, b) => compareAudit(b, a))[0] || null
}

const currentAttemptAudits = (audits, terminalAudit = null) => {
  const previousReject = latestAudit(audits, audit =>
      audit.status === '审核驳回' &&
      (!terminalAudit || compareAudit(audit, terminalAudit) < 0)
  )

  return audits.filter(audit =>
      audit &&
      auditTime(audit) &&
      (!previousReject || compareAudit(audit, previousReject) > 0) &&
      (!terminalAudit || compareAudit(audit, terminalAudit) <= 0)
  )
}

const latestPassed = (audits, sequence) => {
  return latestAudit(audits, audit =>
      audit.sequence === sequence &&
      audit.status === '审核通过'
  )
}

const pendingAudit = (sequence, currentTask = null) => ({
  sequence,
  role: auditRoleMap[sequence],
  teacher_ID: currentTask?.assigneeId || '',
  teacher_name: currentTask?.assigneeName || '待定',
  opinion: '等待处理',
  approve_time: '',
  status: '待审核'
})

const buildAuditTimeline = (status, audits, currentTask = null) => {
  if (status === '待提交') {
    return []
  }

  const latestReject = latestAudit(audits, audit => audit.status === '审核驳回')
  if (status === '审核驳回' && latestReject) {
    const currentAudits = currentAttemptAudits(audits, latestReject)
    const reject = { ...latestReject }
    if (reject.sequence === 99) {
      const maxPassedSequence = Math.max(
          0,
          ...currentAudits
              .filter(audit => audit.status === '审核通过' && audit.sequence >= 1 && audit.sequence <= 3)
              .map(audit => audit.sequence)
      )
      reject.sequence = Math.min(maxPassedSequence + 1, 3)
    }

    const result = []
    for (let sequence = 1; sequence < reject.sequence; sequence++) {
      const passed = latestPassed(currentAudits, sequence)
      if (passed) {
        result.push(passed)
      }
    }
    result.push(reject)
    return result
  }

  const currentAudits = currentAttemptAudits(audits)

  if (status === '待审核') {
    return [pendingAudit(1, currentTask)]
  }

  if (status === '待二次审核') {
    return [latestPassed(currentAudits, 1), pendingAudit(2, currentTask)].filter(Boolean)
  }

  if (status === '待三次审核') {
    return [
      latestPassed(currentAudits, 1),
      latestPassed(currentAudits, 2),
      pendingAudit(3, currentTask)
    ].filter(Boolean)
  }

  if (status === '审核通过' || status === '已报销') {
    return [1, 2, 3]
        .map(sequence => latestPassed(currentAudits, sequence))
        .filter(Boolean)
  }

  return []
}

const viewDetail = async (row) => {
  currentApply.value = row
  detailVisible.value = true

  const [approveRes, taskRes] = await Promise.all([
    request.get(`/approve/selectByApply_ID/${row.apply_ID}`),
    request.get(`/workflow/currentTask/${row.apply_ID}`).catch(() => ({ data: null }))
  ])
  if (approveRes.code === '200') {
    auditList.value = buildAuditTimeline(row.status, approveRes.data || [], taskRes.data)
  }
}

const editApply = (row) => {
  router.push(`/team/edit/${row.apply_ID}`)
}

// 修改这里：调用工作流启动接口
const submitApply = (row) => {
  ElMessageBox.confirm('确定提交该申请吗？提交后将进入审核流程', '确认提交', { type: 'info' })
      .then(async () => {
        const res = await request.post('/apply/startWorkflow', null, {
          params: { applyId: row.apply_ID }
        })
        if (res.code === '200') {
          ElMessage.success('提交成功，等待审核')
          row.status = '待审核'
          if (searchForm.status === '待提交') {
            tableData.value = tableData.value.filter(item => item.apply_ID !== row.apply_ID)
            total.value = Math.max(0, total.value - 1)
          }
          await load()
        } else {
          ElMessage.error(res.msg || '提交失败')
        }
      })
}

const deleteApply = (row) => {
  ElMessageBox.confirm('确定删除该申请吗？', '确认删除', { type: 'warning' })
      .then(async () => {
        const res = await request.delete(`/apply/delByapply_ID/${row.apply_ID}`)
        if (res.code === '200') {
          ElMessage.success('删除成功')
          tableData.value = tableData.value.filter(item => item.apply_ID !== row.apply_ID)
          total.value = Math.max(0, total.value - 1)
          await load()
        }
      })
}

onMounted(() => {
  load()
})

onActivated(() => {
  load()
})
</script>
