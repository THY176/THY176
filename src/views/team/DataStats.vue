<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card>
          <div class="stat-item">
            <div class="stat-value">{{ stats.totalApply }}</div>
            <div class="stat-label">总申请次数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <div class="stat-item">
            <div class="stat-value" style="color: #67c23a">{{ stats.totalMoney }}</div>
            <div class="stat-label">总申请金额（元）</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <div class="stat-item">
            <div class="stat-value" style="color: #409eff">{{ stats.reimbursedMoney }}</div>
            <div class="stat-label">已报销金额（元）</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>经费类型分布</template>
          <div ref="pieChart" style="height: 350px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>月度申请趋势</template>
          <div ref="lineChart" style="height: 350px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { reactive, ref, onActivated, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user.js'
import request from '@/utils/request.js'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

const userStore = useUserStore()
const router = useRouter()
const pieChart = ref(null)
const lineChart = ref(null)

const stats = reactive({
  totalApply: 0,
  totalMoney: 0,
  reimbursedMoney: 0
})

const effectiveStatuses = ['待审核', '待二次审核', '待三次审核', '审核通过', '已报销']
const moneyValue = (value) => parseFloat(value || 0)

const latestReimburse = (reimburses) => {
  return (reimburses || [])
      .filter(item => item.status === '已报销')
      .sort((a, b) => {
        const timeCompare = (b.time || '').localeCompare(a.time || '')
        if (timeCompare !== 0) return timeCompare
        return (b.reimburse_ID || 0) - (a.reimburse_ID || 0)
      })[0] || null
}

const loadStats = async () => {
  // 检查登录状态
  if (!userStore.userId) {
    ElMessage.error('未获取到社团信息，请重新登录')
    router.push('/')
    return
  }

  try {
    // 获取所有申请
    const res = await request.get('/apply/selectAll', { params: { team_ID: userStore.userId } })
    if (res.code !== '200') return

    const list = res.data || []
    // 总申请次数：所有申请
    stats.totalApply = list.length

    // 总申请金额：只统计当前仍在有效流程或已完成的申请，排除待提交和审核驳回。
    const approvedList = list.filter(item => effectiveStatuses.includes(item.status))
    stats.totalMoney = approvedList.reduce((sum, item) => sum + moneyValue(item.apply_money), 0).toFixed(2)

    // 已报销金额：只统计当前仍为已报销申请的最新实际报销记录，避免旧记录继续累加。
    const reimbursedList = list.filter(item => item.status === '已报销')
    const reimburseRecords = await Promise.all(
        reimbursedList.map(async item => {
          const reimburseRes = await request.get(`/reimburse/selectByApply_ID/${item.apply_ID}`)
          return reimburseRes.code === '200' ? latestReimburse(reimburseRes.data || []) : null
        })
    )
    stats.reimbursedMoney = reimburseRecords
        .reduce((sum, item) => sum + moneyValue(item?.money), 0)
        .toFixed(2)

    // 经费类型分布（饼图）- 只统计审核通过的申请
    const typeMap = {}
    approvedList.forEach(item => {
      if (item.apply_type) {
        typeMap[item.apply_type] = (typeMap[item.apply_type] || 0) + moneyValue(item.apply_money)
      }
    })

    const pieOption = {
      tooltip: { trigger: 'item', formatter: '{b}: {c}元 ({d}%)' },
      legend: { orient: 'vertical', left: 'left' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        data: Object.entries(typeMap).map(([name, value]) => ({ name, value }))
      }]
    }

    // 月度申请趋势（折线图）- 只统计审核通过的申请
    const monthMap = {}
    approvedList.forEach(item => {
      if (item.apply_time) {
        const month = item.apply_time.slice(0, 7)
        monthMap[month] = (monthMap[month] || 0) + moneyValue(item.apply_money)
      }
    })
    const sortedMonths = Object.keys(monthMap).sort()

    const lineOption = {
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: sortedMonths },
      yAxis: { type: 'value', name: '金额(元)' },
      series: [{
        data: sortedMonths.map(m => monthMap[m]),
        type: 'line',
        smooth: true,
        areaStyle: { color: 'rgba(64, 158, 255, 0.2)' },
        itemStyle: { color: '#409eff' }
      }]
    }

    // 渲染图表
    if (pieChart.value) {
      echarts.init(pieChart.value).setOption(pieOption, true)
    }
    if (lineChart.value) {
      echarts.init(lineChart.value).setOption(lineOption, true)
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
    ElMessage.error('加载统计数据失败')
  }
}

// 监听成员删除事件，刷新统计数据
const handleDataRefresh = () => {
  console.log('收到数据刷新事件，重新加载统计数据')
  loadStats()
}

onMounted(() => {
  loadStats()
  // 监听自定义事件
  window.addEventListener('member-deleted', handleDataRefresh)
  window.addEventListener('apply-updated', handleDataRefresh)
  window.addEventListener('reimburse-changed', handleDataRefresh)
})

onActivated(() => {
  loadStats()
})

// 组件卸载时移除事件监听
import { onUnmounted } from 'vue'
onUnmounted(() => {
  window.removeEventListener('member-deleted', handleDataRefresh)
  window.removeEventListener('apply-updated', handleDataRefresh)
  window.removeEventListener('reimburse-changed', handleDataRefresh)
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
