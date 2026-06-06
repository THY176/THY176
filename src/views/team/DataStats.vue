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
import { reactive, ref, onMounted } from 'vue'
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

// 定义审核通过的状态（包含所有已提交且未被驳回的状态）
const approvedStatuses = ['待审核', '待二次审核', '待三次审核', '审核通过', '已报销']

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
    const applyIds = list.map(a => a.apply_ID)

    // 总申请次数：所有申请
    stats.totalApply = list.length

    // 总申请金额：只统计审核通过的申请（排除审核驳回和待提交）
    const approvedList = list.filter(item => approvedStatuses.includes(item.status))
    stats.totalMoney = approvedList.reduce((sum, item) => sum + parseFloat(item.apply_money || 0), 0).toFixed(2)

    // 获取报销记录 - 从申请表中直接统计已报销状态的申请金额
    // 这样删除报销记录时，申请状态还是"已报销"，金额不会丢失
    // 如果你希望删除报销记录后金额减少，需要同时将申请状态改回"审核通过"

    // 方案1：从申请表中统计已报销金额（推荐）
    const reimbursedList = list.filter(item => item.status === '已报销')
    stats.reimbursedMoney = reimbursedList.reduce((sum, item) => sum + parseFloat(item.apply_money || 0), 0).toFixed(2)

    // 方案2：从报销表中统计（如果报销记录被删除，金额会减少）
    // const reimburseRes = await request.get('/reimburse/selectAll')
    // const relatedReimburses = (reimburseRes.data || []).filter(r => applyIds.includes(r.apply_ID) && r.status === '已报销')
    // stats.reimbursedMoney = relatedReimburses.reduce((sum, item) => sum + parseFloat(item.money || 0), 0).toFixed(2)

    // 经费类型分布（饼图）- 只统计审核通过的申请
    const typeMap = {}
    approvedList.forEach(item => {
      if (item.apply_type) {
        typeMap[item.apply_type] = (typeMap[item.apply_type] || 0) + parseFloat(item.apply_money || 0)
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
        monthMap[month] = (monthMap[month] || 0) + parseFloat(item.apply_money || 0)
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
      echarts.init(pieChart.value).setOption(pieOption)
    }
    if (lineChart.value) {
      echarts.init(lineChart.value).setOption(lineOption)
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