<template>
  <div class="gf-page">
    <div class="gf-page-head">
      <div>
        <h2 class="gf-title">面料库存</h2>
        <p class="gf-subtitle">低于 {{ dict.lowStockLine }} 米的面料自动标红预警</p>
      </div>
      <div style="display: flex; gap: 8px">
        <el-button @click="drawer = true">＋ 登记面料</el-button>
        <el-button type="primary" plain @click="reload">刷新</el-button>
      </div>
    </div>

    <div class="gf-metric-row">
      <div class="gf-metric">
        <div class="label">面料总数</div>
        <div class="value">{{ metrics.total }}</div>
      </div>
      <div class="gf-metric">
        <div class="label">库存总量（米）</div>
        <div class="value">{{ num(metrics.meters, 1) }}</div>
      </div>
      <div class="gf-metric">
        <div class="label">库存货值（元）</div>
        <div class="value" style="color: var(--gf-primary)">¥{{ yuan(metrics.value) }}</div>
      </div>
      <div class="gf-metric">
        <div class="label">低库存预警</div>
        <div class="value" style="color: var(--gf-danger)">{{ metrics.low }}</div>
      </div>
    </div>

    <div class="gf-toolbar">
      <el-radio-group v-model="query.composition" size="small" @change="reload">
        <el-radio-button label="">全部成分</el-radio-button>
        <el-radio-button v-for="(name, code) in dict.compositions" :key="code" :label="code">{{ name }}</el-radio-button>
      </el-radio-group>
      <el-input v-model="query.keyword" placeholder="搜编号 / 名称 / 供应商" clearable style="width: 220px"
                @keyup.enter="reload" />
      <el-checkbox v-model="query.lowOnly" @change="reload">只看低库存</el-checkbox>
      <el-button type="primary" plain @click="reload">查询</el-button>
      <span class="spacer" />
      <span class="gf-muted">已加载 {{ rows.length }} 卷</span>
    </div>

    <div class="gf-wall">
      <div v-for="item in rows" :key="item.id" class="gf-fabric-card" :class="{ low: item.lowStock }">
        <div class="strip" :style="{ background: item.colorHex }" />
        <div class="body">
          <div class="top">
            <div class="fname">{{ item.name }}</div>
            <div class="fno">{{ item.fabricNo }}</div>
          </div>
          <div class="meta">
            <el-tag size="small" effect="plain" type="danger">{{ dict.compositions[item.composition] }}</el-tag>
            <span class="gf-chip" style="padding: 2px 8px">
              <i class="gf-swatch" :style="{ background: item.colorHex, margin: 0 }" />{{ item.color }}
            </span>
            <span>¥{{ yuan(item.unitPrice) }}/米</span>
          </div>
          <div class="gf-stock-bar" :class="{ low: item.lowStock }">
            <i :style="{ width: stockPercent(item) + '%' }" />
          </div>
          <div class="gf-stock-line">
            <span :class="{ low: item.lowStock }">
              库存 {{ num(item.stockMeters, 1) }} 米
              <el-tag v-if="item.lowStock" size="small" type="danger" effect="dark" style="margin-left: 6px">预警</el-tag>
            </span>
            <span class="gf-muted">{{ item.supplier }}</span>
          </div>
          <div style="margin-top: 10px; display: flex; gap: 6px">
            <el-button size="small" type="primary" plain @click="openRestock(item)">补货</el-button>
            <el-button size="small" @click="openEdit(item)">编辑</el-button>
            <el-button size="small" type="danger" link @click="remove(item)">删除</el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="gf-load-more">
      <el-button v-if="hasNext" :loading="loading" @click="loadMore">加载更多</el-button>
      <span v-else class="gf-muted">没有更多了</span>
    </div>

    <el-dialog v-model="restockVisible" title="面料补货" width="380px">
      <el-form label-width="90px">
        <el-form-item label="面料">{{ restockRow.name }}</el-form-item>
        <el-form-item label="当前库存">
          <span class="gf-num">{{ num(restockRow.stockMeters, 1) }} 米</span>
        </el-form-item>
        <el-form-item label="补货米数">
          <el-input-number v-model="restockDelta" :min="1" :max="10000" :step="10" :precision="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="restockVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doRestock">确认补货</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="drawer" :title="form.id ? '编辑面料' : '登记面料'" width="460px">
      <el-form label-width="96px">
        <el-form-item label="面料编号"><el-input v-model="form.fabricNo" placeholder="如 FB-3013" /></el-form-item>
        <el-form-item label="面料名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="成分">
          <el-select v-model="form.composition" style="width: 100%">
            <el-option v-for="(name, code) in dict.compositions" :key="code" :label="name" :value="code" />
          </el-select>
        </el-form-item>
        <el-form-item label="颜色"><el-input v-model="form.color" placeholder="如 雾霭紫" /></el-form-item>
        <el-form-item label="色卡色值">
          <div style="display: flex; gap: 10px; align-items: center; width: 100%">
            <el-color-picker v-model="form.colorHex" />
            <el-input v-model="form.colorHex" style="flex: 1" />
          </div>
        </el-form-item>
        <el-form-item label="库存米数">
          <el-input-number v-model="form.stockMeters" :min="0" :step="10" :precision="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="单价(元/米)">
          <el-input-number v-model="form.unitPrice" :min="1" :step="20" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="供应商"><el-input v-model="form.supplier" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawer = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fabricApi, metaApi } from '../api/index.js'
import { num, yuan } from '../utils/format.js'

const dict = reactive({ compositions: {}, lowStockLine: 50 })
const query = reactive({ composition: '', keyword: '', lowOnly: false })
const rows = ref([])
const page = ref(0)
const size = 8
const hasNext = ref(false)
const loading = ref(false)
const saving = ref(false)
const drawer = ref(false)

const restockVisible = ref(false)
const restockRow = reactive({})
const restockDelta = ref(50)

const form = reactive(blankForm())

function blankForm() {
  return {
    id: null,
    fabricNo: '',
    name: '',
    composition: 'WOOL',
    color: '',
    colorHex: '#8c3b57',
    stockMeters: 120,
    unitPrice: 320,
    supplier: ''
  }
}

const metrics = computed(() => {
  const total = rows.value.length
  let meters = 0
  let value = 0
  let low = 0
  rows.value.forEach((item) => {
    meters += Number(item.stockMeters || 0)
    value += Number(item.stockMeters || 0) * Number(item.unitPrice || 0)
    if (item.lowStock) low += 1
  })
  return { total, meters, value, low }
})

const maxStock = computed(() =>
  rows.value.reduce((max, item) => Math.max(max, Number(item.stockMeters || 0)), 1)
)

function stockPercent(item) {
  return Math.min(100, Math.round((Number(item.stockMeters || 0) / maxStock.value) * 100))
}

async function fetchPage(reset) {
  if (reset) {
    page.value = 0
    rows.value = []
  }
  loading.value = true
  try {
    const slice = await fabricApi.list({
      composition: query.composition || undefined,
      lowOnly: query.lowOnly ? true : undefined,
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

function openEdit(item) {
  Object.assign(form, {
    id: item.id,
    fabricNo: item.fabricNo,
    name: item.name,
    composition: item.composition,
    color: item.color,
    colorHex: item.colorHex,
    stockMeters: Number(item.stockMeters),
    unitPrice: Number(item.unitPrice),
    supplier: item.supplier
  })
  drawer.value = true
}

async function submit() {
  saving.value = true
  try {
    await fabricApi.save({ ...form })
    ElMessage.success('面料已保存')
    drawer.value = false
    reload()
  } finally {
    saving.value = false
  }
}

function openRestock(item) {
  Object.assign(restockRow, item)
  restockDelta.value = 50
  restockVisible.value = true
}

async function doRestock() {
  saving.value = true
  try {
    await fabricApi.restock(restockRow.id, restockDelta.value)
    ElMessage.success('已补货')
    restockVisible.value = false
    reload()
  } finally {
    saving.value = false
  }
}

async function remove(item) {
  await ElMessageBox.confirm(`确认删除面料「${item.name}」？`, '删除确认', { type: 'warning' })
  await fabricApi.remove(item.id)
  ElMessage.success('已删除')
  reload()
}

onMounted(async () => {
  const meta = await metaApi.dict()
  dict.compositions = meta.compositions
  dict.lowStockLine = meta.lowStockLine
  await fetchPage(true)
})
</script>
