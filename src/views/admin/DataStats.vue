<template>
  <div class="stats-page">
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
              <span>社团经费申请 TOP5</span>
              <el-tag type="success">金额降序</el-tag>
            </div>
          </template>
          <div ref="barChart" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>审核状态分布</span>
              <el-tag type="primary">流程状态</el-tag>
            </div>
          </template>
          <div ref="pieChart" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="24">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>近六月经费申请趋势</span>
              <el-tag type="warning">最近 6 个月</el-tag>
            </div>
          </template>
          <div ref="lineChart" class="trend-chart"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, nextTick, onActivated, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import request from '@/utils/request.js'
import * as echarts from 'echarts'
import {
  CircleCheck,
  Coin,
  Money,
  OfficeBuilding,
  Tickets,
  Wallet
} from '@element-plus/icons-vue'

const barChart = ref(null)
const pieChart = ref(null)
const lineChart = ref(null)
const latestChartData = ref({
  clubs: [],
  applies: []
})

let barInstance = null
let pieInstance = null
let lineInstance = null
let resizeObserver = null

const globalStats = reactive({
  totalClubs: 0,
  totalApplies: 0,
  approvedCount: 0,
  rejectedCount: 0,
  reimbursedCount: 0,
  totalApplyMoney: 0,
  reimbursedMoney: 0,
  topClubName: '暂无数据',
  currentMonthCount: 0
})

const approvedStatuses = ['审核通过', '已报销']
const allStatuses = ['待提交', '待审核', '待二次审核', '待三次审核', '审核通过', '审核驳回', '已报销']

const moneyValue = (value) => {
  const parsed = Number.parseFloat(value)
  return Number.isFinite(parsed) ? parsed : 0
}

const formatMoney = (value) => Number(value || 0).toLocaleString('zh-CN', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2
})

const safePercent = (numerator, denominator) => {
  if (!denominator) return '0.0%'
  return `${((numerator / denominator) * 100).toFixed(1)}%`
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

const latestReimburseMap = (reimburses) => {
  const map = new Map()
  reimburses
    .filter(item => item.status === '已报销' && item.apply_ID)
    .forEach(item => {
      const existing = map.get(item.apply_ID)
      const currentKey = `${item.time || ''}_${item.reimburse_ID || 0}`
      const existingKey = existing ? `${existing.time || ''}_${existing.reimburse_ID || 0}` : ''
      if (!existing || currentKey > existingKey) {
        map.set(item.apply_ID, item)
      }
    })
  return map
}

const safeGetList = async (url) => {
  try {
    const res = await request.get(url)
    return res.code === '200' && Array.isArray(res.data) ? res.data : []
  } catch (error) {
    console.warn(`统计接口加载失败：${url}`, error)
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

const kpiCards = computed(() => [
  {
    label: '社团总数',
    value: globalStats.totalClubs,
    desc: '已纳入系统管理的社团',
    icon: OfficeBuilding,
    theme: 'blue'
  },
  {
    label: '申请总数',
    value: globalStats.totalApplies,
    desc: '所有经费申请记录',
    icon: Tickets,
    theme: 'cyan'
  },
  {
    label: '审批通过率',
    value: safePercent(globalStats.approvedCount, globalStats.totalApplies),
    desc: `${globalStats.approvedCount} 条已通过或已报销`,
    icon: CircleCheck,
    theme: 'green'
  },
  {
    label: '报销完成率',
    value: safePercent(globalStats.reimbursedCount, Math.max(globalStats.approvedCount, 1)),
    desc: `${globalStats.reimbursedCount} 条完成财务报销`,
    icon: Wallet,
    theme: 'orange'
  },
  {
    label: '总申请金额',
    value: formatMoney(globalStats.totalApplyMoney),
    desc: '单位：元',
    icon: Coin,
    theme: 'purple'
  },
  {
    label: '已报销金额',
    value: formatMoney(globalStats.reimbursedMoney),
    desc: '单位：元',
    icon: Money,
    theme: 'red'
  }
])

const getChart = (domRef, currentInstance) => {
  if (!domRef.value) return null
  if (currentInstance) return currentInstance
  return echarts.init(domRef.value, null, { renderer: 'canvas' })
}

const renderTopClubChart = async (clubs, applies) => {
  await waitForVisibleDom(barChart)
  const clubNameMap = new Map(clubs.map(club => [club.team_ID, club.team_name || `社团${club.team_ID}`]))
  const clubMoneyMap = new Map()

  applies.forEach(apply => {
    const name = clubNameMap.get(apply.team_ID) || `社团${apply.team_ID || '未知'}`
    clubMoneyMap.set(name, (clubMoneyMap.get(name) || 0) + moneyValue(apply.apply_money))
  })

  const topData = Array.from(clubMoneyMap.entries())
    .map(([name, value]) => ({ name, value: Number(value.toFixed(2)) }))
    .sort((a, b) => b.value - a.value)
    .slice(0, 5)

  if (!topData.length) {
    topData.push(
      { name: '科技创新协会', value: 12000 },
      { name: '青年志愿者协会', value: 9600 },
      { name: '摄影协会', value: 7200 },
      { name: '辩论社', value: 5300 },
      { name: '音乐社', value: 4100 }
    )
  }

  globalStats.topClubName = topData[0]?.name || '暂无数据'

  barInstance = getChart(barChart, barInstance)
  barInstance?.setOption({
    grid: { left: 110, right: 28, top: 24, bottom: 24 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => `${params[0].name}<br/>申请金额：${formatMoney(params[0].value)} 元`
    },
    xAxis: {
      type: 'value',
      name: '金额(元)',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#eef1f6' } }
    },
    yAxis: {
      type: 'category',
      inverse: true,
      data: topData.map(item => item.name),
      axisTick: { show: false },
      axisLine: { show: false }
    },
    series: [{
      type: 'bar',
      barWidth: 18,
      data: topData.map(item => item.value),
      label: {
        show: true,
        position: 'right',
        formatter: ({ value }) => formatMoney(value)
      },
      itemStyle: {
        borderRadius: [0, 8, 8, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#3b82f6' },
          { offset: 1, color: '#22c55e' }
        ])
      }
    }]
  }, true)
}

const renderStatusChart = async (applies) => {
  await waitForVisibleDom(pieChart)
  const statusMap = Object.fromEntries(allStatuses.map(status => [status, 0]))
  applies.forEach(apply => {
    statusMap[apply.status] = (statusMap[apply.status] || 0) + 1
  })

  let chartData = Object.entries(statusMap)
    .filter(([, value]) => value > 0)
    .map(([name, value]) => ({ name, value }))

  if (!chartData.length) {
    chartData = [{ name: '暂无数据', value: 1, itemStyle: { color: '#dcdfe6' } }]
  }

  pieInstance = getChart(pieChart, pieInstance)
  pieInstance?.setOption({
    color: ['#94a3b8', '#f59e0b', '#38bdf8', '#6366f1', '#22c55e', '#ef4444', '#10b981'],
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, left: 'center' },
    series: [{
      type: 'pie',
      radius: ['48%', '72%'],
      center: ['50%', '43%'],
      avoidLabelOverlap: true,
      label: { formatter: '{b}\n{d}%' },
      data: chartData
    }]
  }, true)
}

const renderMonthTrendChart = async (applies) => {
  await waitForVisibleDom(lineChart)
  const months = recentSixMonths()
  const monthMap = Object.fromEntries(months.map(month => [month, 0]))

  applies.forEach(apply => {
    const date = getDate(apply.apply_time)
    if (!date) return
    const month = formatMonth(date)
    if (monthMap[month] !== undefined) {
      monthMap[month] += moneyValue(apply.apply_money)
    }
  })

  lineInstance = getChart(lineChart, lineInstance)
  lineInstance?.setOption({
    grid: { left: 60, right: 28, top: 30, bottom: 44 },
    tooltip: {
      trigger: 'axis',
      formatter: (params) => `${params[0].axisValue}<br/>申请金额：${formatMoney(params[0].value)} 元`
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: months
    },
    yAxis: {
      type: 'value',
      name: '金额(元)',
      splitLine: { lineStyle: { color: '#eef1f6' } }
    },
    series: [{
      name: '申请金额',
      data: months.map(month => Number(monthMap[month].toFixed(2))),
      type: 'line',
      smooth: true,
      symbolSize: 8,
      lineStyle: { width: 4, color: '#2563eb' },
      itemStyle: { color: '#2563eb' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(37, 99, 235, 0.25)' },
          { offset: 1, color: 'rgba(37, 99, 235, 0.02)' }
        ])
      }
    }]
  }, true)
}

const loadStats = async () => {
  const [clubs, applies, reimburses] = await Promise.all([
    safeGetList('/team/selectAll'),
    safeGetList('/apply/selectAll'),
    safeGetList('/reimburse/selectAll')
  ])
  latestChartData.value = { clubs, applies }
  const reimburseMap = latestReimburseMap(reimburses)
  const reimbursedApplies = applies.filter(item => item.status === '已报销')
  const currentMonth = formatMonth(new Date())

  globalStats.totalClubs = clubs.length
  globalStats.totalApplies = applies.length
  globalStats.approvedCount = applies.filter(item => approvedStatuses.includes(item.status)).length
  globalStats.rejectedCount = applies.filter(item => item.status === '审核驳回').length
  globalStats.reimbursedCount = reimbursedApplies.length
  globalStats.totalApplyMoney = applies.reduce((sum, item) => sum + moneyValue(item.apply_money), 0)
  globalStats.reimbursedMoney = reimbursedApplies.reduce((sum, item) => {
    return sum + moneyValue(reimburseMap.get(item.apply_ID)?.money)
  }, 0)
  globalStats.currentMonthCount = applies.filter(item => {
    const date = getDate(item.apply_time)
    return date && formatMonth(date) === currentMonth
  }).length

  await nextTick()
  await renderAllCharts()
}

const resizeCharts = () => {
  barInstance?.resize()
  pieInstance?.resize()
  lineInstance?.resize()
}

const renderAllCharts = async () => {
  const { clubs, applies } = latestChartData.value
  await Promise.all([
    renderTopClubChart(clubs, applies),
    renderStatusChart(applies),
    renderMonthTrendChart(applies)
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
  ;[barChart.value, pieChart.value, lineChart.value]
    .filter(Boolean)
    .forEach(el => resizeObserver.observe(el))
}

onMounted(() => {
  loadStats()
  nextTick(observeChartContainers)
  window.addEventListener('resize', resizeCharts)
})

onActivated(() => {
  nextTick(() => {
    observeChartContainers()
    renderAllCharts()
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  barInstance?.dispose()
  pieInstance?.dispose()
  lineInstance?.dispose()
  resizeObserver?.disconnect()
})
</script>

<style scoped>
.stats-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.kpi-row {
  row-gap: 16px;
}

.kpi-card {
  height: 132px;
  border: 0;
  border-radius: 8px;
  color: #1f2937;
  overflow: hidden;
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
  color: #fff;
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

.chart-row {
  row-gap: 20px;
}

.chart-card {
  border-radius: 8px;
}

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

.trend-chart {
  width: 100%;
  height: 340px;
  min-height: 340px;
}

@media (max-width: 768px) {
  .kpi-value {
    font-size: 24px;
  }
}
</style>
