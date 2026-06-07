<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card>
          <div class="stat-item">
            <div class="stat-value">{{ globalStats.totalClubs }}</div>
            <div class="stat-label">社团总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-item">
            <div class="stat-value" style="color: #409eff">
              {{ globalStats.approvedCount }}/{{ globalStats.totalApplies }}
            </div>
            <div class="stat-label">通过数/总申请数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-item">
            <div class="stat-value" style="color: #67c23a">{{ globalStats.approvedMoney }}</div>
            <div class="stat-label">总申请金额（元）</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-item">
            <div class="stat-value" style="color: #e6a23c">{{ globalStats.reimbursedMoney }}</div>
            <div class="stat-label">已报销金额（元）</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>各社团经费申请对比</template>
          <div ref="barChart" style="height: 400px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>审核状态分布</template>
          <div ref="pieChart" style="height: 400px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card>
          <template #header>月度经费趋势（所有社团）</template>
          <div ref="lineChart" style="height: 350px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { reactive, ref, onActivated, onMounted } from 'vue'
import request from '@/utils/request.js'
import * as echarts from 'echarts'

const barChart = ref(null)
const pieChart = ref(null)
const lineChart = ref(null)

const globalStats = reactive({
  totalClubs: 0,
  totalApplies: 0,
  approvedCount: 0,        // 通过数（审核通过 + 已报销）
  approvedMoney: 0,        // 总申请金额（只统计审核通过和已报销）
  reimbursedMoney: 0       // 已报销金额
})

const approvedStatuses = ['审核通过', '已报销']
const moneyValue = (value) => parseFloat(value || 0)

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

const loadStats = async () => {
  // 获取所有社团
  const clubRes = await request.get('/team/selectAll')
  const clubs = clubRes.code === '200' ? (clubRes.data || []) : []
  globalStats.totalClubs = clubs.length

  // 获取所有申请
  const applyRes = await request.get('/apply/selectAll')
  const applies = applyRes.code === '200' ? (applyRes.data || []) : []
  globalStats.totalApplies = applies.length

  const approvedApplies = applies.filter(a => approvedStatuses.includes(a.status))
  const reimbursedApplies = applies.filter(a => a.status === '已报销')

  globalStats.approvedCount = approvedApplies.length
  globalStats.approvedMoney = approvedApplies
      .reduce((sum, item) => sum + moneyValue(item.apply_money), 0)
      .toFixed(2)

  // 只统计当前仍为已报销申请的最新报销记录，避免旧流程报销记录继续累加。
  const reRes = await request.get('/reimburse/selectAll')
  const reimburses = reRes.code === '200' ? (reRes.data || []) : []
  const reimburseMap = latestReimburseMap(reimburses)
  globalStats.reimbursedMoney = reimbursedApplies
      .reduce((sum, item) => sum + moneyValue(reimburseMap.get(item.apply_ID)?.money), 0)
      .toFixed(2)

  // 柱状图：各社团申请金额对比（只统计审核通过和已报销的金额）
  const clubMoneyMap = {}
  clubs.forEach(c => clubMoneyMap[c.team_name] = 0)
  approvedApplies.forEach(a => {
    const club = clubs.find(c => c.team_ID === a.team_ID)
    if (club) clubMoneyMap[club.team_name] += moneyValue(a.apply_money)
  })

  if (barChart.value) {
    echarts.init(barChart.value).setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: Object.keys(clubMoneyMap), axisLabel: { rotate: 45, interval: 0 } },
      yAxis: { type: 'value', name: '金额(元)' },
      series: [{ data: Object.values(clubMoneyMap), type: 'bar', itemStyle: { color: '#409eff' } }]
    }, true)
  }

  // 饼图：审核状态分布
  const statusMap = {}
  applies.forEach(a => statusMap[a.status] = (statusMap[a.status] || 0) + 1)

  if (pieChart.value) {
    echarts.init(pieChart.value).setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { orient: 'vertical', left: 'left' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        data: Object.entries(statusMap).map(([name, value]) => ({ name, value }))
      }]
    }, true)
  }

  // 折线图：月度趋势（只统计审核通过和已报销的金额）
  const monthMap = {}
  approvedApplies.forEach(a => {
    if (a.apply_time) {
      const month = a.apply_time.slice(0, 7)
      monthMap[month] = (monthMap[month] || 0) + moneyValue(a.apply_money)
    }
  })
  const sortedMonths = Object.keys(monthMap).sort()

  if (lineChart.value) {
    echarts.init(lineChart.value).setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: sortedMonths },
      yAxis: { type: 'value', name: '金额(元)' },
      series: [{
        data: sortedMonths.map(m => monthMap[m]),
        type: 'line',
      smooth: true,
      areaStyle: { color: 'rgba(103, 194, 58, 0.2)' },
      itemStyle: { color: '#67c23a' }
    }]
    }, true)
  }
}

onMounted(() => {
  loadStats()
})

onActivated(() => {
  loadStats()
})
</script>

<style scoped>
.stat-item {
  text-align: center;
  padding: 10px;
}
.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
}
.stat-label {
  font-size: 14px;
  color: #909399;
}
</style>
