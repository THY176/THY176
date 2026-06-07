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
          <el-button type="warning" size="small" @click="forceStatus(row)">强制改状态</el-button>
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
              :type="item.status === '审核通过' ? 'success' : 'danger'"
          >
            <p><strong>第{{ item.sequence }}级审核（{{ item.role }}）</strong></p>
            <p>审核人：{{ item.teacher_name || item.teacher_ID || '待定' }} | 时间：{{ item.approve_time }}</p>
            <p>意见：{{ item.opinion }}</p>
            <p>结果：{{ item.status }}</p>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-dialog>

    <!-- 强制改状态 -->
    <el-dialog title="强制修改状态" v-model="statusVisible" width="400">
      <el-form v-if="currentApply">
        <el-form-item label="当前状态">
          <el-tag>{{ currentApply.status }}</el-tag>
        </el-form-item>
        <el-form-item label="新状态">
          <el-select v-model="newStatus">
            <el-option v-for="s in statusOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusVisible = false">取消</el-button>
        <el-button type="warning" @click="confirmStatusChange">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import request from '@/utils/request.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import CommonTable from '@/components/CommonTable.vue'

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

const statusOptions = ['待提交', '待审核', '审核驳回', '待二次审核', '待三次审核', '审核通过', '已报销']

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
const newStatus = ref('')

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

const viewDetail = async (row) => {
  currentApply.value = row
  detailVisible.value = true

  // 待提交和待审核状态不显示任何审核流程
  if (row.status === '待提交' || row.status === '待审核') {
    auditList.value = []
    return
  }

  const res = await request.get(`/approve/selectByApply_ID/${row.apply_ID}`)
  if (res.code === '200') {
    let allAudits = res.data || []

    // 1. 按 sequence 分组，每组只取最新的（approve_time 最大的）
    const latestAuditsMap = new Map()

    allAudits.forEach(audit => {
      if (!audit.approve_time) return
      const sequence = audit.sequence
      const existing = latestAuditsMap.get(sequence)
      if (!existing || audit.approve_time > existing.approve_time) {
        latestAuditsMap.set(sequence, audit)
      }
    })

    // 2. 转换为数组并按 sequence 排序
    let latestAudits = Array.from(latestAuditsMap.values())
    latestAudits.sort((a, b) => a.sequence - b.sequence)

    // 3. 根据当前状态过滤显示
    let filteredAudits = []
    let maxSequence = 0

    switch (row.status) {
      case '待二次审核':
        maxSequence = 1
        break
      case '待三次审核':
        maxSequence = 2
        break
      case '审核通过':
      case '已报销':
      case '审核驳回':
        maxSequence = 99
        break
      default:
        maxSequence = 99
    }

    // 4. 按顺序添加，如果遇到不通过的审核则停止
    for (let i = 0; i < latestAudits.length && latestAudits[i].sequence <= maxSequence; i++) {
      const audit = latestAudits[i]
      filteredAudits.push(audit)
      if (audit.status !== '审核通过') {
        break
      }
    }

    auditList.value = filteredAudits
  }
}

const forceStatus = (row) => {
  currentApply.value = row
  newStatus.value = row.status
  statusVisible.value = true
}

const confirmStatusChange = () => {
  request.post('/apply/forceStatus', null, {
    params: {
      applyId: currentApply.value.apply_ID,
      status: newStatus.value
    }
  }).then(res => {
    if (res.code === '200') {
      ElMessage.success('状态修改成功')
      statusVisible.value = false
      load()
    } else {
      ElMessage.error(res.msg || '修改失败')
    }
  }).catch(err => {
    console.error('修改失败:', err)
    ElMessage.error(err.message || '修改失败')
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
