<script setup lang="ts">
/**
 * ============================================
 *  库存查询页 — 候选人需要实现（任务2）
 * ============================================
 *
 * 需求：
 * 1. 搜索栏：商品名称/SKU 模糊搜索 + 仓库下拉筛选
 * 2. 表格展示：商品名称、SKU、库位编码、仓库名、库存数量、更新时间
 * 3. 库存数量 < 10 的行高亮为红色
 * 4. 支持分页
 *
 * 建议使用 AI 协作完成此页面，参考 ProductsView.vue 的实现风格
 */
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getInventory, getWarehouses, type InventoryItem, type Warehouse } from '@/api'

const keyword = ref('')
const warehouseId = ref<number | undefined>()
const loading = ref(false)
const inventoryList = ref<InventoryItem[]>([])
const warehouses = ref<Warehouse[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
let keywordSearchTimer: number | undefined
let latestRequestId = 0

const loadInventory = async () => {
  const requestId = ++latestRequestId
  loading.value = true
  try {
    const res = await getInventory({
      keyword: keyword.value.trim() || undefined,
      warehouseId: warehouseId.value,
      page: page.value,
      pageSize: pageSize.value,
    })
    if (requestId !== latestRequestId) return

    inventoryList.value = res.data.list
    total.value = res.data.total
  } catch (e: any) {
    if (requestId === latestRequestId) {
      ElMessage.error('加载库存失败：' + (e.response?.data?.message || e.message))
    }
  } finally {
    if (requestId === latestRequestId) {
      loading.value = false
    }
  }
}

const loadWarehouses = async () => {
  try {
    const res = await getWarehouses()
    warehouses.value = res.data
  } catch (e: any) {
    ElMessage.error('加载仓库失败：' + (e.response?.data?.message || e.message))
  }
}

const searchFromFirstPage = () => {
  page.value = 1
  loadInventory()
}

const handleWarehouseChange = () => {
  searchFromFirstPage()
}

const handlePageChange = (nextPage: number) => {
  page.value = nextPage
  loadInventory()
}

const getRowStyle = (row: InventoryItem) => {
  if (row.quantity < 10) {
    return { backgroundColor: '#fff1f0', color: '#cf1322' }
  }
  return undefined
}

watch(keyword, () => {
  if (keywordSearchTimer !== undefined) window.clearTimeout(keywordSearchTimer)
  keywordSearchTimer = window.setTimeout(searchFromFirstPage, 350)
})

onMounted(async () => {
  await Promise.all([loadWarehouses(), loadInventory()])
})

onBeforeUnmount(() => {
  if (keywordSearchTimer !== undefined) window.clearTimeout(keywordSearchTimer)
})
</script>

<template>
  <div>
    <h3> 库存查询</h3>

    <div style="display: flex; gap: 12px; margin-bottom: 16px">
      <el-input
        v-model="keyword"
        placeholder="搜索商品名称/SKU..."
        style="width: 300px"
        clearable
        @keyup.enter="searchFromFirstPage"
      />
      <el-select
        v-model="warehouseId"
        placeholder="选择仓库"
        clearable
        style="width: 200px"
        @change="handleWarehouseChange"
      >
        <el-option v-for="warehouse in warehouses" :key="warehouse.id" :label="warehouse.name" :value="warehouse.id" />
      </el-select>
      <el-button type="primary" @click="searchFromFirstPage">查询</el-button>
    </div>

    <el-table :data="inventoryList" v-loading="loading" border stripe :row-style="getRowStyle">
      <el-table-column prop="productName" label="商品名称" />
      <el-table-column prop="sku" label="SKU" width="150" />
      <el-table-column prop="locationCode" label="库位编码" width="150" />
      <el-table-column prop="warehouseName" label="仓库" width="120" />
      <el-table-column prop="quantity" label="库存数量" width="100" />
      <el-table-column prop="updatedAt" label="更新时间" width="180" />
    </el-table>

    <div style="margin-top: 16px; text-align: right">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>
