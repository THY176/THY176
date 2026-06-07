<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>编辑申请表</span>
          <el-button @click="router.back()">返回</el-button>
        </div>
      </template>

      <el-form :model="formData" :rules="rules" ref="formRef" label-width="140px" v-loading="loading">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="申请金额（元）" prop="apply_money">
              <el-input v-model="formData.apply_money" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="申请类型" prop="apply_type">
              <el-select v-model="formData.apply_type">
                <el-option label="活动费" value="活动费" />
                <el-option label="比赛费" value="比赛费" />
                <el-option label="购物费" value="采购费" />
                <el-option label="设备费" value="设备费" />
                <el-option label="培训费" value="培训费" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="申请原因" prop="reason">
          <el-input v-model="formData.reason" type="textarea" :rows="4" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="收款账户名" prop="account_name">
              <el-input v-model="formData.account_name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开户银行" prop="bank">
              <el-input v-model="formData.bank" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="银行账号" prop="account">
          <el-input v-model="formData.account" style="width: 400px" />
        </el-form-item>

        <!-- 四象限分类标签 -->
        <el-form-item label="事项分类" required>
          <div class="quadrant-tags">
            <el-tag
                :type="selectedQuadrant === 'urgent_important' ? 'danger' : 'info'"
                :effect="selectedQuadrant === 'urgent_important' ? 'dark' : 'plain'"
                @click="selectQuadrant('urgent_important')"
                class="quadrant-tag"
            >
              紧急且重要
            </el-tag>
            <el-tag
                :type="selectedQuadrant === 'not_urgent_important' ? 'warning' : 'info'"
                :effect="selectedQuadrant === 'not_urgent_important' ? 'dark' : 'plain'"
                @click="selectQuadrant('not_urgent_important')"
                class="quadrant-tag"
            >
              重要不紧急
            </el-tag>
            <el-tag
                :type="selectedQuadrant === 'urgent_not_important' ? 'success' : 'info'"
                :effect="selectedQuadrant === 'urgent_not_important' ? 'dark' : 'plain'"
                @click="selectQuadrant('urgent_not_important')"
                class="quadrant-tag"
            >
              紧急不重要
            </el-tag>
            <el-tag
                :type="selectedQuadrant === 'not_urgent_not_important' ? '' : 'info'"
                :effect="selectedQuadrant === 'not_urgent_not_important' ? 'dark' : 'plain'"
                @click="selectQuadrant('not_urgent_not_important')"
                class="quadrant-tag"
            >
              不紧急不重要
            </el-tag>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSave">保存修改</el-button>
          <el-button type="success" @click="handleSubmit">保存并提交</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request.js'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const loading = ref(true)

// 四象限选择
const selectedQuadrant = ref('')

// 四象限配置
const quadrantMap = {
  'urgent_important': { label: '紧急且重要' },
  'not_urgent_important': { label: '重要不紧急' },
  'urgent_not_important': { label: '紧急不重要' },
  'not_urgent_not_important': { label: '不紧急不重要' }
}

// 根据标签获取象限key
const getQuadrantKey = (tag) => {
  if (!tag) return ''
  for (const [key, value] of Object.entries(quadrantMap)) {
    if (value.label === tag) {
      return key
    }
  }
  return ''
}

const selectQuadrant = (quadrant) => {
  selectedQuadrant.value = quadrant
}

const formData = reactive({
  apply_ID: '',
  team_ID: '',
  apply_money: '',
  apply_type: '',
  reason: '',
  apply_time: '',
  account_name: '',
  bank: '',
  account: '',
  status: '',
  tags: '',
  processInstanceId: ''
})

const rules = {
  apply_money: [{ required: true, message: '请输入申请金额', trigger: 'blur' }],
  apply_type: [{ required: true, message: '请选择申请类型', trigger: 'change' }],
  reason: [{ required: true, message: '请输入申请原因', trigger: 'blur' }],
  account_name: [{ required: true, message: '请输入收款账户名', trigger: 'blur' }],
  bank: [{ required: true, message: '请输入开户银行', trigger: 'blur' }],
  account: [{ required: true, message: '请输入银行账号', trigger: 'blur' }]
}

onMounted(() => {
  const apply_ID = route.params.apply_ID
  console.log('编辑申请ID:', apply_ID)

  request.get(`/apply/selectByapply_ID/${apply_ID}`).then(res => {
    console.log('后端返回的原始数据:', res)

    if (res.code === '200' && res.data) {
      const data = res.data
      console.log('data 的所有字段:', Object.keys(data))
      console.log('apply_money:', data.apply_money)
      console.log('apply_type:', data.apply_type)
      console.log('account_name:', data.account_name)
      console.log('tags:', data.tags)

      // 直接使用 Object.assign 复制所有字段
      Object.assign(formData, {
        apply_ID: data.apply_ID,
        team_ID: data.team_ID,
        apply_money: data.apply_money,
        apply_type: data.apply_type,
        reason: data.reason,
        apply_time: data.apply_time,
        account_name: data.account_name,
        bank: data.bank,
        account: data.account,
        status: data.status,
        tags: data.tags || '',
        processInstanceId: data.processInstanceId || ''
      })

      console.log('赋值后的 formData:', formData)

      // 根据已有的 tags 设置选中的象限
      const quadrantKey = getQuadrantKey(formData.tags)
      if (quadrantKey) {
        selectedQuadrant.value = quadrantKey
        console.log('已选中象限:', quadrantKey)
      }

      loading.value = false
    } else {
      ElMessage.error('加载申请数据失败')
      router.back()
    }
  }).catch(err => {
    console.error('加载失败:', err)
    ElMessage.error('加载失败')
    router.back()
  })
})

const doUpdate = (action) => {
  // 验证四象限是否已选择
  if (!selectedQuadrant.value) {
    ElMessage.error('请选择事项分类')
    return
  }

  // 更新标签
  formData.tags = quadrantMap[selectedQuadrant.value].label

  console.log('提交更新数据:', formData)

  const payload = { ...formData }

  if (action === 'save') {
    payload.status = formData.status || '待提交'
    request.put('/apply/update', payload).then(res => {
      if (res.code === '200') {
        ElMessage.success('保存成功')
        router.push('/team/history')
      } else {
        ElMessage.error(res.msg || '操作失败')
      }
    }).catch(err => {
      console.error('更新失败:', err)
      ElMessage.error(err.message || '更新失败')
    })
    return
  }

  if (formData.status === '待提交') {
    payload.status = '待提交'
    request.put('/apply/update', payload)
        .then(() => request.post('/apply/startWorkflow', null, {
          params: { applyId: formData.apply_ID }
        }))
        .then(res => {
          if (res.code === '200') {
            ElMessage.success('提交成功')
            router.push('/team/history')
          } else {
            ElMessage.error(res.msg || '操作失败')
          }
        })
        .catch(err => {
          console.error('提交失败:', err)
          ElMessage.error(err.message || '提交失败')
        })
    return
  }

  if (formData.status === '审核驳回') {
    request.post('/apply/resubmit', payload).then(res => {
      if (res.code === '200') {
        ElMessage.success('提交成功')
        router.push('/team/history')
      } else {
        ElMessage.error(res.msg || '操作失败')
      }
    }).catch(err => {
      console.error('重新提交失败:', err)
      ElMessage.error(err.message || '重新提交失败')
    })
    return
  }

  payload.status = formData.status
  request.put('/apply/update', payload).then(res => {
    if (res.code === '200') {
      ElMessage.success('提交成功')
      router.push('/team/history')
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  }).catch(err => {
    console.error('提交失败:', err)
    ElMessage.error(err.message || '提交失败')
  })
}

const handleSave = () => {
  formRef.value.validate().then(() => doUpdate('save'))
}

const handleSubmit = () => {
  formRef.value.validate().then(() => doUpdate('submit'))
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.quadrant-tags {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}
.quadrant-tag {
  cursor: pointer;
  padding: 8px 16px;
  font-size: 14px;
  transition: all 0.3s;
}
.quadrant-tag:hover {
  transform: scale(1.05);
}
</style>
