<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createInboundOrder,
  getLocations,
  getProducts,
  getWarehouses,
  type Location,
  type Product,
  type Warehouse,
} from '@/api'

interface InboundFormItem {
  productId?: number
  warehouseId?: number
  locationCode?: string
  quantity: number | undefined
  locations: Location[]
  loadingLocations: boolean
}

const supplierName = ref('')
const items = ref<InboundFormItem[]>([])
const products = ref<Product[]>([])
const warehouses = ref<Warehouse[]>([])
const submitting = ref(false)

const createEmptyItem = (): InboundFormItem => ({
  productId: undefined,
  warehouseId: undefined,
  locationCode: undefined,
  quantity: 1,
  locations: [],
  loadingLocations: false,
})

const addItem = () => {
  items.value.push(createEmptyItem())
}

const removeItem = (index: number) => {
  items.value.splice(index, 1)
}

const loadBaseOptions = async () => {
  try {
    const [productRes, warehouseRes] = await Promise.all([getProducts(), getWarehouses()])
    products.value = productRes.data
    warehouses.value = warehouseRes.data
  } catch (e: any) {
    ElMessage.error('加载商品或仓库数据失败：' + (e.response?.data?.message || e.message))
  }
}

const handleWarehouseChange = async (item: InboundFormItem) => {
  item.locationCode = undefined
  item.locations = []
  if (!item.warehouseId) return

  const selectedWarehouseId = item.warehouseId
  item.loadingLocations = true
  try {
    const res = await getLocations(selectedWarehouseId)
    if (item.warehouseId === selectedWarehouseId) {
      item.locations = res.data
    }
  } catch (e: any) {
    if (item.warehouseId === selectedWarehouseId) {
      ElMessage.error('加载库位失败：' + (e.response?.data?.message || e.message))
    }
  } finally {
    if (item.warehouseId === selectedWarehouseId) {
      item.loadingLocations = false
    }
  }
}

const validateForm = () => {
  if (!supplierName.value.trim()) {
    ElMessage.warning('请输入供应商名称')
    return false
  }
  if (items.value.length === 0) {
    ElMessage.warning('请至少添加一条入库明细')
    return false
  }

  const invalidIndex = items.value.findIndex((item) =>
    !item.productId || !item.warehouseId || !item.locationCode ||
    !Number.isInteger(item.quantity) || (item.quantity ?? 0) <= 0,
  )
  if (invalidIndex !== -1) {
    ElMessage.warning(`请完整填写第 ${invalidIndex + 1} 条入库明细`)
    return false
  }
  return true
}

const handleSubmit = async () => {
  if (!validateForm()) return

  submitting.value = true
  try {
    const res = await createInboundOrder({
      supplierName: supplierName.value.trim(),
      items: items.value.map((item) => ({
        productId: item.productId as number,
        quantity: item.quantity as number,
        locationCode: item.locationCode as string,
      })),
    })
    ElMessage.success(`入库单创建成功：${res.data?.orderNo || ''}`)
    supplierName.value = ''
    items.value = []
  } catch (e: any) {
    ElMessage.error('创建入库单失败：' + (e.response?.data?.message || e.message))
  } finally {
    submitting.value = false
  }
}

onMounted(loadBaseOptions)
</script>

<template>
  <div>
    <h3>入库管理</h3>

    <el-form label-width="100px" style="max-width: 960px">
      <el-form-item label="供应商名称" required>
        <el-input v-model="supplierName" placeholder="请输入供应商名称" maxlength="200" />
      </el-form-item>

      <el-form-item label="入库明细">
        <el-button type="primary" @click="addItem">+ 添加明细</el-button>
      </el-form-item>
    </el-form>

    <div
      v-for="(item, index) in items"
      :key="index"
      style="margin-bottom: 12px; display: flex; gap: 12px; align-items: center"
    >
      <el-select v-model="item.productId" filterable placeholder="选择商品" style="width: 220px">
        <el-option
          v-for="product in products"
          :key="product.id"
          :label="`${product.name}（${product.sku}）`"
          :value="product.id"
        />
      </el-select>

      <el-select v-model="item.warehouseId" placeholder="选择仓库" style="width: 160px" @change="handleWarehouseChange(item)">
        <el-option v-for="warehouse in warehouses" :key="warehouse.id" :label="warehouse.name" :value="warehouse.id" />
      </el-select>

      <el-select
        v-model="item.locationCode"
        placeholder="选择库位"
        style="width: 180px"
        :loading="item.loadingLocations"
        :disabled="!item.warehouseId || item.loadingLocations"
      >
        <el-option v-for="location in item.locations" :key="location.id" :label="location.code" :value="location.code" />
      </el-select>

      <el-input-number v-model="item.quantity" :min="1" :precision="0" placeholder="数量" style="width: 130px" />
      <el-button type="danger" size="small" @click="removeItem(index)">删除</el-button>
    </div>

    <el-button type="success" :loading="submitting" :disabled="items.length === 0" @click="handleSubmit">
      提交入库单
    </el-button>

    <el-empty v-if="items.length === 0" description="请点击“添加明细”按钮添加入库商品" />
  </div>
</template>
