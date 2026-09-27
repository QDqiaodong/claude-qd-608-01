<template>
  <div class="gf-page">
    <div class="gf-page-head">
      <div>
        <h2 class="gf-title">款式管理</h2>
        <p class="gf-subtitle">款号为定制款的唯一档案编号，停售款式不能用于新订单</p>
      </div>
      <el-button type="primary" @click="openCreate">＋ 新增款式</el-button>
    </div>

    <div class="gf-toolbar">
      <el-select v-model="query.category" placeholder="全部品类" clearable style="width: 150px">
        <el-option v-for="(name, code) in dict.categories" :key="code" :label="name" :value="code" />
      </el-select>
      <el-select v-model="query.onSale" placeholder="上下架" clearable style="width: 130px">
        <el-option label="在售" :value="true" />
        <el-option label="停售" :value="false" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="搜款号 / 名称" clearable style="width: 210px"
                @keyup.enter="reload" />
      <el-button type="primary" plain @click="reload">查询</el-button>
      <span class="spacer" />
      <span class="gf-muted">已加载 {{ rows.length }} 款</span>
    </div>

    <div class="gf-card" style="padding: 6px 6px 10px">
      <el-table :data="rows" border stripe style="width: 100%">
        <el-table-column prop="styleNo" label="款号" width="120" />
        <el-table-column prop="name" label="款式名称" min-width="180" />
        <el-table-column label="品类" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" type="danger">{{ dict.categories[row.category] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="基础价" width="130" align="right">
          <template #default="{ row }"><span class="gf-money">¥{{ yuan(row.basePrice) }}</span></template>
        </el-table-column>
        <el-table-column label="工艺难度" width="170">
          <template #default="{ row }">
            <el-rate :model-value="row.craftLevel" :max="5" disabled size="small" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.onSale ? 'success' : 'info'">{{ row.onSale ? '在售' : '停售' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.onSale ? 'warning' : 'success'" @click="toggle(row)">
              {{ row.onSale ? '停售' : '上架' }}
            </el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="gf-load-more">
        <el-button v-if="hasNext" :loading="loading" @click="loadMore">加载更多</el-button>
        <span v-else class="gf-muted">没有更多了</span>
      </div>
    </div>

    <el-drawer v-model="drawer" :title="form.id ? '编辑款式' : '新增款式'" size="440px">
      <el-form label-width="88px">
        <el-form-item label="款号">
          <el-input v-model="form.styleNo" placeholder="如 ST-1013" />
        </el-form-item>
        <el-form-item label="款式名称">
          <el-input v-model="form.name" placeholder="如 双面呢长款大衣" />
        </el-form-item>
        <el-form-item label="品类">
          <el-select v-model="form.category" style="width: 100%">
            <el-option v-for="(name, code) in dict.categories" :key="code" :label="name" :value="code" />
          </el-select>
        </el-form-item>
        <el-form-item label="基础价">
          <el-input-number v-model="form.basePrice" :min="0" :step="100" :precision="2" style="width: 100%" />
          <div class="gf-muted" style="font-size: 12px; margin-top: 4px">
            {{ dict.categories[form.category] }}指导最低价 ¥{{ yuan(minPrice) }}
          </div>
        </el-form-item>
        <el-form-item label="工艺难度">
          <el-slider v-model="form.craftLevel" :min="1" :max="5" show-stops />
        </el-form-item>
        <el-form-item label="是否在售">
          <el-switch v-model="form.onSale" active-text="在售" inactive-text="停售" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="工艺亮点 / 版型说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawer = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { styleApi, metaApi } from '../api/index.js'
import { yuan } from '../utils/format.js'

const dict = reactive({ categories: {} })
const query = reactive({ category: '', onSale: '', keyword: '' })
const rows = ref([])
const page = ref(0)
const size = 8
const hasNext = ref(false)
const loading = ref(false)
const saving = ref(false)
const drawer = ref(false)
const form = reactive(blankForm())

const categoryMinPrice = {
  SUIT: 2000,
  OVERCOAT: 3000,
  SHIRT: 600,
  DRESS: 4000,
  PANTS: 700
}
const minPrice = computed(() => categoryMinPrice[form.category] || 0)

function blankForm() {
  return {
    id: null,
    styleNo: '',
    name: '',
    category: 'SUIT',
    basePrice: 3880,
    craftLevel: 3,
    onSale: true,
    remark: ''
  }
}

async function loadDict() {
  const meta = await metaApi.dict()
  Object.assign(dict, { categories: meta.categories })
}

async function fetchPage(reset) {
  if (reset) {
    page.value = 0
    rows.value = []
  }
  loading.value = true
  try {
    const slice = await styleApi.list({
      category: query.category || undefined,
      onSale: query.onSale === '' ? undefined : query.onSale,
      keyword: query.keyword || undefined,
      page: page.value,
      size
    })
    rows.value = rows.value.concat(slice.content)
    hasNext.value = slice.hasNext
  } finally {
    loading.value = false
  }
}

function reload() {
  fetchPage(true)
}

function loadMore() {
  page.value += 1
  fetchPage(false)
}

function openCreate() {
  Object.assign(form, blankForm())
  drawer.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    styleNo: row.styleNo,
    name: row.name,
    category: row.category,
    basePrice: Number(row.basePrice),
    craftLevel: row.craftLevel,
    onSale: row.onSale,
    remark: row.remark
  })
  drawer.value = true
}

async function submit() {
  saving.value = true
  try {
    await styleApi.save({ ...form })
    ElMessage.success('款式已保存')
    drawer.value = false
    reload()
  } finally {
    saving.value = false
  }
}

async function toggle(row) {
  await styleApi.toggleSale(row.id)
  ElMessage.success(row.onSale ? '已停售' : '已上架')
  reload()
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除款式「${row.name}」？`, '删除确认', { type: 'warning' })
  await styleApi.remove(row.id)
  ElMessage.success('已删除')
  reload()
}

onMounted(async () => {
  await loadDict()
  await fetchPage(true)
})
</script>
