<template>
  <div>
    <el-card>
      <div style="margin-bottom: 15px">
        <el-input
            v-model="searchForm.teacher_ID"
            placeholder="工号"
            clearable
            style="width: 150px; margin-right: 10px"
            @clear="handleSearch"
            @keyup.enter="handleSearch"
        />
        <el-input
            v-model="searchForm.name"
            placeholder="管理员姓名"
            clearable
            style="width: 150px; margin-right: 10px"
            @clear="handleSearch"
            @keyup.enter="handleSearch"
        />
        <el-button type="success" @click="openAddDialog">新增管理员</el-button>
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
        <el-form-item label="工号" prop="teacher_ID" v-if="isAdd">
          <el-input v-model="formData.teacher_ID" placeholder="管理员账号工号" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="formData.name" />
        </el-form-item>
        <el-form-item label="初始密码" prop="password" v-if="isAdd">
          <el-input v-model="formData.password" placeholder="登录密码" />
        </el-form-item>
        <el-form-item label="电话" prop="tele">
          <el-input v-model="formData.tele" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="formData.gender">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="formData.age" :min="20" :max="80" />
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

const searchForm = reactive({ teacher_ID: '', name: '' })
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])

const columns = [
  { prop: 'teacher_ID', label: '工号', width: 100 },
  { prop: 'name', label: '姓名', width: 120 },
  { prop: 'tele', label: '电话', width: 150 },
  { prop: 'gender', label: '性别', width: 80 },
  { prop: 'age', label: '年龄', width: 80 }
]

const dialogVisible = ref(false)
const isAdd = ref(true)
const dialogTitle = computed(() => isAdd.value ? '新增管理员' : '编辑管理员')

const formData = reactive({
  teacher_ID: null,
  name: '',
  password: '',
  tele: '',
  gender: '男',
  age: 30
})

const rules = {
  teacher_ID: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  tele: [{ required: true, message: '请输入电话', trigger: 'blur' }]
}

const load = () => {
  // 构建参数，只传递非空值
  const params = {
    pageNum: pageNum.value,
    pageSize: pageSize.value
  }

  if (searchForm.teacher_ID && searchForm.teacher_ID.trim()) {
    params.teacher_ID = searchForm.teacher_ID.trim()
  }
  if (searchForm.name && searchForm.name.trim()) {
    params.name = searchForm.name.trim()
  }

  request.get('/admin/selectPage', { params }).then(res => {
    if (res.code === '200') {
      tableData.value = res.data.list || []
      total.value = res.data.total || 0
    }
  })
}

// 查询方法（重置页码后加载）
const handleSearch = () => {
  pageNum.value = 1
  load()
}

// 监听搜索条件变化，自动查询（防抖）
let debounceTimer = null
watch(
    () => [searchForm.teacher_ID, searchForm.name],
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
  Object.assign(formData, { teacher_ID: null, name: '', password: '', tele: '', gender: '男', age: 30 })
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
    request.post('/admin/add', formData).then(res => {
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
    // 编辑时不需要传递密码（如果不修改密码）
    const updateData = { ...formData }
    if (!updateData.password || updateData.password === '') {
      delete updateData.password
    }

    request.put('/admin/update', updateData).then(res => {
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
  ElMessageBox.confirm(`确定删除管理员"${row.name}"吗？`, '确认删除', { type: 'warning' })
      .then(() => {
        request.delete(`/admin/delByteacher_ID/${row.teacher_ID}`).then(res => {
          if (res.code === '200') {
            ElMessage.success('删除成功')
            load()
          }
        })
      })
}

// 初始加载
load()
</script>