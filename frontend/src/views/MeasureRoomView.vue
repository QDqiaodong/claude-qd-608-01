<template>
  <div class="gf-page">
    <div class="gf-page-head">
      <div>
        <h2 class="gf-title">量体记录</h2>
        <p class="gf-subtitle">红色标记为超出该部位合理区间、需要复核的尺寸</p>
      </div>
      <el-button type="primary" @click="openCreate">＋ 录一单量体</el-button>
    </div>

    <div class="gf-toolbar">
      <el-select v-model="query.customerId" placeholder="全部客户" clearable filterable style="width: 190px">
        <el-option v-for="c in customers" :key="c.id" :label="`${c.name} · ${c.customerNo}`" :value="c.id" />
      </el-select>
      <el-select v-model="query.tailorName" placeholder="全部量体师" clearable style="width: 150px">
        <el-option v-for="t in tailors" :key="t" :label="t" :value="t" />
      </el-select>
      <el-button type="primary" plain @click="reload">查询</el-button>
      <span class="spacer" />
      <span class="gf-muted">已加载 {{ rows.length }} 条</span>
    </div>

    <div class="gf-card" style="padding: 6px 6px 10px">
      <el-table :data="rows" border stripe style="width: 100%">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div style="padding: 8px 14px; display: flex; flex-wrap: wrap; gap: 10px 22px">
              <div v-for="p in parts" :key="p.key">
                <div class="gf-muted" style="font-size: 12px">{{ p.name }}</div>
                <div style="font-size: 15px; font-weight: 600" :style="{ color: inRange(row, p.key) ? '' : 'var(--gf-danger)' }">
                  {{ num(row[p.key]) }} cm
                </div>
              </div>
              <div style="flex: 1 1 240px">
                <div class="gf-muted" style="font-size: 12px">备注</div>
                <div style="font-size: 13.5px">{{ row.remark || '—' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="recordNo" label="量体单号" width="130" />
        <el-table-column prop="customerName" label="客户" width="120" />
        <el-table-column prop="tailorName" label="量体师" width="100" />
        <el-table-column label="量体日期" width="130">
          <template #default="{ row }">{{ day(row.measureDate) }}</template>
        </el-table-column>
        <el-table-column label="三围" min-width="220">
          <template #default="{ row }">
            胸 {{ num(row.bust) }} · 腰 {{ num(row.waist) }} · 臀 {{ num(row.hip) }}
          </template>
        </el-table-column>
        <el-table-column label="异常复核" width="110" align="center">
          <template #default="{ row }">
            <el-tag v-if="abnormal(row)" size="small" type="danger">需复核</el-tag>
            <el-tag v-else size="small" type="success">正常</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="gf-load-more">
        <el-button v-if="hasNext" :loading="loading" @click="loadMore">加载更多</el-button>
        <span v-else class="gf-muted">没有更多了</span>
      </div>
    </div>

    <el-drawer v-model="drawer" :title="form.id ? '编辑量体记录' : '录入量体'" size="520px">
      <el-form label-width="86px">
        <el-form-item label="量体单号">
          <el-input v-model="form.recordNo" placeholder="如 ME-4013" />
        </el-form-item>
        <el-form-item label="客户">
          <el-select v-model="form.customerId" filterable style="width: 100%">
            <el-option v-for="c in customers" :key="c.id" :label="`${c.name} · ${c.customerNo}`" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="量体师">
          <el-select v-model="form.tailorName" allow-create filterable default-first-option style="width: 100%">
            <el-option v-for="t in tailors" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="量体日期">
          <el-date-picker v-model="form.measureDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>

      <el-collapse v-model="openPanels">
        <el-collapse-item name="upper" title="上半身 · 胸围 / 肩宽 / 袖长 / 衣长">
          <el-row :gutter="14">
            <el-col :span="12" v-for="p in upperParts" :key="p.key">
              <div class="gf-part">
                <div class="gf-part-label">{{ p.name }}<span class="gf-muted"> cm</span></div>
                <el-input-number v-model="form[p.key]" :min="rangeOf(p.key).min" :max="rangeOf(p.key).max" :step="0.5"
                                 :precision="1" controls-position="right" style="width: 100%" />
              </div>
            </el-col>
          </el-row>
        </el-collapse-item>
        <el-collapse-item name="lower" title="下半身 · 腰围 / 臀围 / 裤长">
          <el-row :gutter="14">
            <el-col :span="12" v-for="p in lowerParts" :key="p.key">
              <div class="gf-part">
                <div class="gf-part-label">{{ p.name }}<span class="gf-muted"> cm</span></div>
                <el-input-number v-model="form[p.key]" :min="rangeOf(p.key).min" :max="rangeOf(p.key).max" :step="0.5"
                                 :precision="1" controls-position="right" style="width: 100%" />
              </div>
            </el-col>
          </el-row>
        </el-collapse-item>
      </el-collapse>

      <el-form label-width="86px" style="margin-top: 14px">
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="版型偏好 / 特殊体型说明" />
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { measurementApi, metaApi, orderApi } from '../api/index.js'
import { day, num, todayStr } from '../utils/format.js'

const dict = reactive({ partRange: {} })
const query = reactive({ customerId: null, tailorName: '' })
const customers = ref([])
const tailors = ref([])
const rows = ref([])
const page = ref(0)
const size = 8
const hasNext = ref(false)
const loading = ref(false)
const saving = ref(false)
const drawer = ref(false)
const openPanels = ref(['upper', 'lower'])

const parts = [
  { key: 'bust', name: '胸围' },
  { key: 'waist', name: '腰围' },
  { key: 'hip', name: '臀围' },
  { key: 'shoulder', name: '肩宽' },
  { key: 'sleeveLength', name: '袖长' },
  { key: 'bodyLength', name: '衣长' },
  { key: 'pantsLength', name: '裤长' }
]
const upperParts = [
  { key: 'bust', name: '胸围' },
  { key: 'shoulder', name: '肩宽' },
  { key: 'sleeveLength', name: '袖长' },
  { key: 'bodyLength', name: '衣长' }
]
const lowerParts = [
  { key: 'waist', name: '腰围' },
  { key: 'hip', name: '臀围' },
  { key: 'pantsLength', name: '裤长' }
]

const form = reactive(blankForm())

function blankForm() {
  return {
    id: null,
    recordNo: '',
    customerId: null,
    tailorName: '方绮',
    measureDate: todayStr(),
    bust: 86,
    waist: 68,
    hip: 92,
    shoulder: 38,
    sleeveLength: 57,
    bodyLength: 62,
    pantsLength: 96,
    remark: ''
  }
}

function rangeOf(key) {
  return dict.partRange[key] || { min: 0, max: 999 }
}

function inRange(row, key) {
  const value = Number(row[key])
  const range = rangeOf(key)
  return value >= Number(range.min) && value <= Number(range.max)
}

function abnormal(row) {
  return parts.some((p) => !inRange(row, p.key))
}

async function fetchPage(reset) {
  if (reset) {
    page.value = 0
    rows.value = []
  }
  loading.value = true
  try {
    const slice = await measurementApi.list({
      customerId: query.customerId || undefined,
      tailorName: query.tailorName || undefined,
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
  form.recordNo = nextNo()
  drawer.value = true
}

function nextNo() {
  return `ME-${4013 + rows.value.length}`
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    recordNo: row.recordNo,
    customerId: row.customerId,
    tailorName: row.tailorName,
    measureDate: day(row.measureDate),
    bust: Number(row.bust),
    waist: Number(row.waist),
    hip: Number(row.hip),
    shoulder: Number(row.shoulder),
    sleeveLength: Number(row.sleeveLength),
    bodyLength: Number(row.bodyLength),
    pantsLength: Number(row.pantsLength),
    remark: row.remark
  })
  drawer.value = true
}

async function submit() {
  saving.value = true
  try {
    await measurementApi.save({ ...form })
    ElMessage.success('量体记录已保存')
    drawer.value = false
    reload()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除量体单「${row.recordNo}」？`, '删除确认', { type: 'warning' })
  await measurementApi.remove(row.id)
  ElMessage.success('已删除')
  reload()
}

onMounted(async () => {
  const meta = await metaApi.dict()
  dict.partRange = meta.partRange
  customers.value = await orderApi.customers()
  tailors.value = await measurementApi.tailors()
  await fetchPage(true)
})
</script>

<style scoped>
.gf-part {
  margin-bottom: 12px;
}

.gf-part-label {
  font-size: 12.5px;
  color: var(--gf-ink-2);
  margin-bottom: 5px;
}
</style>
