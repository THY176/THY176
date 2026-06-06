<template>
  <div>
    <el-card>
      <div style="margin-bottom: 15px">
        <el-input
            v-model="searchForm.team_name"
            placeholder="社团名称"
            clearable
            style="width: 200px; margin-right: 10px"
            @clear="handleClear"
            @keyup.enter="load"
        />
        <el-button type="success" @click="openAddDialog">新增社团</el-button>
      </div>

      <CommonTable :data="tableData" :columns="columns" :total="total" v-model:page-num="pageNum" v-model:page-size="pageSize" @page-change="load">
        <template #action="{ row }">
          <el-button type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
          <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </CommonTable>
    </el-card>

    <CommonDialog :title="dialogTitle" v-model:visible="dialogVisible" :form-data="formData" :rules="rules" @submit="handleSubmit">
      <template #form-items>
        <el-form-item label="社团名称" prop="team_name">
          <el-input v-model="formData.team_name" />
        </el-form-item>
        <el-form-item label="指导老师" prop="teacher_ID">
          <el-select v-model="formData.teacher_ID" placeholder="请选择指导老师">
            <el-option v-for="t in teacherList" :key="t.teacher_ID" :label="t.name" :value="t.teacher_ID" />
          </el-select>
        </el-form-item>
        <el-form-item label="初始密码" prop="password" v-if="isAdd">
          <el-input v-model="formData.password" placeholder="社团登录密码" />
        </el-form-item>
        <el-form-item label="成立时间" prop="time">
          <el-date-picker v-model="formData.time" type="datetime" placeholder="选择日期时间" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
      </template>
    </CommonDialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed, watch } from 'vue'
import request from '@/utils/request.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import CommonTable from '@/components/CommonTable.vue'
import CommonDialog from '@/components/CommonDialog.vue'

const searchForm = reactive({ team_name: '' })
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const teacherList = ref([])

const columns = [
  { prop: 'team_ID', label: '社团编号', width: 100 },
  { prop: 'team_name', label: '社团名称', width: 150 },
  { prop: 'teacher_name', label: '指导老师', width: 120 },
  { prop: 'number', label: '人数', width: 80 },
  { prop: 'time', label: '成立时间', width: 160 }
]

const dialogVisible = ref(false)
const isAdd = ref(true)
const dialogTitle = computed(() => isAdd.value ? '新增社团' : '编辑社团')

const formData = reactive({
  team_ID: null,
  team_name: '',
  teacher_ID: null,
  password: '',
  number: 0,
  time: ''
})

const rules = {
  team_name: [{ required: true, message: '请输入社团名称', trigger: 'blur' }],
  teacher_ID: [{ required: true, message: '请选择指导老师', trigger: 'change' }],
  password: [{ required: true, message: '请输入初始密码', trigger: 'blur' }],
  time: [{ required: true, message: '请选择成立时间', trigger: 'change' }]
}

const loadTeachers = async () => {
  const res = await request.get('/teacher/selectAll')
  if (res.code === '200') {
    teacherList.value = res.data || []
  }
}

const load = async () => {
  const res = await request.get('/team/selectPage', {
    params: {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      team_name: searchForm.team_name || undefined
    }
  })
  if (res.code === '200') {
    const list = res.data.list || []
    for (let item of list) {
      const teacher = teacherList.value.find(t => t.teacher_ID === item.teacher_ID)
      item.teacher_name = teacher?.name || '未知'
    }
    tableData.value = list
    total.value = res.data.total
  }
}

// 清空搜索框时重新加载全部数据
const handleClear = () => {
  searchForm.team_name = ''
  pageNum.value = 1
  load()
}

// 监听搜索条件变化，自动查询（防抖）
let debounceTimer = null
watch(
    () => searchForm.team_name,
    () => {
      if (debounceTimer) clearTimeout(debounceTimer)
      debounceTimer = setTimeout(() => {
        pageNum.value = 1
        load()
      }, 300)
    }
)

const openAddDialog = () => {
  isAdd.value = true
  Object.assign(formData, {
    team_ID: null,
    team_name: '',
    teacher_ID: null,
    password: '',
    number: 0,
    time: ''
  })
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isAdd.value = false
  Object.assign(formData, { ...row })
  dialogVisible.value = true
}

const handleSubmit = () => {
  if (isAdd.value) {
    // 新增：使用 POST
    request.post('/team/add', formData).then(res => {
      if (res.code === '200') {
        ElMessage.success('新增成功')
        dialogVisible.value = false
        load()
      } else {
        ElMessage.error(res.msg || '新增失败')
      }
    })
  } else {
    // 编辑：使用 PUT
    const updateData = { ...formData }
    // 如果密码为空，删除密码字段（不修改密码）
    if (!updateData.password || updateData.password === '') {
      delete updateData.password
    }

    request.put('/team/update', updateData).then(res => {
      if (res.code === '200') {
        ElMessage.success('修改成功')
        dialogVisible.value = false
        load()
      } else {
        ElMessage.error(res.msg || '修改失败')
      }
    })
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除社团"${row.team_name}"吗？`, '确认删除', { type: 'warning' })
      .then(() => {
        request.delete(`/team/delByteam_ID/${row.team_ID}`).then(res => {
          if (res.code === '200') {
            ElMessage.success('删除成功')
            load()
          }
        })
      })
}

loadTeachers().then(() => load())
</script>

<style scoped>
</style>