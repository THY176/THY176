<<template>
  <div>
    <el-card>
      <template #header>我的审核历史</template>

      <el-select v-model="filterStatus" placeholder="结果筛选" clearable style="width: 150px; margin-bottom: 15px" @change="load">
        <el-option label="审核通过" value="审核通过" />
        <el-option label="审核驳回" value="审核驳回" />
      </el-select>

      <CommonTable :data="tableData" :columns="columns" :total="total" v-model:page-num="pageNum" v-model:page-size="pageSize" @page-change="load">
        <template #status="{ row }">
          <el-tag :type="row.status === '审核通过' ? 'success' : 'danger'">{{ row.status }}</el-tag>
        </template>
      </CommonTable>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'  // 添加 onMounted
import { useUserStore } from '@/stores/user.js'
import request from '@/utils/request.js'
import CommonTable from '@/components/CommonTable.vue'

const userStore = useUserStore()

const filterStatus = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])

const columns = [
  { prop: 'approve_ID', label: '审核编号', width: 100 },
  { prop: 'apply_ID', label: '申请编号', width: 100 },
  { prop: 'apply_money', label: '申请金额', width: 120 },
  { prop: 'apply_type', label: '类型', width: 100 },
  { prop: 'opinion', label: '审核意见', width: 200 },
  { prop: 'approve_time', label: '审核时间', width: 160 },
  { prop: 'status', label: '结果', width: 100, slot: true }
]

const load = async () => {
  // 检查用户是否登录
  if (!userStore.userId) {
    console.log('用户未登录')
    return
  }

  const res = await request.get('/approve/selectByTeacher_ID/' + userStore.userId)
  if (res.code !== '200') return

  let list = res.data || []
  if (filterStatus.value) {
    list = list.filter(item => item.status === filterStatus.value)
  }

  // 补充申请信息
  for (let item of list) {
    const applyRes = await request.get(`/apply/selectByapply_ID/${item.apply_ID}`)
    if (applyRes.code === '200') {
      item.apply_money = applyRes.data?.apply_money
      item.apply_type = applyRes.data?.apply_type
    }
  }

  tableData.value = list
  total.value = list.length
}

onMounted(() => {
  load()
})
</script>