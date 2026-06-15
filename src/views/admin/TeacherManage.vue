<template>
  <div>
    <el-card>
      <div style="margin-bottom: 15px">
        <el-input
            v-model="searchForm.name"
            placeholder="教师姓名"
            clearable
            style="width: 200px; margin-right: 10px"
            @clear="handleSearch"
            @keyup.enter="handleSearch"
        />
        <el-button type="success" @click="openAddDialog">新增教师</el-button>
      </div>

      <CommonTable :data="tableData" :columns="columns" :total="total" v-model:page-num="pageNum" v-model:page-size="pageSize" @page-change="load">
        <template #action="{ row }">
          <el-button type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
          <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </CommonTable>
    </el-card>

    <CommonDialog :title="dialogTitle" v-model:visible="dialogVisible" :form-data="formData" :rules="rules" :submit-loading="submitting" @submit="handleSubmit">
      <template #form-items>
        <el-form-item label="工号" prop="teacher_ID" v-if="isAdd">
          <el-input v-model="formData.teacher_ID" placeholder="请输入教师工号" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="formData.name" />
        </el-form-item>
        <el-form-item label="所属社团" prop="team_ID">
          <el-select v-model="formData.team_ID" placeholder="请选择社团（可为空）" clearable>
            <el-option v-for="club in clubList" :key="club.team_ID" :label="club.team_name" :value="club.team_ID" />
          </el-select>
        </el-form-item>
        <el-form-item label="初始密码" prop="password" v-if="isAdd">
          <el-input v-model="formData.password" placeholder="登录密码" />
        </el-form-item>
        <el-form-item label="电话" prop="tele">
          <el-input v-model="formData.tele" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="formData.gender">
            <el-radio label="男">男</el-radio>
            <el-radio label="女">女</el-radio>
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

const searchForm = reactive({ name: '' })
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const clubList = ref([])

const columns = [
  { prop: 'teacher_ID', label: '工号', width: 100 },
  { prop: 'name', label: '姓名', width: 120 },
  { prop: 'team_name', label: '负责社团', width: 150 },
  { prop: 'tele', label: '电话', width: 150 },
  { prop: 'gender', label: '性别', width: 80 },
  { prop: 'age', label: '年龄', width: 80 }
]

const dialogVisible = ref(false)
const submitting = ref(false)
const isAdd = ref(true)
const dialogTitle = computed(() => isAdd.value ? '新增教师' : '编辑教师')

const formData = reactive({
  teacher_ID: null,
  name: '',
  team_ID: null,
  password: '',
  tele: '',
  gender: '男',
  age: 30
})

const validateTeacherId = (_rule, value, callback) => {
  const id = Number(value)
  if (!Number.isInteger(id) || id <= 0) {
    callback(new Error('工号必须是正整数'))
    return
  }
  callback()
}

const validatePhone = (_rule, value, callback) => {
  const phone = String(value || '').trim()
  if (!/^\d{1,11}$/.test(phone)) {
    callback(new Error('电话必须是 1 到 11 位数字'))
    return
  }
  callback()
}

const rules = {
  teacher_ID: [
    { required: true, message: '请输入工号', trigger: 'blur' },
    { validator: validateTeacherId, trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  tele: [
    { required: true, message: '请输入电话', trigger: 'blur' },
    { validator: validatePhone, trigger: 'blur' }
  ],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  age: [{ required: true, message: '请输入年龄', trigger: 'change' }]
}

const loadClubs = async () => {
  const res = await request.get('/team/selectAll')
  if (res.code === '200') clubList.value = res.data || []
}

const load = async () => {
  // 构建参数，只传递非空值
  const params = {
    pageNum: pageNum.value,
    pageSize: pageSize.value
  }

  if (searchForm.name && searchForm.name.trim()) {
    params.name = searchForm.name.trim()
  }

  const res = await request.get('/teacher/selectPage', { params })
  if (res.code === '200') {
    const list = res.data.list || []
    for (let item of list) {
      const club = clubList.value.find(c => c.team_ID === item.team_ID)
      item.team_name = club?.team_name || '未分配'
    }
    tableData.value = list
    total.value = res.data.total
  }
}

// 查询方法（重置页码后加载）
const handleSearch = () => {
  pageNum.value = 1
  load()
}

// 监听搜索条件变化，自动查询（防抖）
let debounceTimer = null
watch(
    () => searchForm.name,
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
    teacher_ID: null,
    name: '',
    team_ID: null,
    password: '',
    tele: '',
    gender: '男',
    age: 30
  })
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isAdd.value = false
  Object.assign(formData, { ...row })
  dialogVisible.value = true
}

const handleSubmit = () => {
  submitting.value = true
  if (isAdd.value) {
    // 新增：使用 POST
    const submitData = {
      teacher_ID: Number(formData.teacher_ID),
      name: String(formData.name || '').trim(),
      team_ID: formData.team_ID || null,
      password: String(formData.password || '').trim(),
      tele: String(formData.tele || '').trim(),
      gender: formData.gender,
      age: formData.age
    }
    request.post('/teacher/add', submitData).then(res => {
      if (res.code === '200') {
        ElMessage.success('新增成功')
        dialogVisible.value = false
        load()
      } else {
        ElMessage.error(res.msg || '新增失败')
      }
    }).catch(err => {
      console.error('新增失败:', err)
    }).finally(() => {
      submitting.value = false
    })
  } else {
    // 编辑：使用 PUT
    const updateData = {
      ...formData,
      teacher_ID: Number(formData.teacher_ID),
      name: String(formData.name || '').trim(),
      team_ID: formData.team_ID || null,
      tele: String(formData.tele || '').trim()
    }
    // 如果密码为空，删除密码字段（不修改密码）
    if (!updateData.password || updateData.password === '') {
      delete updateData.password
    } else {
      updateData.password = String(updateData.password).trim()
    }

    request.put('/teacher/update', updateData).then(res => {
      if (res.code === '200') {
        ElMessage.success('修改成功')
        dialogVisible.value = false
        load()
      } else {
        ElMessage.error(res.msg || '修改失败')
      }
    }).catch(err => {
      console.error('修改失败:', err)
    }).finally(() => {
      submitting.value = false
    })
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除教师"${row.name}"吗？`, '确认删除', { type: 'warning' })
      .then(() => {
        request.delete(`/teacher/delByteacher_ID/${row.teacher_ID}`).then(res => {
          if (res.code === '200') {
            ElMessage.success('删除成功')
            load()
          }
        })
      })
}

loadClubs().then(() => load())
</script>
