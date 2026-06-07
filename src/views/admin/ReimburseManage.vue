<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>经费报销管理</span>
          <el-tag type="success">对审核通过的申请执行报销</el-tag>
        </div>
      </template>

      <div style="margin-bottom: 15px">
        <el-select v-model="searchForm.status" placeholder="状态筛选" clearable style="width: 150px; margin-right: 10px" @change="handleSearch">
          <el-option label="审核通过" value="审核通过" />
          <el-option label="已报销" value="已报销" />
        </el-select>
      </div>

      <CommonTable :data="tableData" :columns="columns" :total="total" v-model:page-num="pageNum" v-model:page-size="pageSize" @page-change="load">
        <template #status="{ row }">
          <el-tag :type="row.status === '审核通过' ? 'success' : 'info'">{{ row.status }}</el-tag>
        </template>
        <template #action="{ row }">
          <el-button v-if="row.status === '审核通过'" type="success" size="small" @click="openReimburseDialog(row)">执行报销</el-button>
          <el-button type="info" size="small" @click="viewDetail(row)">详情</el-button>
        </template>
      </CommonTable>
    </el-card>

    <!-- 报销弹窗 -->
    <CommonDialog title="经费报销执行" v-model:visible="reimburseVisible" :form-data="reimburseForm" :rules="reimburseRules" @submit="handleReimburse">
      <template #form-items>
        <el-form-item label="申请编号">
          <el-input :model-value="currentApply?.apply_ID" disabled />
        </el-form-item>
        <el-form-item label="申请社团">
          <el-input :model-value="currentApply?.team_name" disabled />
        </el-form-item>
        <el-form-item label="申请金额">
          <el-input :model-value="currentApply?.apply_money + ' 元'" disabled />
        </el-form-item>
        <el-form-item label="收款账户">
          <el-input :model-value="currentApply?.account_name + ' / ' + currentApply?.bank + ' / ' + currentApply?.account" disabled />
        </el-form-item>
        <el-form-item label="实际报销金额" prop="money">
          <el-input v-model="reimburseForm.money" placeholder="请输入实际报销金额" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="reimburseForm.remark" type="textarea" :rows="2" placeholder="可选填" />
        </el-form-item>
      </template>
    </CommonDialog>

    <!-- 详情弹窗 -->
    <el-dialog title="报销详情" v-model="detailVisible" width="600">
      <el-descriptions :column="2" border v-if="currentApply">
        <el-descriptions-item label="申请编号">{{ currentApply.apply_ID }}</el-descriptions-item>
        <el-descriptions-item label="社团">{{ currentApply.team_name }}</el-descriptions-item>
        <el-descriptions-item label="申请金额">{{ currentApply.apply_money }} 元</el-descriptions-item>
        <el-descriptions-item label="当前状态">{{ currentApply.status }}</el-descriptions-item>
        <el-descriptions-item label="收款账户">{{ currentApply.account_name }}</el-descriptions-item>
        <el-descriptions-item label="银行">{{ currentApply.bank }}</el-descriptions-item>
        <el-descriptions-item label="账号">{{ currentApply.account }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="reimburseInfo" style="margin-top: 20px">
        <h4>报销记录</h4>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="报销金额">{{ reimburseInfo.money }} 元</el-descriptions-item>
          <el-descriptions-item label="报销时间">{{ reimburseInfo.time }}</el-descriptions-item>
          <el-descriptions-item label="处理人ID">{{ reimburseInfo.teacher_ID }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ reimburseInfo.status }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, watch } from 'vue'
import { useUserStore } from '@/stores/user.js'
import request from '@/utils/request.js'
import { ElMessage } from 'element-plus'
import CommonTable from '@/components/CommonTable.vue'
import CommonDialog from '@/components/CommonDialog.vue'

const userStore = useUserStore()

const searchForm = reactive({ status: '' })
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const clubMap = ref(new Map())  // 使用 Map 存储社团ID到名称的映射

const columns = [
  { prop: 'apply_ID', label: '申请编号', width: 100 },
  { prop: 'team_name', label: '社团', width: 150 },
  { prop: 'apply_money', label: '申请金额(元)', width: 130 },
  { prop: 'apply_type', label: '类型', width: 100 },
  { prop: 'status', label: '状态', width: 100, slot: true },
  { prop: 'apply_time', label: '申请时间', width: 160 }
]

const reimburseVisible = ref(false)
const detailVisible = ref(false)
const currentApply = ref(null)
const reimburseInfo = ref(null)

const reimburseForm = reactive({
  money: '',
  remark: ''
})

const reimburseRules = {
  money: [{ required: true, message: '请输入报销金额', trigger: 'blur' }]
}

// 加载社团列表并建立映射
const loadClubs = async () => {
  const res = await request.get('/team/selectAll')
  if (res.code === '200') {
    const clubs = res.data || []
    clubMap.value.clear()
    clubs.forEach(club => {
      clubMap.value.set(club.team_ID, club.team_name)
    })
    console.log('社团映射表:', clubMap.value)
  }
}

// 获取社团名称
const getTeamName = (teamId) => {
  return clubMap.value.get(teamId) || '未知社团'
}

const loadApprovedList = async () => {
  const res = await request.get('/workflow/reimburse/tasks')
  if (res.code !== '200') return []

  return (res.data || []).filter(task => task.apply).map(task => ({
    ...task.apply,
    taskId: task.taskId,
    taskName: task.taskName,
    taskDefinitionKey: task.taskDefinitionKey,
    team_name: task.teamName || task.team_name || getTeamName(task.apply.team_ID)
  }))
}

const loadReimbursedList = async () => {
  const res = await request.get('/apply/selectByStatus/已报销')
  if (res.code !== '200') return []

  const list = res.data || []
  for (let item of list) {
    const rRes = await request.get(`/reimburse/selectByApply_ID/${item.apply_ID}`)
    if (rRes.code === '200' && rRes.data?.length > 0) {
      item.reimburse_money = rRes.data[0].money
      item.reimburse_time = rRes.data[0].time
    }
  }
  return list
}

const load = async () => {
  const status = searchForm.status
  let list = []

  if (status === '审核通过') {
    list = await loadApprovedList()
  } else if (status === '已报销') {
    list = await loadReimbursedList()
  } else {
    const [approvedList, reimbursedList] = await Promise.all([
      loadApprovedList(),
      loadReimbursedList()
    ])
    list = [...approvedList, ...reimbursedList]
  }

  // 补充社团名称
  for (let item of list) {
    item.team_name = getTeamName(item.team_ID)
  }

  tableData.value = list
  total.value = list.length
  console.log('报销管理表格数据:', tableData.value)
}

// 查询方法
const handleSearch = () => {
  pageNum.value = 1
  load()
}

// 监听状态变化自动查询
let debounceTimer = null
watch(
    () => searchForm.status,
    () => {
      if (debounceTimer) clearTimeout(debounceTimer)
      debounceTimer = setTimeout(() => {
        pageNum.value = 1
        load()
      }, 300)
    }
)

const openReimburseDialog = (row) => {
  currentApply.value = row
  reimburseForm.money = row.apply_money
  reimburseForm.remark = ''
  reimburseVisible.value = true
}

const viewDetail = async (row) => {
  currentApply.value = row
  detailVisible.value = true

  const rRes = await request.get(`/reimburse/selectByApply_ID/${row.apply_ID}`)
  reimburseInfo.value = rRes.code === '200' && rRes.data?.length > 0 ? rRes.data[0] : null
}

const handleReimburse = async () => {
  if (!currentApply.value) return

  try {
    const res = await request.post('/workflow/reimburse', null, {
      params: {
        taskId: currentApply.value.taskId,
        applyId: currentApply.value.apply_ID,
        financeId: userStore.userId,
        financeName: userStore.userInfo?.name || '管理员',
        reimburseAmount: reimburseForm.money
      }
    })

    if (res.code === '200') {
      ElMessage.success('报销执行成功')
      reimburseVisible.value = false
      load()
    } else {
      ElMessage.error(res.msg || '报销失败')
    }
  } catch (error) {
    console.error('报销失败:', error)
    ElMessage.error(error.message || '报销失败')
  }
}

onMounted(async () => {
  await loadClubs()
  await load()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
