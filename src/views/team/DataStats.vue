<template>
  <div class="team-stats-page">
    <el-card class="page-header-card">
      <div class="page-header">
        <div>
          <div class="page-title">{{ userStore.userName }} 数据统计</div>
          <div class="page-subtitle">当前社团经费申请、审批进度与报销完成情况</div>
        </div>
        <el-tag type="primary" size="large">社团端视角</el-tag>
      </div>
    </el-card>

    <el-row :gutter="16" class="kpi-row">
      <el-col v-for="item in kpiCards" :key="item.label" :xs="24" :sm="12" :lg="8" :xl="4">
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
              <span>我的申请状态分布</span>
              <el-tag type="primary">流程状态</el-tag>
            </div>
          </template>
          <div ref="statusChart" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>经费类型分布</span>
              <el-tag type="success">按金额统计</el-tag>
            </div>
          </template>
          <div ref="typeChart" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :xs="24" :lg="16">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>近六月申请金额趋势</span>
              <el-tag type="warning">最近 6 个月</el-tag>
            </div>
          </template>
          <div ref="trendChart" class="trend-chart"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card class="progress-card">
          <template #header>
            <div class="card-header">
              <span>报销进度</span>
              <el-tag type="success">当前社团</el-tag>
            </div>
          </template>
          <div class="progress-block">
            <el-progress
              type="dashboard"
              :percentage="reimbursePercentValue"
              :width="168"
              color="#16a34a"
            />
            <div class="progress-title">报销完成率</div>
            <div class="progress-desc">已报销 {{ stats.reimbursedCount }} 条，通过或已报销 {{ stats.approvedCount }} 条</div>
          </div>
          <div class="progress-list">
            <div class="progress-item">
              <span>待处理申请</span>
              <strong>{{ stats.runningCount }} 条</strong>
            </div>
            <div class="progress-item">
              <span>驳回申请</span>
              <strong>{{ stats.rejectedCount }} 条</strong>
            </div>
            <div class="progress-item">
              <span>待报销金额</span>
              <strong>{{ formatMoney(pendingReimburseMoney) }} 元</strong>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, nextTick, onActivated, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user.js'
import request from '@/utils/request.js'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import {
  CircleCheck,
  Coin,
  Loading,
  Money,
  Tickets,
  Wallet
} from '@element-plus/icons-vue'

const userStore = useUserStore()
const router = useRouter()

const statusChart = ref(null)
const typeChart = ref(null)
const trendChart = ref(null)
const latestChartData = ref({
  applies: [],
  effectiveList: []
})

let statusInstance = null
let typeInstance = null
let trendInstance = null
let resizeObserver = null

const stats = reactive({
  totalApply: 0,
  runningCount: 0,
  approvedCount: 0,
  rejectedCount: 0,
  reimbursedCount: 0,
  totalMoney: 0,
  reimbursedMoney: 0,
  currentMonthCount: 0
})

const effectiveStatuses = ['待审核', '待二次审核', '待三次审核', '审核通过', '已报销']
const approvedStatuses = ['审核通过', '已报销']
const runningStatuses = ['待审核', '待二次审核', '待三次审核', '审核通过']
const allStatuses = ['待提交', '待审核', '待二次审核', '待三次审核', '审核通过', '审核驳回', '已报销']

const moneyValue = (value) => {
  const parsed = Number.parseFloat(value)
  return Number.isFinite(parsed) ? parsed : 0
}

const formatMoney = (value) => Number(value || 0).toLocaleString('zh-CN', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2
})

const percent = (numerator, denominator) => {
  if (!denominator) return '0.0%'
  return `${((numerator / denominator) * 100).toFixed(1)}%`
}

const percentNumber = (numerator, denominator) => {
  if (!denominator) return 0
  return Number(((numerator / denominator) * 100).toFixed(1))
}

const getDate = (value) => {
  if (!value) return null
  const date = new Date(String(value).replace(/-/g, '/'))
  return Number.isNaN(date.getTime()) ? null : date
}

const formatMonth = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  return `${year}-${month}`
}

const recentSixMonths = () => {
  const now = new Date()
  return Array.from({ length: 6 }, (_, index) => {
    const date = new Date(now.getFullYear(), now.getMonth() - (5 - index), 1)
    return formatMonth(date)
  })
}

const latestReimburse = (reimburses) => {
  return (reimburses || [])
    .filter(item => item.status === '已报销')
    .sort((a, b) => {
      const timeCompare = (b.time || '').localeCompare(a.time || '')
      if (timeCompare !== 0) return timeCompare
      return (b.reimburse_ID || 0) - (a.reimburse_ID || 0)
    })[0] || null
}

const kpiCards = computed(() => [
  {
    label: '申请总数',
    value: stats.totalApply,
    desc: '本社团累计提交记录',
    icon: Tickets,
    theme: 'blue'
  },
  {
    label: '审批中',
    value: stats.runningCount,
    desc: '审核中或等待报销',
    icon: Loading,
    theme: 'cyan'
  },
  {
    label: '审批通过率',
    value: percent(stats.approvedCount, stats.totalApply),
    desc: `${stats.approvedCount} 条已通过或已报销`,
    icon: CircleCheck,
    theme: 'green'
  },
  {
    label: '报销完成率',
    value: percent(stats.reimbursedCount, Math.max(stats.approvedCount, 1)),
    desc: `${stats.reimbursedCount} 条完成报销`,
    icon: Wallet,
    theme: 'orange'
  },
  {
    label: '有效申请金额',
    value: formatMoney(stats.totalMoney),
    desc: '单位：元，排除草稿和驳回',
    icon: Coin,
    theme: 'purple'
  },
  {
    label: '已报销金额',
    value: formatMoney(stats.reimbursedMoney),
    desc: '单位：元',
    icon: Money,
    theme: 'red'
  }
])

const reimbursePercentValue = computed(() => percentNumber(stats.reimbursedCount, Math.max(stats.approvedCount, 1)))
const pendingReimburseMoney = computed(() => Math.max(stats.totalMoney - stats.reimbursedMoney, 0))

const safeGetList = async (url, config) => {
  try {
    const res = await request.get(url, config)
    return res.code === '200' && Array.isArray(res.data) ? res.data : []
  } catch (error) {
    console.warn(`社团统计接口加载失败：${url}`, error)
    return []
  }
}

const waitForVisibleDom = (domRef) => {
  return new Promise(resolve => {
    let attempts = 0
    const check = () => {
      const el = domRef.value
      if (!el) {
        resolve(null)
        return
      }
      const rect = el.getBoundingClientRect()
      if (rect.width > 20 && rect.height > 20) {
        resolve(el)
        return
      }
      attempts += 1
      if (attempts >= 30) {
        resolve(el)
        return
      }
      requestAnimationFrame(check)
    }
    requestAnimationFrame(check)
  })
}

const getChart = (domRef, currentInstance) => {
  if (!domRef.value) return null
  if (currentInstance) return currentInstance
  return echarts.init(domRef.value, null, { renderer: 'canvas' })
}

const renderStatusChart = async (applies) => {
  await waitForVisibleDom(statusChart)
  const statusMap = Object.fromEntries(allStatuses.map(status => [status, 0]))
  applies.forEach(item => {
    statusMap[item.status] = (statusMap[item.status] || 0) + 1
  })

  let chartData = Object.entries(statusMap)
    .filter(([, value]) => value > 0)
    .map(([name, value]) => ({ name, value }))

  if (!chartData.length) {
    chartData = [{ name: '暂无数据', value: 1, itemStyle: { color: '#dcdfe6' } }]
  }

  statusInstance = getChart(statusChart, statusInstance)
  statusInstance?.setOption({
    color: ['#94a3b8', '#f59e0b', '#38bdf8', '#6366f1', '#22c55e', '#ef4444', '#10b981'],
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, left: 'center' },
    series: [{
      type: 'pie',
      radius: ['48%', '72%'],
      center: ['50%', '43%'],
      label: { formatter: '{b}\n{d}%' },
      data: chartData
    }]
  }, true)
}

const renderTypeChart = async (applies) => {
  await waitForVisibleDom(typeChart)
  const typeMap = {}
  applies.forEach(item => {
    const type = item.apply_type || '未分类'
    typeMap[type] = (typeMap[type] || 0) + moneyValue(item.apply_money)
  })

  let chartData = Object.entries(typeMap).map(([name, value]) => ({
    name,
    value: Number(value.toFixed(2))
  }))

  if (!chartData.length) {
    chartData = [
      { name: '活动经费', value: 6000 },
      { name: '物资采购', value: 3600 },
      { name: '宣传费用', value: 2200 }
    ]
  }

  typeInstance = getChart(typeChart, typeInstance)
  typeInstance?.setOption({
    color: ['#2563eb', '#16a34a', '#f97316', '#7c3aed', '#06b6d4', '#e11d48'],
    tooltip: {
      trigger: 'item',
      formatter: ({ name, value, percent }) => `${name}<br/>金额：${formatMoney(value)} 元<br/>占比：${percent}%`
    },
    legend: { bottom: 0, left: 'center' },
    series: [{
      type: 'pie',
      radius: ['0%', '68%'],
      center: ['50%', '43%'],
      roseType: 'radius',
      label: { formatter: '{b}\n{d}%' },
      data: chartData
    }]
  }, true)
}

const renderTrendChart = async (applies) => {
  await waitForVisibleDom(trendChart)
  const months = recentSixMonths()
  const monthMap = Object.fromEntries(months.map(month => [month, 0]))

  applies.forEach(item => {
    const date = getDate(item.apply_time)
    if (!date) return
    const month = formatMonth(date)
    if (monthMap[month] !== undefined) {
      monthMap[month] += moneyValue(item.apply_money)
    }
  })

  trendInstance = getChart(trendChart, trendInstance)
  trendInstance?.setOption({
    grid: { left: 60, right: 48, top: 36, bottom: 44 },
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        return `${params[0].axisValue}<br/>申请金额：${formatMoney(params[0]?.value || 0)} 元`
      }
    },
    xAxis: { type: 'category', boundaryGap: false, data: months },
    yAxis: {
      type: 'value',
      name: '金额(元)',
      splitLine: { lineStyle: { color: '#eef1f6' } }
    },
    series: [{
      name: '申请金额',
      type: 'line',
      smooth: true,
      symbolSize: 8,
      data: months.map(month => Number(monthMap[month].toFixed(2))),
      lineStyle: { width: 4, color: '#2563eb' },
      itemStyle: { color: '#2563eb' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(37, 99, 235, 0.24)' },
          { offset: 1, color: 'rgba(37, 99, 235, 0.02)' }
        ])
      }
    }]
  }, true)
}

const resetStats = () => {
  stats.totalApply = 0
  stats.runningCount = 0
  stats.approvedCount = 0
  stats.rejectedCount = 0
  stats.reimbursedCount = 0
  stats.totalMoney = 0
  stats.reimbursedMoney = 0
  stats.currentMonthCount = 0
}

const loadStats = async () => {
  if (!userStore.userId) {
    ElMessage.error('未获取到社团信息，请重新登录')
    router.push('/')
    return
  }

  const applies = await safeGetList('/apply/selectAll', { params: { team_ID: userStore.userId } })
  const effectiveList = applies.filter(item => effectiveStatuses.includes(item.status))
  const reimbursedList = applies.filter(item => item.status === '已报销')
  latestChartData.value = { applies, effectiveList }

  resetStats()
  stats.totalApply = applies.length
  stats.runningCount = applies.filter(item => runningStatuses.includes(item.status)).length
  stats.approvedCount = applies.filter(item => approvedStatuses.includes(item.status)).length
  stats.rejectedCount = applies.filter(item => item.status === '审核驳回').length
  stats.reimbursedCount = reimbursedList.length
  stats.totalMoney = effectiveList.reduce((sum, item) => sum + moneyValue(item.apply_money), 0)

  const currentMonth = formatMonth(new Date())
  stats.currentMonthCount = applies.filter(item => {
    const date = getDate(item.apply_time)
    return date && formatMonth(date) === currentMonth
  }).length

  const reimburseRecords = await Promise.all(
    reimbursedList.map(async item => {
      const records = await safeGetList(`/reimburse/selectByApply_ID/${item.apply_ID}`)
      return latestReimburse(records)
    })
  )
  stats.reimbursedMoney = reimburseRecords.reduce((sum, item) => sum + moneyValue(item?.money), 0)

  await nextTick()
  await renderAllCharts()
}

const resizeCharts = () => {
  statusInstance?.resize()
  typeInstance?.resize()
  trendInstance?.resize()
}

const renderAllCharts = async () => {
  const { applies, effectiveList } = latestChartData.value
  await Promise.all([
    renderStatusChart(applies),
    renderTypeChart(effectiveList),
    renderTrendChart(effectiveList)
  ])
  resizeCharts()
  requestAnimationFrame(resizeCharts)
  setTimeout(resizeCharts, 300)
  setTimeout(resizeCharts, 800)
}

const observeChartContainers = () => {
  if (!window.ResizeObserver) return
  resizeObserver?.disconnect()
  resizeObserver = new ResizeObserver(() => {
    resizeCharts()
  })
  ;[statusChart.value, typeChart.value, trendChart.value]
    .filter(Boolean)
    .forEach(el => resizeObserver.observe(el))
}

const handleDataRefresh = () => {
  loadStats()
}

onMounted(() => {
  loadStats()
  nextTick(observeChartContainers)
  window.addEventListener('member-changed', handleDataRefresh)
  window.addEventListener('apply-updated', handleDataRefresh)
  window.addEventListener('reimburse-changed', handleDataRefresh)
  window.addEventListener('resize', resizeCharts)
})

onActivated(() => {
  nextTick(() => {
    observeChartContainers()
    renderAllCharts()
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('member-changed', handleDataRefresh)
  window.removeEventListener('apply-updated', handleDataRefresh)
  window.removeEventListener('reimburse-changed', handleDataRefresh)
  window.removeEventListener('resize', resizeCharts)
  statusInstance?.dispose()
  typeInstance?.dispose()
  trendInstance?.dispose()
  resizeObserver?.disconnect()
})
</script>

<style scoped>
.team-stats-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header-card,
.kpi-card,
.chart-card,
.progress-card {
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

.theme-orange {
  background: linear-gradient(135deg, #fff7ed 0%, #ffffff 75%);
}
.theme-orange .kpi-icon { background: #f97316; }

.theme-purple {
  background: linear-gradient(135deg, #f5f3ff 0%, #ffffff 75%);
}
.theme-purple .kpi-icon { background: #7c3aed; }

.theme-red {
  background: linear-gradient(135deg, #fff1f2 0%, #ffffff 75%);
}
.theme-red .kpi-icon { background: #e11d48; }

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-weight: 700;
}

.chart-box {
  width: 100%;
  height: 370px;
  min-height: 370px;
}

.trend-chart {
  width: 100%;
  height: 360px;
  min-height: 360px;
}

.progress-card {
  height: 100%;
}

.progress-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 14px 0 18px;
}

.progress-title {
  margin-top: 8px;
  font-size: 17px;
  font-weight: 800;
  color: #111827;
}

.progress-desc {
  margin-top: 6px;
  font-size: 13px;
  color: #64748b;
}

.progress-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.progress-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-radius: 8px;
  background: #f8fafc;
  color: #475569;
}

.progress-item strong {
  color: #111827;
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
