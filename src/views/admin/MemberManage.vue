<template>
  <div>
    <el-card>
      <div style="margin-bottom: 15px">
        <el-select
            v-model="searchForm.team_ID"
            placeholder="所属社团"
            clearable
            style="width: 180px; margin-right: 10px"
            @change="handleSearch"
            @clear="handleSearch"
        >
          <el-option v-for="club in clubList" :key="club.team_ID" :label="club.team_name" :value="club.team_ID" />
        </el-select>
        <el-input
            v-model="searchForm.name"
            placeholder="成员姓名"
            clearable
            style="width: 150px; margin-right: 10px"
            @clear="handleSearch"
            @keyup.enter="handleSearch"
        />
        <el-input
            v-model="searchForm.ID"
            placeholder="学号"
            clearable
            style="width: 150px; margin-right: 10px"
            @clear="handleSearch"
            @keyup.enter="handleSearch"
        />
        <el-button type="success" @click="openAddDialog">新增成员</el-button>
      </div>

      <CommonTable :data="tableData" :columns="columns" :total="total" v-model:page-num="pageNum" v-model:page-size="pageSize" @page-change="load">
        <template #action="{ row }">
          <el-button type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
          <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </CommonTable>
    </el-card>

    <!-- 新增/编辑成员弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px">
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="学号" prop="ID">
          <el-input v-model="formData.ID" placeholder="请输入学号" :disabled="!isAdd" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="formData.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="职位" prop="role">
          <el-select v-model="formData.role" placeholder="请选择职位">
            <el-option label="社长" value="社长" />
            <el-option label="副社长" value="副社长" />
            <el-option label="成员" value="成员" />
          </el-select>
        </el-form-item>
        <el-form-item label="电话" prop="tele">
          <el-input v-model="formData.tele" placeholder="请输入电话" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="formData.gender">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="formData.age" :min="15" :max="30" />
        </el-form-item>
        <el-form-item label="所属社团" prop="team_ID">
          <el-select v-model="formData.team_ID" placeholder="请选择所属社团">
            <el-option v-for="club in clubList" :key="club.team_ID" :label="club.team_name" :value="club.team_ID" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, watch, computed } from 'vue'
import request from '@/utils/request.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import CommonTable from '@/components/CommonTable.vue'

const searchForm = reactive({ team_ID: '', name: '', ID: '' })
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const clubList = ref([])

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const isAdd = ref(true)  // true: 新增, false: 编辑
const dialogTitle = computed(() => isAdd.value ? '新增成员' : '编辑成员')

const formData = reactive({
  ID: '',
  name: '',
  role: '',
  tele: '',
  gender: '男',
  age: 18,
  team_ID: ''
})

const validateStudentId = (_rule, value, callback) => {
  const id = Number(value)
  if (!Number.isInteger(id) || id <= 0) {
    callback(new Error('学号必须是正整数'))
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
  ID: [
    { required: true, message: '请输入学号', trigger: 'blur' },
    { validator: validateStudentId, trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择职位', trigger: 'change' }],
  tele: [
    { required: true, message: '请输入电话', trigger: 'blur' },
    { validator: validatePhone, trigger: 'blur' }
  ],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  age: [{ required: true, message: '请输入年龄', trigger: 'change' }],
  team_ID: [{ required: true, message: '请选择所属社团', trigger: 'change' }]
}

const columns = [
  { prop: 'ID', label: '学号', width: 100 },
  { prop: 'name', label: '姓名', width: 120 },
  { prop: 'team_name', label: '所属社团', width: 150 },
  { prop: 'role', label: '职位', width: 100 },
  { prop: 'tele', label: '电话', width: 150 },
  { prop: 'gender', label: '性别', width: 80 },
  { prop: 'age', label: '年龄', width: 80 }
]

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

  if (searchForm.team_ID) {
    params.team_ID = searchForm.team_ID
  }
  if (searchForm.name && searchForm.name.trim()) {
    params.name = searchForm.name.trim()
  }
  if (searchForm.ID && searchForm.ID.trim()) {
    params.ID = searchForm.ID.trim()
  }

  const res = await request.get('/student/selectPage', { params })
  if (res.code === '200') {
    const list = res.data.list || []
    // 补充社团名称
    for (let item of list) {
      const club = clubList.value.find(c => c.team_ID === item.team_ID)
      item.team_name = club?.team_name || '未知'
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
    () => [searchForm.name, searchForm.ID, searchForm.team_ID],
    () => {
      if (debounceTimer) clearTimeout(debounceTimer)
      debounceTimer = setTimeout(() => {
        pageNum.value = 1
        load()
      }, 300)
    }
)

// 打开新增弹窗
const openAddDialog = () => {
  isAdd.value = true
  formData.ID = ''
  formData.name = ''
  formData.role = ''
  formData.tele = ''
  formData.gender = '男'
  formData.age = 18
  formData.team_ID = ''
  dialogVisible.value = true
}

// 打开编辑弹窗
const openEditDialog = (row) => {
  isAdd.value = false
  formData.ID = row.ID
  formData.name = row.name
  formData.role = row.role
  formData.tele = row.tele
  formData.gender = row.gender
  formData.age = row.age
  formData.team_ID = row.team_ID
  dialogVisible.value = true
}

// 提交（新增或编辑）
const handleSubmit = () => {
  formRef.value.validate().then(() => {
    submitting.value = true

    if (isAdd.value) {
      // 新增：使用 POST
      const studentId = Number(formData.ID)
      const submitData = {
        ID: studentId,
        name: String(formData.name || '').trim(),
        role: formData.role,
        tele: String(formData.tele || '').trim(),
        gender: formData.gender,
        age: formData.age,
        team_ID: formData.team_ID
      }
      request.post('/student/add', submitData).then(res => {
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
      const studentId = Number(formData.ID)
      const submitData = {
        ID: studentId,
        name: String(formData.name || '').trim(),
        role: formData.role,
        tele: String(formData.tele || '').trim(),
        gender: formData.gender,
        age: formData.age,
        team_ID: formData.team_ID
      }
      request.put('/student/update', submitData).then(res => {
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
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除成员"${row.name}"吗？`, '确认删除', { type: 'warning' })
      .then(() => {
        request.delete(`/student/delByID/${row.ID}`).then(res => {
          if (res.code === '200') {
            ElMessage.success('删除成功')
            load()
          }
        })
      })
}

loadClubs().then(() => load())
</script>
