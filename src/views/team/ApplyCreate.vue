<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>经费申请表</span>
          <el-radio-group v-model="submitType" size="small">
            <el-radio-button label="save">暂存草稿</el-radio-button>
            <el-radio-button label="submit">立即提交</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-form :model="formData" :rules="rules" ref="formRef" label-width="140px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="申请金额（元）" prop="apply_money">
              <el-input v-model="formData.apply_money" placeholder="请输入金额" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="申请类型" prop="apply_type">
              <el-select v-model="formData.apply_type" placeholder="请选择类型">
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
          <el-input
              v-model="formData.reason"
              type="textarea"
              :rows="4"
              placeholder="请详细说明经费用途，经费使用明细，在200字左右"
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="收款账户名" prop="account_name">
              <el-input v-model="formData.account_name" placeholder="请输入账户名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开户银行" prop="bank">
              <el-input v-model="formData.bank" placeholder="请输入开户银行" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="银行账号" prop="account">
          <el-input v-model="formData.account" placeholder="请输入银行账号" style="width: 400px" />
        </el-form-item>

        <!-- 四象限分类标签 -->
        <el-form-item label="标签" required>
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

        <!-- 按钮顺序：提交/保存按钮在前，重置在后 -->
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            {{ submitType === 'save' ? '保存草稿' : '提交申请' }}
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, nextTick, onMounted } from 'vue'
import { useUserStore } from '@/stores/user.js'
import request from '@/utils/request.js'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()
const formRef = ref(null)

onMounted(() => {
  if (!userStore.userId) {
    ElMessage.error('未获取到社团信息，请重新登录')
    router.push('/')
  }
})

const submitType = ref('save')
const submitting = ref(false)

// 四象限选择
const selectedQuadrant = ref('')

// 四象限配置
const quadrantMap = {
  'urgent_important': { label: '紧急且重要' },
  'not_urgent_important': { label: '重要不紧急' },
  'urgent_not_important': { label: '紧急不重要' },
  'not_urgent_not_important': { label: '不紧急不重要' }
}

const selectQuadrant = (quadrant) => {
  selectedQuadrant.value = quadrant
}

const formData = reactive({
  team_ID: userStore.userId,
  apply_money: '',
  apply_type: '',
  reason: '',
  apply_time: '',
  account_name: '',
  bank: '',
  account: '',
  status: '待提交',
  tags: ''
})

const rules = {
  apply_money: [{ required: true, message: '请输入申请金额', trigger: 'blur' }],
  apply_type: [{ required: true, message: '请选择申请类型', trigger: 'change' }],
  reason: [{ required: true, message: '请输入申请原因', trigger: 'blur' }],
  account_name: [{ required: true, message: '请输入收款账户名', trigger: 'blur' }],
  bank: [{ required: true, message: '请输入开户银行', trigger: 'blur' }],
  account: [{ required: true, message: '请输入银行账号', trigger: 'blur' }]
}

const handleSubmit = () => {
  // 验证四象限是否已选择
  if (!selectedQuadrant.value) {
    ElMessage.error('请选择事项分类')
    return
  }

  formRef.value.validate().then(() => {
    submitting.value = true

    // 设置标签为选中的四象限名称
    formData.tags = quadrantMap[selectedQuadrant.value].label
    formData.apply_time = new Date().toISOString().slice(0, 19).replace('T', ' ')
    formData.status = submitType.value === 'save' ? '待提交' : '待审核'

    const submitData = {
      team_ID: formData.team_ID,
      apply_money: formData.apply_money,
      apply_type: formData.apply_type,
      reason: formData.reason,
      apply_time: formData.apply_time,
      account_name: formData.account_name,
      bank: formData.bank,
      account: formData.account,
      status: formData.status,
      tags: formData.tags
    }

    if (submitType.value === 'submit') {
      // 提交申请 - 调用工作流提交接口
      request.post('/apply/submit', submitData).then(res => {
        if (res.code === '200') {
          ElMessage.success('申请提交成功，等待审核')
          router.push('/team/history')
        } else {
          ElMessage.error(res.msg || '操作失败')
        }
      }).finally(() => {
        submitting.value = false
      })
    } else {
      // 保存草稿
      request.post('/apply/add', submitData).then(res => {
        if (res.code === '200') {
          ElMessage.success('草稿保存成功')
          resetForm()
        } else {
          ElMessage.error(res.msg || '操作失败')
        }
      }).finally(() => {
        submitting.value = false
      })
    }
  })
}

const resetForm = () => {
  formRef.value?.resetFields()
  selectedQuadrant.value = ''
  Object.assign(formData, {
    team_ID: userStore.userId,
    apply_money: '',
    apply_type: '',
    reason: '',
    apply_time: '',
    account_name: '',
    bank: '',
    account: '',
    status: '待提交',
    tags: ''
  })
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