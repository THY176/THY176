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
import { reactive, ref, onMounted } from 'vue'
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

const loadStats = async () => {
  // 获取所有社团
  const clubRes = await request.get('/team/selectAll')
  const clubs = clubRes.code === '200' ? (clubRes.data || []) : []
  globalStats.totalClubs = clubs.length

  // 获取所有申请
  const applyRes = await request.get('/apply/selectAll')
  const applies = applyRes.code === '200' ? (applyRes.data || []) : []
  globalStats.totalApplies = applies.length

  // 统计通过数和通过金额（状态为"审核通过"或"已报销"）
  let approvedCount = 0
  let approvedMoney = 0
  applies.forEach(a => {
    if (a.status === '审核通过' || a.status === '已报销') {
      approvedCount++
      approvedMoney += parseFloat(a.apply_money || 0)
    }
  })
  globalStats.approvedCount = approvedCount
  globalStats.approvedMoney = approvedMoney.toFixed(2)

  // 获取所有报销记录，计算已报销金额
  const reRes = await request.get('/reimburse/selectAll')
  const reimburses = reRes.code === '200' ? (reRes.data || []) : []
  let reimbursedMoney = 0
  reimburses.forEach(r => {
    if (r.status === '已报销') {
      reimbursedMoney += parseFloat(r.money || 0)
    }
  })
  globalStats.reimbursedMoney = reimbursedMoney.toFixed(2)

  // 柱状图：各社团申请金额对比（只统计审核通过和已报销的金额）
  const clubMoneyMap = {}
  clubs.forEach(c => clubMoneyMap[c.team_name] = 0)
  applies.forEach(a => {
    if (a.status === '审核通过' || a.status === '已报销') {
      const club = clubs.find(c => c.team_ID === a.team_ID)
      if (club) clubMoneyMap[club.team_name] += parseFloat(a.apply_money || 0)
    }
  })

  if (barChart.value) {
    echarts.init(barChart.value).setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: Object.keys(clubMoneyMap), axisLabel: { rotate: 45, interval: 0 } },
      yAxis: { type: 'value', name: '金额(元)' },
      series: [{ data: Object.values(clubMoneyMap), type: 'bar', itemStyle: { color: '#409eff' } }]
    })
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
    })
  }

  // 折线图：月度趋势（只统计审核通过和已报销的金额）
  const monthMap = {}
  applies.forEach(a => {
    if ((a.status === '审核通过' || a.status === '已报销') && a.apply_time) {
      const month = a.apply_time.slice(0, 7)
      monthMap[month] = (monthMap[month] || 0) + parseFloat(a.apply_money || 0)
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
    })
  }
}

onMounted(() => {
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