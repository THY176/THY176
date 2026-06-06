<<template>
  <div class="table-container">
    <el-table :data="data" stripe style="width: 100%" v-loading="loading">
      <el-table-column
          v-for="col in columns"
          :key="col.prop"
          :prop="col.prop"
          :label="col.label"
          :width="col.width"
          :formatter="col.formatter"
      >
        <template #default="scope" v-if="col.slot">
          <slot :name="col.prop" :row="scope.row" :$index="scope.$index"></slot>
        </template>
      </el-table-column>

      <el-table-column label="操作" :width="actionWidth" v-if="showAction">
        <template #default="scope">
          <slot name="action" :row="scope.row" :$index="scope.$index"></slot>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper" v-if="showPagination">
      <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 15, 20, 50]"
          :total="total"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  data: { type: Array, default: () => [] },
  columns: { type: Array, default: () => [] },
  total: { type: Number, default: 0 },
  pageNum: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
  loading: { type: Boolean, default: false },
  showAction: { type: Boolean, default: true },
  actionWidth: { type: [String, Number], default: 250 },
  showPagination: { type: Boolean, default: true }
})

const emit = defineEmits(['update:pageNum', 'update:pageSize', 'pageChange'])

const currentPage = computed({
  get: () => props.pageNum,
  set: (val) => emit('update:pageNum', val)
})

const pageSize = computed({
  get: () => props.pageSize,
  set: (val) => emit('update:pageSize', val)
})

const handlePageChange = () => {
  emit('pageChange')
}

const handleSizeChange = () => {
  currentPage.value = 1
  emit('pageChange')
}
</script>

<style scoped>
.pagination-wrapper {
  margin-top: 15px;
  display: flex;
  justify-content: flex-end;
}
</style>