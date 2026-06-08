<template>
  <div>
    <el-card>
      <div style="margin-bottom: 15px">
        <el-select
            v-model="searchForm.status"
            placeholder="状态筛选"
            clearable
            style="width: 150px; margin-right: 10px"
            @change="handleSearch"
            @clear="handleSearch"
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
            @change="handleSearch"
            @clear="handleSearch"
        >
          <el-option label="活动费" value="活动费" />
          <el-option label="比赛费" value="比赛费" />
          <el-option label="采购费" value="采购费" />
          <el-option label="设备费" value="设备费" />
          <el-option label="培训费" value="培训费" />
          <el-option label="其他" value="其他" />
        </el-select>

        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>

      <CommonTable :data="tableData" :columns="columns" :total="total" v-model:page-num="pageNum" v-model:page-size="pageSize" @page-change="load">
        <template #status="{ row }">
          <el-tag :type="statusTypeMap[row.status]">
            {{ row.status }}
          </el-tag>
        </template>
        <template #action="{ row }">
          <el-button type="info" size="small" @click="viewDetail(row)">详情</el-button>
          <el-button v-if="row.status !== '已报销'" type="warning" size="small" @click="forceReject(row)">强制驳回</el-button>
          <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </CommonTable>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog title="申请详情与管理" v-model="detailVisible" width="700">
      <el-descriptions :column="2" border v-if="currentApply">
        <el-descriptions-item label="申请编号">{{ currentApply.apply_ID }}</el-descriptions-item>
        <el-descriptions-item label="申请社团">{{ currentApply.team_name }}</el-descriptions-item>
        <el-descriptions-item label="申请金额">{{ currentApply.apply_money }} 元</el-descriptions-item>
        <el-descriptions-item label="申请类型">{{ currentApply.apply_type }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">
          <el-tag :type="statusTypeMap[currentApply.status]">
            {{ currentApply.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ currentApply.apply_time }}</el-descriptions-item>
        <el-descriptions-item label="收款账户">{{ currentApply.account_name }}</el-descriptions-item>
        <el-descriptions-item label="开户银行">{{ currentApply.bank }}</el-descriptions-item>
        <el-descriptions-item label="银行账号">{{ currentApply.account }}</el-descriptions-item>
        <el-descriptions-item label="事项分类">{{ currentApply.tags || '无' }}</el-descriptions-item>
        <el-descriptions-item label="申请原因" :span="2">{{ currentApply.reason }}</el-descriptions-item>
      </el-descriptions>

      <!-- 审核流程 -->
      <div style="margin-top: 20px">
        <h4>审核流程记录</h4>
        <el-timeline>
          <el-timeline-item
              v-for="(item, index) in auditList"
              :key="index"
              :type="item.status === '审核通过' ? 'success' : item.status === '审核驳回' ? 'danger' : 'primary'"
          >
            <p><strong>第{{ item.sequence }}级审核（{{ item.role }}）</strong></p>
            <p>审核人：{{ item.teacher_name || item.teacher_ID || '待定' }} | 时间：{{ item.approve_time || '待审核' }}</p>
            <p>意见：{{ item.opinion || '暂无' }}</p>
            <p>结果：{{ item.status }}</p>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-dialog>

    <!-- 强制驳回 -->
    <el-dialog title="强制驳回申请" v-model="statusVisible" width="420">
      <el-alert
          title="强制驳回会终止当前工作流，申请将变为审核驳回，社团可编辑后重新提交。"
          type="warning"
          :closable="false"
          style="margin-bottom: 12px"
      />
      <el-form v-if="currentApply">
        <el-form-item label="当前状态">
          <el-tag>{{ currentApply.status }}</el-tag>
        </el-form-item>
        <el-form-item label="驳回原因">
          <el-input
              v-model="rejectOpinion"
              type="textarea"
              :rows="3"
              placeholder="请输入强制驳回原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusVisible = false">取消</el-button>
        <el-button type="warning" @click="confirmForceReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import request from '@/utils/request.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import CommonTable from '@/components/CommonTable.vue'
import { useUserStore } from '@/stores/user.js'

const userStore = useUserStore()
const searchForm = reactive({ status: '', apply_type: '' })
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const clubList = ref([])

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
  { prop: 'team_name', label: '社团', width: 150 },
  { prop: 'apply_money', label: '金额(元)', width: 120 },
  { prop: 'apply_type', label: '类型', width: 100 },
  { prop: 'status', label: '状态', width: 120, slot: true },
  { prop: 'apply_time', label: '申请时间', width: 160 }
]

const detailVisible = ref(false)
const statusVisible = ref(false)
const currentApply = ref(null)
const auditList = ref([])
const rejectOpinion = ref('')

const loadClubs = async () => {
  const res = await request.get('/team/selectAll')
  if (res.code === '200') clubList.value = res.data || []
}

const load = async () => {
  const params = {
    pageNum: pageNum.value,
    pageSize: pageSize.value
  }

  if (searchForm.status) {
    params.status = searchForm.status
  }
  if (searchForm.apply_type) {
    params.apply_type = searchForm.apply_type
  }

  const res = await request.get('/apply/selectPage', { params })
  if (res.code === '200') {
    const list = res.data.list || []
    for (let item of list) {
      const club = clubList.value.find(c => c.team_ID === item.team_ID)
      item.team_name = club?.team_name || '未知'
    }
    tableData.value = list
    total.value = res.data.total
  }
}

const handleSearch = () => {
  pageNum.value = 1
  load()
}

let debounceTimer = null
watch(
    () => [searchForm.status, searchForm.apply_type],
    () => {
      if (debounceTimer) clearTimeout(debounceTimer)
      debounceTimer = setTimeout(() => {
        pageNum.value = 1
        load()
      }, 300)
    }
)

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

const forceReject = (row) => {
  if (row.status === '已报销') {
    ElMessage.warning('已报销申请不能强制驳回')
    return
  }
  currentApply.value = row
  rejectOpinion.value = ''
  statusVisible.value = true
}

const confirmForceReject = () => {
  const userInfo = userStore.userInfo || {}
  request.post('/apply/forceReject', null, {
    params: {
      applyId: currentApply.value.apply_ID,
      adminId: userStore.userId,
      adminName: userInfo.name || '管理员',
      opinion: rejectOpinion.value || '管理员强制驳回'
    }
  }).then(res => {
    if (res.code === '200') {
      ElMessage.success('已强制驳回')
      statusVisible.value = false
      load()
    } else {
      ElMessage.error(res.msg || '强制驳回失败')
    }
  }).catch(err => {
    console.error('强制驳回失败:', err)
    ElMessage.error(err.message || '强制驳回失败')
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除申请#${row.apply_ID}吗？`, '确认删除', { type: 'warning' })
      .then(() => {
        request.delete(`/apply/delByapply_ID/${row.apply_ID}`).then(res => {
          if (res.code === '200') {
            ElMessage.success('删除成功')
            load()
          }
        })
      })
}

loadClubs().then(() => load())
</script>
