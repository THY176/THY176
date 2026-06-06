<template>
  <div>
    <el-card>
      <div style="margin-bottom: 15px">
        <el-input
            v-model="searchForm.name"
            placeholder="请输入成员姓名"
            style="width: 200px; margin-right: 10px"
            clearable
            @clear="load"
            @keyup.enter="load"
        />
        <el-input
            v-model="searchForm.ID"
            placeholder="请输入学号"
            style="width: 200px; margin-right: 10px"
            clearable
            @clear="load"
            @keyup.enter="load"
        />
        <el-button type="success" @click="openAddDialog">新增成员</el-button>
      </div>

      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%">
        <el-table-column label="学号" width="120">
          <template #default="{ row }">
            {{ row.ID || row.id }}
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="role" label="职位" width="100" />
        <el-table-column prop="tele" label="电话" width="150" />
        <el-table-column prop="gender" label="性别" width="80" />
        <el-table-column prop="age" label="年龄" width="80" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :page-sizes="[10, 15, 20, 50]"
            :total="total"
            background
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="load"
            @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <CommonDialog
        :title="dialogTitle"
        v-model:visible="dialogVisible"
        :form-data="formData"
        :rules="rules"
        @submit="handleSubmit"
    >
      <template #form-items>
        <!-- 学号输入框，新增和编辑都可编辑 -->
        <el-form-item label="学号" prop="ID">
          <el-input v-model="formData.ID" placeholder="请输入学号" />
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
      </template>
    </CommonDialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user.js'
import request from '@/utils/request.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import CommonDialog from '@/components/CommonDialog.vue'

const userStore = useUserStore()
const router = useRouter()

const getTeamId = () => {
  return userStore.userId
}

const searchForm = reactive({ name: '', ID: '' })
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const loading = ref(false)

const dialogVisible = ref(false)
const isAdd = ref(true)
const dialogTitle = computed(() => isAdd.value ? '新增成员' : '编辑成员')

// 保存原学号，用于编辑时判断是否需要删除旧记录
const originalId = ref(null)

// 默认空表单数据
const getEmptyFormData = () => ({
  ID: '',
  name: '',
  role: '',
  tele: '',
  gender: '男',
  age: 18,
  team_ID: null
})

// 表单数据 - 使用 ref 包装对象，便于整体替换
const formData = ref(getEmptyFormData())

// 重置表单数据
const resetFormData = () => {
  formData.value = getEmptyFormData()
}

const rules = {
  ID: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择职位', trigger: 'change' }],
  tele: [{ required: true, message: '请输入电话', trigger: 'blur' }]
}

const load = () => {
  const teamId = getTeamId()
  if (!teamId) {
    ElMessage.error('未获取到社团信息，请重新登录')
    setTimeout(() => {
      router.push('/')
    }, 1500)
    return
  }

  loading.value = true

  const params = {
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    team_ID: teamId
  }

  if (searchForm.name && searchForm.name.trim()) {
    params.name = searchForm.name.trim()
  }
  if (searchForm.ID && searchForm.ID.trim()) {
    params.ID = searchForm.ID.trim()
  }

  request.get('/student/selectPage', { params }).then(res => {
    if (res.code === '200') {
      tableData.value = res.data.list || []
      total.value = res.data.total || 0
    }
  }).catch(err => {
    console.error('加载成员失败:', err)
    ElMessage.error('加载成员失败')
  }).finally(() => {
    loading.value = false
  })
}

const handleSizeChange = () => {
  pageNum.value = 1
  load()
}

let debounceTimer = null
watch(
    () => [searchForm.name, searchForm.ID],
    () => {
      pageNum.value = 1
      if (debounceTimer) clearTimeout(debounceTimer)
      debounceTimer = setTimeout(() => {
        load()
      }, 300)
    }
)

// 打开新增对话框
const openAddDialog = () => {
  isAdd.value = true
  originalId.value = null
  const teamId = getTeamId()

  // 重置为空表单
  resetFormData()
  formData.value.team_ID = teamId

  dialogVisible.value = true
}

// 打开编辑对话框
const openEditDialog = (row) => {
  isAdd.value = false
  // 保存原学号
  originalId.value = row.ID || row.id

  // 设置表单为要编辑的成员信息
  formData.value = {
    ID: row.ID || row.id,
    name: row.name || '',
    role: row.role || '',
    tele: row.tele || '',
    gender: row.gender || '男',
    age: row.age || 18,
    team_ID: row.team_ID || getTeamId()
  }

  dialogVisible.value = true
}

const handleSubmit = () => {
  const teamId = getTeamId()

  if (!teamId) {
    ElMessage.error('未获取到社团信息')
    return
  }

  // 验证学号
  if (!formData.value.ID || formData.value.ID === '') {
    ElMessage.error('请填写学号')
    return
  }

  const submitData = {
    ID: Number(formData.value.ID),
    name: formData.value.name,
    role: formData.value.role,
    tele: formData.value.tele,
    gender: formData.value.gender,
    age: formData.value.age,
    team_ID: teamId
  }
  console.log('提交数据:', submitData)

  if (isAdd.value) {
    // 新增：使用 POST
    request.post('/student/add', submitData).then(res => {
      if (res.code === '200') {
        ElMessage.success('新增成功')
        dialogVisible.value = false
        // 重置表单
        resetFormData()
        load()
        window.dispatchEvent(new CustomEvent('member-deleted'))
      } else {
        ElMessage.error(res.msg || '新增失败')
      }
    }).catch(err => {
      console.error('新增失败:', err)
      ElMessage.error('新增失败')
    })
  } else {
    // 编辑：传递原学号参数
    const url = originalId.value && originalId.value !== formData.value.ID
        ? `/student/update?oldId=${originalId.value}`
        : '/student/update'

    console.log('请求URL:', url)

    request.put(url, submitData).then(res => {
      if (res.code === '200') {
        ElMessage.success('修改成功')
        dialogVisible.value = false
        // 重置表单
        resetFormData()
        load()
      } else {
        ElMessage.error(res.msg || '修改失败')
      }
    }).catch(err => {
      console.error('修改失败:', err)
      ElMessage.error('修改失败')
    })
  }
}

const handleDelete = (row) => {
  const studentId = row.ID || row.id
  ElMessageBox.confirm(`确定删除成员"${row.name}"吗？`, '确认删除', { type: 'warning' })
      .then(() => {
        request.delete(`/student/delByID/${studentId}`).then(res => {
          if (res.code === '200') {
            ElMessage.success('删除成功')
            load()
            if (window.location.pathname === '/team/profile') {
              window.location.reload()
            }
          } else {
            ElMessage.error(res.msg || '删除失败')
          }
        }).catch(err => {
          console.error('删除失败:', err)
          ElMessage.error('删除失败')
        })
      })
}

onMounted(() => {
  if (!getTeamId()) {
    ElMessage.error('未获取到社团信息，请重新登录')
    setTimeout(() => {
      router.push('/')
    }, 1500)
  } else {
    load()
  }
})
</script>

<style scoped>
.pagination-wrapper {
  margin-top: 15px;
  display: flex;
  justify-content: flex-end;
}
</style>
