<<template>
  <el-dialog
      :title="title"
      :model-value="visible"
      @update:model-value="handleVisibleChange"
      :width="width"
      :close-on-click-modal="false"
      @close="handleClose"
  >
    <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="120px"
        style="padding-right: 30px"
    >
      <slot name="form-items"></slot>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCancel">取 消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确 定</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  title: { type: String, default: '对话框' },
  visible: { type: Boolean, default: false },
  width: { type: [String, Number], default: 500 },
  formData: { type: Object, default: () => ({}) },
  rules: { type: Object, default: () => ({}) },
  submitLoading: { type: Boolean, default: false }
})

const emit = defineEmits(['update:visible', 'submit', 'cancel'])

const formRef = ref(null)

// 监听对话框打开，重置表单验证状态
watch(() => props.visible, (val) => {
  if (val) {
    // 延迟执行，确保表单已渲染
    nextTick(() => {
      formRef.value?.clearValidate()
    })
  }
})

const handleVisibleChange = (val) => {
  emit('update:visible', val)
}

const handleClose = () => {
  emit('update:visible', false)
}

const handleCancel = () => {
  emit('cancel')
  emit('update:visible', false)
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    emit('submit')
  } catch (error) {
    ElMessage.warning('请检查表单填写是否正确')
  }
}

defineExpose({ formRef })
</script>