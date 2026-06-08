<template>
  <div class="workflow-page">
    <el-card class="page-header-card">
      <div class="page-header">
        <div>
          <div class="page-title">工作流引擎监控</div>
          <div class="page-subtitle">Flowable 审批节点转化、耗时与最近操作记录</div>
        </div>
        <el-tag type="primary" size="large">审核流程可视化</el-tag>
      </div>
    </el-card>

    <el-row :gutter="16" class="kpi-row">
      <el-col v-for="item in workflowKpis" :key="item.label" :xs="24" :sm="12" :lg="8" :xl="4">
        <el-card class="kpi-card" :class="`theme-${item.theme}`" shadow="hover">
          <div class="kpi-content">
            <div class="kpi-icon">
              <el-icon><component :is="item.icon" /></el-icon>
            </div>
            <div>
              <div class="kpi-value">{{ item.value }}</div>
              <div class="kpi-label">{{ item.label }}</div>
              <div class="kpi-desc">{{ item.desc }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>审批流程漏斗</span>
              <el-tag type="success">节点转化</el-tag>
            </div>
          </template>
          <div ref="funnelChart" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>节点耗时分析</span>
              <el-tag type="warning">平均小时</el-tag>
            </div>
          </template>
          <div ref="durationChart" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="timeline-card">
      <template #header>
        <div class="card-header">
          <span>最近审批时间线</span>
          <el-tag type="primary">最新 {{ timelineList.length }} 条</el-tag>
        </div>
      </template>
      <el-timeline v-if="timelineList.length">
        <el-timeline-item
          v-for="item in timelineList"
          :key="item.key"
          :timestamp="item.time"
          :type="item.type"
          placement="top"
        >
          <div class="timeline-item">
            <div class="timeline-title">
              <span>{{ item.node }}</span>
              <el-tag :type="item.type" size="small">{{ item.result }}</el-tag>
            </div>
            <div class="timeline-meta">操作人：{{ item.operator }}</div>
            <div class="timeline-opinion">审批意见：{{ item.opinion || '无' }}</div>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无审批记录，流程产生后会自动展示" />
    </el-card>

    <el-card class="detail-card">
      <template #header>
        <div class="card-header">
          <span>全局流程统计明细</span>
          <el-tag type="info">保留原始统计表</el-tag>
        </div>
      </template>
      <el-table :data="flowStats" stripe>
        <el-table-column prop="status" label="流程状态">
          <template #default="{ row }">
            <el-tag :type="statusTypeMap[row.status] || 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="count" label="数量" />
        <el-table-column prop="avgTime" label="平均耗时(小时)" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import request from '@/utils/request.js'
import * as echarts from 'echarts'
import {
  Document,
  Finished,
  Operation,
  Timer,
  Warning
} from '@element-plus/icons-vue'

const funnelChart = ref(null)
const durationChart = ref(null)
const flowStats = ref([])
const timelineList = ref([])

let funnelInstance = null
let durationInstance = null

const workflowSummary = reactive({
  totalApplies: 0,
  runningCount: 0,
  completedCount: 0,
  rejectedCount: 0,
  avgApprovalHours: '-'
})

const statusTypeMap = {
  '待提交': 'info',
  '待审核': 'warning',
  '审核驳回': 'danger',
  '待二次审核': 'warning',
  '待三次审核': 'warning',
  '审核通过': 'success',
  '已报销': 'success'
}

const funnelNodes = ['待提交', '待审核', '待二次审核', '待三次审核', '审核通过', '已报销']
const durationNodes = ['待审核', '待二次审核', '待三次审核', '审核通过', '已报销']
const nodeNameMap = {
  1: '一级审核',
  2: '二级审核',
  3: '三级审核'
}

const workflowKpis = computed(() => [
  {
    label: '申请总数',
    value: workflowSummary.totalApplies,
    desc: '进入系统的全部经费流程',
    icon: Document,
    theme: 'blue'
  },
  {
    label: '运行中流程数',
    value: workflowSummary.runningCount,
    desc: '正在审核或等待报销',
    icon: Operation,
    theme: 'cyan'
  },
  {
    label: '已完成流程数',
    value: workflowSummary.completedCount,
    desc: '已完成报销闭环',
    icon: Finished,
    theme: 'green'
  },
  {
    label: '驳回率',
    value: percent(workflowSummary.rejectedCount, workflowSummary.totalApplies),
    desc: `${workflowSummary.rejectedCount} 条申请被驳回`,
    icon: Warning,
    theme: 'red'
  },
  {
    label: '平均审批时长',
    value: workflowSummary.avgApprovalHours,
    desc: '单位：小时，不含待提交',
    icon: Timer,
    theme: 'orange'
  }
])

const percent = (numerator, denominator) => {
  if (!denominator) return '0.0%'
  return `${((numerator / denominator) * 100).toFixed(1)}%`
}

const getDate = (value) => {
  if (!value) return null
  const date = value instanceof Date ? value : new Date(String(value).replace(/-/g, '/'))
  return Number.isNaN(date.getTime()) ? null : date
}

const safeGetList = async (url) => {
  try {
    const res = await request.get(url)
    return res.code === '200' && Array.isArray(res.data) ? res.data : []
  } catch (error) {
    console.warn(`工作流统计接口加载失败：${url}`, error)
    return []
  }
}

const getHoursDiff = (startTime, endTime) => {
  const start = getDate(startTime)
  const end = getDate(endTime)
  if (!start || !end || start > end) return null
  const diffHours = (end - start) / (1000 * 60 * 60)
  return Math.round(diffHours * 100) / 100
}

const avg = (times) => {
  if (!times.length) return null
  return times.reduce((sum, value) => sum + value, 0) / times.length
}

const avgText = (times) => {
  const value = avg(times)
  return value === null ? '-' : value.toFixed(2)
}

const getLatestApproveMap = (approves) => {
  const map = new Map()
  approves.forEach(approve => {
    if (!approve.apply_ID || !approve.sequence || !approve.approve_time) return
    const key = `${approve.apply_ID}_${approve.sequence}`
    const existing = map.get(key)
    if (!existing || getDate(approve.approve_time) > getDate(existing.approve_time)) {
      map.set(key, approve)
    }
  })
  return map
}

const getLatestReimburseMap = (reimburses) => {
  const map = new Map()
  reimburses.forEach(reimburse => {
    if (!reimburse.apply_ID || reimburse.status !== '已报销') return
    const existing = map.get(reimburse.apply_ID)
    const currentTime = getDate(reimburse.time)
    const existingTime = getDate(existing?.time)
    if (!existing || (currentTime && (!existingTime || currentTime > existingTime))) {
      map.set(reimburse.apply_ID, reimburse)
    }
  })
  return map
}

const getRejectTime = (applyId, approveMap) => {
  return [1, 2, 3]
    .map(sequence => approveMap.get(`${applyId}_${sequence}`))
    .filter(approve => approve?.status === '审核驳回' && approve.approve_time)
    .sort((a, b) => getDate(b.approve_time) - getDate(a.approve_time))[0]?.approve_time
}

const getApprovalFinishTime = (apply, approveMap) => {
  const rejectTime = getRejectTime(apply.apply_ID, approveMap)
  if (rejectTime) return rejectTime
  return approveMap.get(`${apply.apply_ID}_3`)?.approve_time
    || approveMap.get(`${apply.apply_ID}_2`)?.approve_time
    || approveMap.get(`${apply.apply_ID}_1`)?.approve_time
}

const buildStats = (applies, approveMap, reimburseMap) => {
  const statsData = {
    '待提交': { times: [], count: 0 },
    '待审核': { times: [], count: 0 },
    '待二次审核': { times: [], count: 0 },
    '待三次审核': { times: [], count: 0 },
    '审核通过': { times: [], count: 0 },
    '审核驳回': { times: [], count: 0 },
    '已报销': { times: [], count: 0 }
  }

  const approvalDurations = []
  const now = new Date()

  applies.forEach(apply => {
    const status = apply.status || '待提交'
    if (!statsData[status]) {
      statsData[status] = { times: [], count: 0 }
    }
    statsData[status].count += 1

    const applyTime = apply.apply_time
    if (!applyTime) return

    const firstAudit = approveMap.get(`${apply.apply_ID}_1`)
    const secondAudit = approveMap.get(`${apply.apply_ID}_2`)
    const thirdAudit = approveMap.get(`${apply.apply_ID}_3`)

    if (status === '待提交' || status === '待审核') {
      const hours = getHoursDiff(applyTime, now)
      if (hours !== null) statsData[status].times.push(hours)
    } else if (status === '待二次审核') {
      const hours = getHoursDiff(firstAudit?.approve_time || applyTime, now)
      if (hours !== null) statsData[status].times.push(hours)
    } else if (status === '待三次审核') {
      const hours = getHoursDiff(secondAudit?.approve_time || firstAudit?.approve_time || applyTime, now)
      if (hours !== null) statsData[status].times.push(hours)
    } else if (status === '审核通过') {
      const hours = getHoursDiff(applyTime, thirdAudit?.approve_time)
      if (hours !== null) statsData[status].times.push(hours)
    } else if (status === '审核驳回') {
      const hours = getHoursDiff(applyTime, getRejectTime(apply.apply_ID, approveMap))
      if (hours !== null) statsData[status].times.push(hours)
    } else if (status === '已报销') {
      const hours = getHoursDiff(applyTime, reimburseMap.get(apply.apply_ID)?.time || thirdAudit?.approve_time)
      if (hours !== null) statsData[status].times.push(hours)
    }

    const approvalFinishTime = getApprovalFinishTime(apply, approveMap)
    const approvalHours = getHoursDiff(applyTime, approvalFinishTime)
    if (approvalHours !== null && status !== '待提交') {
      approvalDurations.push(approvalHours)
    }
  })

  workflowSummary.totalApplies = applies.length
  workflowSummary.runningCount = applies.filter(apply => ['待审核', '待二次审核', '待三次审核', '审核通过'].includes(apply.status)).length
  workflowSummary.completedCount = applies.filter(apply => apply.status === '已报销').length
  workflowSummary.rejectedCount = applies.filter(apply => apply.status === '审核驳回').length
  workflowSummary.avgApprovalHours = avgText(approvalDurations)

  flowStats.value = Object.entries(statsData).map(([status, data]) => ({
    status,
    count: data.count,
    avgTime: avgText(data.times),
    rawAvg: avg(data.times) || 0
  }))

  return statsData
}

const getChart = (domRef, currentInstance) => {
  if (!domRef.value) return null
  if (currentInstance) return currentInstance
  return echarts.init(domRef.value)
}

const renderFunnelChart = (applies) => {
  const counts = Object.fromEntries(funnelNodes.map(status => [status, 0]))
  applies.forEach(apply => {
    if (counts[apply.status] !== undefined) counts[apply.status] += 1
  })

  const hasData = applies.length > 0
  const fallback = [18, 14, 10, 8, 6, 3]
  const data = funnelNodes.map((name, index) => ({
    name,
    value: hasData ? counts[name] : fallback[index]
  }))

  funnelInstance = getChart(funnelChart, funnelInstance)
  funnelInstance?.setOption({
    color: ['#64748b', '#f59e0b', '#38bdf8', '#6366f1', '#22c55e', '#10b981'],
    tooltip: {
      trigger: 'item',
      formatter: ({ name, value }) => `${name}<br/>流程数量：${value}${hasData ? '' : '（占位）'}`
    },
    series: [{
      name: '流程节点',
      type: 'funnel',
      left: '8%',
      top: 24,
      bottom: 20,
      width: '84%',
      minSize: '28%',
      maxSize: '100%',
      sort: 'none',
      gap: 3,
      label: {
        show: true,
        position: 'inside',
        formatter: '{b}: {c}'
      },
      itemStyle: {
        borderColor: '#fff',
        borderWidth: 2
      },
      data
    }]
  }, true)
}

const renderDurationChart = (statsData, applies) => {
  const hasData = applies.length > 0
  const fallback = [2.5, 4.2, 3.8, 9.6, 12.4]
  const data = durationNodes.map((status, index) => {
    const value = avg(statsData[status]?.times || [])
    return Number((value === null ? (hasData ? 0 : fallback[index]) : value).toFixed(2))
  })

  durationInstance = getChart(durationChart, durationInstance)
  durationInstance?.setOption({
    grid: { left: 58, right: 22, top: 30, bottom: 46 },
    color: ['#2563eb'],
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => `${params[0].axisValue}<br/>平均耗时：${params[0].value} 小时${hasData ? '' : '（占位）'}`
    },
    xAxis: {
      type: 'category',
      data: durationNodes,
      axisLabel: { interval: 0 }
    },
    yAxis: {
      type: 'value',
      name: '小时',
      splitLine: { lineStyle: { color: '#eef1f6' } }
    },
    series: [{
      type: 'bar',
      barWidth: 28,
      data,
      label: { show: true, position: 'top', formatter: '{c}h' },
      itemStyle: {
        borderRadius: [8, 8, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#2563eb' },
          { offset: 1, color: '#06b6d4' }
        ])
      }
    }]
  }, true)
}

const buildTimeline = (applies, approves, reimburses) => {
  const approveItems = approves
    .filter(item => item.approve_time)
    .map(item => ({
      key: `approve-${item.approve_ID}`,
      time: item.approve_time,
      operator: item.teacher_name || item.role || '审批人',
      node: nodeNameMap[item.sequence] || '审批节点',
      result: item.status || '已处理',
      opinion: item.opinion,
      type: item.status === '审核驳回' ? 'danger' : 'success'
    }))

  const reimburseItems = reimburses
    .filter(item => item.time && item.status === '已报销')
    .map(item => ({
      key: `reimburse-${item.reimburse_ID}`,
      time: item.time,
      operator: item.teacher_ID ? `财务人员 #${item.teacher_ID}` : '财务人员',
      node: '执行报销',
      result: '已报销',
      opinion: `报销金额 ${item.money || 0} 元`,
      type: 'success'
    }))

  let items = [...approveItems, ...reimburseItems]

  if (!items.length) {
    items = applies
      .filter(item => item.apply_time)
      .map(item => ({
        key: `apply-${item.apply_ID}`,
        time: item.apply_time,
        operator: `社团 #${item.team_ID || '-'}`,
        node: '申请提交',
        result: item.status || '待提交',
        opinion: item.reason || '系统根据申请记录生成',
        type: statusTypeMap[item.status] || 'info'
      }))
  }

  timelineList.value = items
    .sort((a, b) => getDate(b.time) - getDate(a.time))
    .slice(0, 8)
}

const loadStats = async () => {
  const [applies, approves, reimburses] = await Promise.all([
    safeGetList('/apply/selectAll'),
    safeGetList('/approve/selectAll'),
    safeGetList('/reimburse/selectAll')
  ])

  const approveMap = getLatestApproveMap(approves)
  const reimburseMap = getLatestReimburseMap(reimburses)
  const statsData = buildStats(applies, approveMap, reimburseMap)

  await nextTick()
  renderFunnelChart(applies)
  renderDurationChart(statsData, applies)
  buildTimeline(applies, approves, reimburses)
  requestAnimationFrame(resizeCharts)
  setTimeout(resizeCharts, 200)
}

const resizeCharts = () => {
  funnelInstance?.resize()
  durationInstance?.resize()
}

onMounted(() => {
  loadStats()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  funnelInstance?.dispose()
  durationInstance?.dispose()
})
</script>

<style scoped>
.workflow-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header-card,
.kpi-card,
.chart-card,
.timeline-card,
.detail-card {
  border-radius: 8px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-title {
  font-size: 22px;
  font-weight: 800;
  color: #111827;
}

.page-subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: #64748b;
}

.kpi-row,
.chart-row {
  row-gap: 16px;
}

.kpi-card {
  height: 132px;
  border: 0;
}

.kpi-content {
  display: flex;
  align-items: center;
  gap: 14px;
  height: 84px;
}

.kpi-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  font-size: 25px;
  color: #ffffff;
  flex: 0 0 auto;
}

.kpi-value {
  font-size: 28px;
  line-height: 34px;
  font-weight: 800;
  color: #111827;
}

.kpi-label {
  margin-top: 4px;
  font-size: 14px;
  font-weight: 700;
  color: #374151;
}

.kpi-desc {
  margin-top: 4px;
  font-size: 12px;
  color: #6b7280;
}

.theme-blue {
  background: linear-gradient(135deg, #eff6ff 0%, #ffffff 75%);
}
.theme-blue .kpi-icon { background: #2563eb; }

.theme-cyan {
  background: linear-gradient(135deg, #ecfeff 0%, #ffffff 75%);
}
.theme-cyan .kpi-icon { background: #0891b2; }

.theme-green {
  background: linear-gradient(135deg, #f0fdf4 0%, #ffffff 75%);
}
.theme-green .kpi-icon { background: #16a34a; }

.theme-red {
  background: linear-gradient(135deg, #fff1f2 0%, #ffffff 75%);
}
.theme-red .kpi-icon { background: #e11d48; }

.theme-orange {
  background: linear-gradient(135deg, #fff7ed 0%, #ffffff 75%);
}
.theme-orange .kpi-icon { background: #f97316; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  font-weight: 700;
}

.chart-box {
  width: 100%;
  height: 390px;
  min-height: 390px;
}

.timeline-item {
  padding: 12px 14px;
  border-radius: 8px;
  background: #f8fafc;
  border-left: 4px solid #2563eb;
}

.timeline-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-weight: 700;
  color: #1f2937;
}

.timeline-meta,
.timeline-opinion {
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 768px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .kpi-value {
    font-size: 24px;
  }
}
</style>
