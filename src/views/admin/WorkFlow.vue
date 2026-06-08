<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>工作流引擎监控</span>
          <el-tag type="primary">审核流程可视化</el-tag>
        </div>
      </template>


      <!-- 流程图展示 -->
      <div v-if="currentFlow" class="flow-container">
        <h3>申请 #{{ currentFlow.apply_ID }} 的审核流程</h3>
        <div class="flow-status">
          当前状态：<el-tag :type="statusTypeMap[currentFlow.apply_status]">{{ currentFlow.apply_status }}</el-tag>
        </div>

        <div class="flow-steps">
          <div
              v-for="(step, index) in flowSteps"
              :key="index"
              class="flow-step"
              :class="{
              'step-completed': step.completed,
              'step-current': step.current,
              'step-pending': !step.completed && !step.current
            }"
          >
            <div class="step-icon">
              <el-icon v-if="step.completed && step.status === '审核通过'"><Check /></el-icon>
              <el-icon v-else-if="step.completed && step.status === '审核驳回'"><Close /></el-icon>
              <el-icon v-else><MoreFilled /></el-icon>
            </div>
            <div class="step-content">
              <div class="step-title">{{ step.title }}</div>
              <div class="step-actor">{{ step.actor }}</div>
              <div class="step-time" v-if="step.time">{{ step.time }}</div>
              <div class="step-opinion" v-if="step.opinion">意见：{{ step.opinion }}</div>
              <el-tag v-if="step.status" :type="step.status === '审核通过' ? 'success' : 'danger'" size="small">
                {{ step.status }}
              </el-tag>
            </div>
            <div v-if="index < flowSteps.length - 1" class="step-arrow">→</div>
          </div>
        </div>
      </div>

      <!-- 全局流程统计 -->
      <el-divider content-position="left">全局流程统计</el-divider>

      <el-table :data="flowStats" stripe>
        <el-table-column prop="status" label="流程状态" />
        <el-table-column prop="count" label="数量" />
        <el-table-column prop="avgTime" label="平均耗时(小时)" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import request from '@/utils/request.js'
import { ElMessage } from 'element-plus'
import { Check, Close, MoreFilled } from '@element-plus/icons-vue'

const searchApplyId = ref('')
const currentFlow = ref(null)
const flowSteps = ref([])

const statusTypeMap = {
  '待提交': 'info',
  '待审核': 'warning',
  '审核驳回': 'danger',
  '待二次审核': 'warning',
  '待三次审核': 'warning',
  '审核通过': 'success',
  '已报销': 'success'
}

const flowStats = ref([])

// 计算两个时间之间的耗时（小时），确保是正数
const getHoursDiff = (startTime, endTime) => {
  if (!startTime || !endTime) return null
  const start = new Date(startTime)
  const end = new Date(endTime)
  // 确保开始时间早于结束时间
  if (start > end) {
    return null
  }
  const diffMs = end - start
  const diffHours = diffMs / (1000 * 60 * 60)
  return Math.round(diffHours * 100) / 100
}

const searchFlow = async () => {
  if (!searchApplyId.value) {
    ElMessage.warning('请输入申请编号')
    return
  }

  const applyRes = await request.get(`/apply/selectByapply_ID/${searchApplyId.value}`)

  if (applyRes.code !== '200' || !applyRes.data) {
    ElMessage.error('申请不存在')
    return
  }

  const apply = applyRes.data
  currentFlow.value = {
    apply_ID: apply.apply_ID,
    apply_status: apply.status
  }

  const auditRes = await request.get(`/approve/selectByApply_ID/${apply.apply_ID}`)

  let audits = auditRes.code === '200' ? (auditRes.data || []) : []

  const latestAuditsMap = new Map()
  audits.forEach(audit => {
    if (!audit.approve_time) return
    const sequence = audit.sequence
    const existing = latestAuditsMap.get(sequence)
    if (!existing || new Date(audit.approve_time) > new Date(existing.approve_time)) {
      latestAuditsMap.set(sequence, audit)
    }
  })
  const latestAudits = Array.from(latestAuditsMap.values())

  const steps = [
    {
      title: '申请提交',
      actor: '社团',
      completed: true,
      current: false,
      time: apply.apply_time,
      status: null,
      opinion: null
    },
    {
      title: '一级审核',
      actor: '指导老师',
      completed: latestAudits.some(a => a.sequence === 1),
      current: apply.status === '待审核',
      time: latestAudits.find(a => a.sequence === 1)?.approve_time,
      status: latestAudits.find(a => a.sequence === 1)?.status,
      opinion: latestAudits.find(a => a.sequence === 1)?.opinion
    },
    {
      title: '二级审核',
      actor: '管理员',
      completed: latestAudits.some(a => a.sequence === 2),
      current: apply.status === '待二次审核',
      time: latestAudits.find(a => a.sequence === 2)?.approve_time,
      status: latestAudits.find(a => a.sequence === 2)?.status,
      opinion: latestAudits.find(a => a.sequence === 2)?.opinion
    },
    {
      title: '三级审核',
      actor: '管理员',
      completed: latestAudits.some(a => a.sequence === 3),
      current: apply.status === '待三次审核',
      time: latestAudits.find(a => a.sequence === 3)?.approve_time,
      status: latestAudits.find(a => a.sequence === 3)?.status,
      opinion: latestAudits.find(a => a.sequence === 3)?.opinion
    },
    {
      title: '经费报销',
      actor: '财务管理员',
      completed: apply.status === '已报销',
      current: apply.status === '审核通过',
      time: null,
      status: apply.status === '已报销' ? '已报销' : null,
      opinion: null
    }
  ]

  let stopNext = false
  for (let i = 0; i < steps.length; i++) {
    const step = steps[i]
    if (stopNext) {
      step.completed = false
      step.current = false
    }
    if (step.status === '审核驳回') {
      stopNext = true
    }
  }

  flowSteps.value = steps
}

// 加载全局统计并计算平均耗时
const loadStats = async () => {
  const applyRes = await request.get('/apply/selectAll')
  const applies = applyRes.code === '200' ? (applyRes.data || []) : []

  // 获取所有审核记录
  const approveRes = await request.get('/approve/selectAll')
  const allApproves = approveRes.code === '200' ? (approveRes.data || []) : []

  // 按 apply_ID 和 sequence 分组，每组取最新的
  const approvesByApply = new Map()
  allApproves.forEach(approve => {
    if (!approve.approve_time) return
    const key = `${approve.apply_ID}_${approve.sequence}`
    const existing = approvesByApply.get(key)
    if (!existing || new Date(approve.approve_time) > new Date(existing.approve_time)) {
      approvesByApply.set(key, approve)
    }
  })

  // 获取所有报销记录
  const reimburseRes = await request.get('/reimburse/selectAll')
  const allReimburses = reimburseRes.code === '200' ? (reimburseRes.data || []) : []
  const reimburseMap = new Map()
  allReimburses.forEach(r => {
    if (r.status === '已报销') {
      reimburseMap.set(r.apply_ID, r)
    }
  })

  // 统计各状态的耗时
  const statsData = {
    '待提交': { times: [], count: 0 },
    '待审核': { times: [], count: 0 },
    '待二次审核': { times: [], count: 0 },
    '待三次审核': { times: [], count: 0 },
    '审核通过': { times: [], count: 0 },
    '审核驳回': { times: [], count: 0 },
    '已报销': { times: [], count: 0 }
  }

  const now = new Date()

  for (const apply of applies) {
    const status = apply.status
    if (statsData[status]) {
      statsData[status].count++
    }

    const applyTime = apply.apply_time
    if (!applyTime) continue

    // 根据状态计算耗时
    if (status === '待提交') {
      // 待提交：从申请到现在
      const hours = getHoursDiff(applyTime, now)
      if (hours !== null && hours >= 0) statsData['待提交'].times.push(hours)
    }
    else if (status === '待审核') {
      // 待审核：从申请到现在
      const hours = getHoursDiff(applyTime, now)
      if (hours !== null && hours >= 0) statsData['待审核'].times.push(hours)
    }
    else if (status === '待二次审核') {
      // 待二次审核：从一审完成到现在
      const firstAudit = approvesByApply.get(`${apply.apply_ID}_1`)
      if (firstAudit && firstAudit.approve_time) {
        const hours = getHoursDiff(firstAudit.approve_time, now)
        if (hours !== null && hours >= 0) statsData['待二次审核'].times.push(hours)
      }
    }
    else if (status === '待三次审核') {
      // 待三次审核：从二审完成到现在
      const secondAudit = approvesByApply.get(`${apply.apply_ID}_2`)
      if (secondAudit && secondAudit.approve_time) {
        const hours = getHoursDiff(secondAudit.approve_time, now)
        if (hours !== null && hours >= 0) statsData['待三次审核'].times.push(hours)
      }
    }
    else if (status === '审核通过') {
      // 审核通过：从申请到三审完成
      const thirdAudit = approvesByApply.get(`${apply.apply_ID}_3`)
      if (thirdAudit && thirdAudit.approve_time) {
        const hours = getHoursDiff(applyTime, thirdAudit.approve_time)
        if (hours !== null && hours >= 0) statsData['审核通过'].times.push(hours)
      }
    }
    else if (status === '审核驳回') {
      // 审核驳回：从申请到驳回时间
      let rejectTime = null
      const firstAudit = approvesByApply.get(`${apply.apply_ID}_1`)
      if (firstAudit && firstAudit.status === '审核驳回') rejectTime = firstAudit.approve_time
      const secondAudit = approvesByApply.get(`${apply.apply_ID}_2`)
      if (secondAudit && secondAudit.status === '审核驳回') rejectTime = secondAudit.approve_time
      const thirdAudit = approvesByApply.get(`${apply.apply_ID}_3`)
      if (thirdAudit && thirdAudit.status === '审核驳回') rejectTime = thirdAudit.approve_time

      if (rejectTime) {
        const hours = getHoursDiff(applyTime, rejectTime)
        if (hours !== null && hours >= 0) statsData['审核驳回'].times.push(hours)
      }
    }
    else if (status === '已报销') {
      // 已报销：从申请到报销完成
      const reimburse = reimburseMap.get(apply.apply_ID)
      if (reimburse && reimburse.time) {
        const hours = getHoursDiff(applyTime, reimburse.time)
        if (hours !== null && hours >= 0) statsData['已报销'].times.push(hours)
      }
    }
  }

  // 计算平均耗时
  const calculateAvg = (times) => {
    if (times.length === 0) return '-'
    const sum = times.reduce((a, b) => a + b, 0)
    return (sum / times.length).toFixed(2)
  }

  flowStats.value = Object.entries(statsData).map(([status, data]) => ({
    status,
    count: data.count,
    avgTime: calculateAvg(data.times)
  }))

}

onMounted(() => {
  loadStats()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.flow-container {
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 20px;
}
.flow-status {
  margin: 15px 0;
  font-size: 16px;
}
.flow-steps {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  flex-wrap: wrap;
}
.flow-step {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 15px;
  border-radius: 8px;
  min-width: 180px;
}
.step-completed {
  background: #f0f9eb;
  border: 2px solid #67c23a;
}
.step-current {
  background: #fdf6ec;
  border: 2px solid #e6a23c;
  animation: pulse 2s infinite;
}
.step-pending {
  background: #f4f4f5;
  border: 2px solid #909399;
  opacity: 0.6;
}
@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(230, 162, 60, 0.4); }
  70% { box-shadow: 0 0 0 10px rgba(230, 162, 60, 0); }
  100% { box-shadow: 0 0 0 0 rgba(230, 162, 60, 0); }
}
.step-icon {
  font-size: 24px;
}
.step-content {
  flex: 1;
}
.step-title {
  font-weight: bold;
  font-size: 14px;
}
.step-actor {
  color: #666;
  font-size: 12px;
}
.step-time {
  color: #999;
  font-size: 12px;
}
.step-opinion {
  color: #409eff;
  font-size: 12px;
  margin-top: 5px;
}
.step-arrow {
  font-size: 24px;
  color: #909399;
}
</style>
