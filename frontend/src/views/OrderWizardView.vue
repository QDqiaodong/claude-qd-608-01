<template>
  <div class="gf-page">
    <div class="gf-page-head">
      <div>
        <h2 class="gf-title">向导式下单</h2>
        <p class="gf-subtitle">选客户与款式 → 录量体尺寸 → 选面料 → 确认订单</p>
      </div>
      <el-tag effect="plain" type="danger" size="small">第 {{ step + 1 }} / 4 步</el-tag>
    </div>

    <div class="gf-card wizard-steps">
      <el-steps :active="step" finish-status="success" align-center>
        <el-step title="客户与款式" :description="customer && style ? `${customer.name} · ${style.name}` : '待选择'" />
        <el-step title="量体尺寸" :description="step2Desc" />
        <el-step title="面料选配" :description="fabric ? fabric.name : '待选择'" />
        <el-step title="确认订单" description="算价 · 定金 · 交期" />
      </el-steps>
    </div>

    <!-- 第 1 步：客户 + 款式 -->
    <section v-show="step === 0">
      <div class="gf-card" style="padding: 16px; margin-bottom: 14px">
        <h3 class="gf-section-title">① 选择客户</h3>
        <div class="pick-row">
          <div
            v-for="c in customers"
            :key="c.id"
            class="pick-card"
            :class="{ active: customer && customer.id === c.id }"
            @click="pickCustomer(c)"
          >
            <div class="pick-title">{{ c.name }}</div>
            <div class="pick-sub">{{ c.customerNo }} · {{ c.phone }}</div>
            <div class="pick-foot">
              <el-tag size="small" :type="c.vipLevel === 'GOLD' ? 'danger' : 'info'">{{ vipName(c.vipLevel) }}</el-tag>
              <span class="gf-muted">历史 {{ c.orderCount }} 单 / 量体 {{ c.measureCount }} 次</span>
            </div>
          </div>
        </div>
      </div>

      <div class="gf-card" style="padding: 16px">
        <h3 class="gf-section-title">
          ② 选择款式
          <el-radio-group v-model="categoryFilter" size="small" style="margin-left: 12px" @change="loadStyles">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button v-for="(name, code) in dict.categories" :key="code" :label="code">{{ name }}</el-radio-button>
          </el-radio-group>
        </h3>
        <div class="pick-row">
          <div
            v-for="s in styles"
            :key="s.id"
            class="pick-card"
            :class="{ active: style && style.id === s.id, disabled: !s.onSale }"
            @click="pickStyle(s)"
          >
            <div class="pick-title">{{ s.name }}</div>
            <div class="pick-sub">{{ s.styleNo }} · {{ dict.categories[s.category] }}</div>
            <div class="pick-foot">
              <span class="gf-money">¥{{ yuan(s.basePrice) }}</span>
              <span class="gf-muted">工艺 {{ s.craftLevel }} 级</span>
              <el-tag v-if="!s.onSale" size="small" type="info">停售</el-tag>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 第 2 步：量体 -->
    <section v-show="step === 1">
      <div class="gf-card" style="padding: 16px">
        <h3 class="gf-section-title">量体尺寸</h3>
        <el-tabs v-model="measureMode">
          <el-tab-pane label="复用历史量体" name="history">
            <div v-if="historyList.length === 0" class="gf-empty">该客户暂无历史量体记录，请切换到「现场录入」</div>
            <el-radio-group v-else v-model="order.measurementId" class="msr-radio">
              <el-radio v-for="m in historyList" :key="m.id" :value="m.id" class="msr-item">
                <div class="msr-line">
                  <b>{{ m.recordNo }}</b>
                  <span class="gf-muted">{{ day(m.measureDate) }} · {{ m.tailorName }}</span>
                </div>
                <div class="msr-nums">
                  胸 {{ num(m.bust) }} · 腰 {{ num(m.waist) }} · 臀 {{ num(m.hip) }} · 肩 {{ num(m.shoulder) }} ·
                  袖长 {{ num(m.sleeveLength) }} · 衣长 {{ num(m.bodyLength) }} · 裤长 {{ num(m.pantsLength) }}
                </div>
              </el-radio>
            </el-radio-group>
          </el-tab-pane>
          <el-tab-pane label="现场录入" name="new">
            <el-form label-width="86px" style="max-width: 620px">
              <el-row :gutter="12">
                <el-col :span="8">
                  <el-form-item label="量体单号"><el-input v-model="msr.recordNo" /></el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="量体师">
                    <el-select v-model="msr.tailorName" allow-create filterable default-first-option>
                      <el-option v-for="t in tailors" :key="t" :label="t" :value="t" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="量体日期">
                    <el-date-picker v-model="msr.measureDate" type="date" value-format="YYYY-MM-DD"
                                    style="width: 100%" />
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>

            <el-collapse v-model="msrPanels">
              <el-collapse-item name="upper" title="上身部位">
                <el-row :gutter="12">
                  <el-col :span="6" v-for="p in upperParts" :key="p.key">
                    <div class="gf-part">
                      <div class="gf-part-label">{{ p.name }}<span class="gf-muted"> cm</span></div>
                      <el-input-number v-model="msr[p.key]" :min="partMin(p.key)" :max="partMax(p.key)" :step="0.5"
                                       :precision="1" controls-position="right" style="width: 100%" />
                    </div>
                  </el-col>
                </el-row>
              </el-collapse-item>
              <el-collapse-item name="lower" title="下身部位">
                <el-row :gutter="12">
                  <el-col :span="8" v-for="p in lowerParts" :key="p.key">
                    <div class="gf-part">
                      <div class="gf-part-label">{{ p.name }}<span class="gf-muted"> cm</span></div>
                      <el-input-number v-model="msr[p.key]" :min="partMin(p.key)" :max="partMax(p.key)" :step="0.5"
                                       :precision="1" controls-position="right" style="width: 100%" />
                    </div>
                  </el-col>
                </el-row>
              </el-collapse-item>
            </el-collapse>
            <el-input v-model="msr.remark" type="textarea" :rows="2" placeholder="版型备注"
                      style="margin-top: 12px" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </section>

    <!-- 第 3 步：面料 -->
    <section v-show="step === 2">
      <div class="gf-toolbar">
        <el-radio-group v-model="fabricComposition" size="small" @change="loadFabrics(true)">
          <el-radio-button label="">全部成分</el-radio-button>
          <el-radio-button v-for="(name, code) in dict.compositions" :key="code" :label="code">{{ name }}</el-radio-button>
        </el-radio-group>
        <el-input v-model="fabricKeyword" placeholder="搜面料名称 / 供应商" clearable style="width: 200px"
                  @keyup.enter="loadFabrics(true)" />
        <span class="spacer" />
        <span class="gf-muted">已选 {{ fabric ? fabric.name : '未选择' }}</span>
      </div>

      <div class="gf-wall">
        <div
          v-for="item in fabrics"
          :key="item.id"
          class="gf-fabric-card"
          :class="{ selected: fabric && fabric.id === item.id, low: item.lowStock }"
          @click="pickFabric(item)"
        >
          <div class="strip" :style="{ background: item.colorHex }" />
          <div class="body">
            <div class="top">
              <div class="fname">{{ item.name }}</div>
              <div class="fno">{{ item.fabricNo }}</div>
            </div>
            <div class="meta">
              <el-tag size="small" effect="plain" type="danger">{{ dict.compositions[item.composition] }}</el-tag>
              <span>{{ item.color }}</span>
            </div>
            <div class="gf-stock-bar" :class="{ low: item.lowStock }">
              <i :style="{ width: fabricPercent(item) + '%' }" />
            </div>
            <div class="gf-stock-line">
              <span :class="{ low: item.lowStock }">库存 {{ num(item.stockMeters, 1) }} 米</span>
              <span class="gf-money">¥{{ yuan(item.unitPrice) }}/米</span>
            </div>
            <div class="gf-muted" style="margin-top: 4px; font-size: 12px">{{ item.supplier }}</div>
          </div>
        </div>
      </div>
      <div class="gf-load-more">
        <el-button v-if="fabricHasNext" @click="loadFabrics(false)">加载更多</el-button>
        <span v-else class="gf-muted">没有更多了</span>
      </div>
    </section>

    <!-- 第 4 步：确认订单 -->
    <section v-show="step === 3">
      <div class="confirm-grid">
        <div class="gf-card" style="padding: 18px">
          <h3 class="gf-section-title">用量与交期</h3>
          <el-form label-width="110px">
            <el-form-item label="面料用量(米)">
              <el-input-number v-model="order.usageMeters" :min="0.1" :max="20" :step="0.1" :precision="1"
                               style="width: 100%" @change="calcPrice" />
              <div class="gf-muted" style="font-size: 12px">
                {{ style ? dict.categories[style.category] : '' }}推荐用料
                {{ style ? recommendedMeters : '-' }} 米；单次最多 {{ dict.maxUsage }} 米
              </div>
            </el-form-item>
            <el-form-item label="交期">
              <el-date-picker v-model="order.deliveryDate" type="date" value-format="YYYY-MM-DD"
                              style="width: 100%" @change="calcPrice" />
              <div class="gf-muted" style="font-size: 12px">最短工期 {{ dict.minLeadDays }} 天</div>
            </el-form-item>
            <el-form-item label="已收定金">
              <el-switch v-model="order.payDeposit" active-text="已收" inactive-text="待收" />
            </el-form-item>
            <el-form-item label="订单备注">
              <el-input v-model="order.remark" type="textarea" :rows="2" />
            </el-form-item>
          </el-form>
        </div>

        <div class="gf-card bill">
          <div class="bill-head">定制订单核算单</div>
          <div class="bill-line">
            <span>客户</span><b>{{ customer?.name || '-' }}</b>
          </div>
          <div class="bill-line">
            <span>款式</span><b>{{ style?.name || '-' }}</b>
          </div>
          <div class="bill-line">
            <span>面料</span><b>{{ fabric?.name || '-' }}</b>
          </div>
          <div class="bill-line">
            <span>量体单</span><b>{{ selectedMeasureNo }}</b>
          </div>
          <el-divider style="margin: 12px 0" />
          <div class="bill-line">
            <span>面料费用（{{ num(order.usageMeters, 1) }} 米 × ¥{{ yuan(preview.unitPrice) }}）</span>
            <b class="gf-money">¥{{ yuan(preview.fabricCost) }}</b>
          </div>
          <div class="bill-line">
            <span>工艺基础价</span><b class="gf-money">¥{{ yuan(preview.basePrice) }}</b>
          </div>
          <div class="bill-line total">
            <span>总价</span><b>¥{{ yuan(preview.totalPrice) }}</b>
          </div>
          <div class="bill-split">
            <div>
              <div class="label">定金 50%</div>
              <div class="value">¥{{ yuan(preview.deposit) }}</div>
            </div>
            <div>
              <div class="label">尾款 50%</div>
              <div class="value">¥{{ yuan(preview.balance) }}</div>
            </div>
          </div>
          <div class="gf-muted" style="font-size: 12px; margin-top: 10px">
            交期 {{ order.deliveryDate || '-' }}（距今 {{ leadDays }} 天）
          </div>
          <el-button type="primary" size="large" style="width: 100%; margin-top: 14px" :loading="submitting"
                     @click="submit">
            提交订单并投产
          </el-button>
        </div>
      </div>
    </section>

    <div class="wizard-foot">
      <el-button v-if="step > 0" @click="prev">上一步</el-button>
      <span class="spacer" />
      <el-button v-if="step < 3" type="primary" :disabled="!canNext" @click="next">下一步</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { metaApi, orderApi, styleApi, fabricApi, measurementApi } from '../api/index.js'
import { yuan, num, day, todayStr, addDaysStr, daysFromToday } from '../utils/format.js'

const router = useRouter()
const dict = reactive({ categories: {}, compositions: {}, partRange: {}, maxUsage: 20, minLeadDays: 7 })
const defaultMeters = reactive({})

const step = ref(0)
const customers = ref([])
const styles = ref([])
const fabrics = ref([])
const tailors = ref([])
const historyList = ref([])

const customer = ref(null)
const style = ref(null)
const fabric = ref(null)
const measureMode = ref('history')
const msrPanels = ref(['upper', 'lower'])
const categoryFilter = ref('')
const fabricComposition = ref('')
const fabricKeyword = ref('')
const fabricHasNext = ref(false)
const fabricPage = ref(0)
const submitting = ref(false)

const order = reactive({
  measurementId: null,
  usageMeters: 3.6,
  deliveryDate: addDaysStr(21),
  payDeposit: true,
  remark: ''
})

const msr = reactive({
  recordNo: '',
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
})

const preview = reactive({
  unitPrice: null,
  fabricCost: null,
  basePrice: null,
  totalPrice: null,
  deposit: null,
  balance: null
})

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

const vipMap = { GOLD: '金卡', SILVER: '银卡', NORMAL: '普通' }
const vipName = (level) => vipMap[level] || level
const partMin = (key) => Number(dict.partRange[key]?.min ?? 0)
const partMax = (key) => Number(dict.partRange[key]?.max ?? 200)

const recommendedMeters = computed(() =>
  style.value ? defaultMeters[style.value.category] || 3.6 : '-'
)

const selectedMeasureNo = computed(() => {
  if (measureMode.value === 'history') {
    const hit = historyList.value.find((m) => m.id === order.measurementId)
    return hit ? hit.recordNo : '未选择'
  }
  return msr.recordNo || '现场录入'
})

const leadDays = computed(() => daysFromToday(order.deliveryDate))
const step2Desc = computed(() => {
  if (measureMode.value === 'history') {
    const hit = historyList.value.find((m) => m.id === order.measurementId)
    return hit ? `${hit.recordNo} · 复用历史` : '待选择量体记录'
  }
  return msr.recordNo ? `${msr.recordNo} · 现场录入` : '现场录入'
})

const canNext = computed(() => {
  if (step.value === 0) return !!customer.value && !!style.value && style.value.onSale
  if (step.value === 1) return measureMode.value === 'history' ? !!order.measurementId : !!msr.recordNo
  if (step.value === 2) return !!fabric.value
  return true
})

async function loadDict() {
  const meta = await metaApi.dict()
  dict.categories = meta.categories
  dict.compositions = meta.compositions
  dict.maxUsage = meta.maxUsage
  dict.minLeadDays = meta.minLeadDays
  dict.partRange = meta.partRange
  const styleMeta = await styleApi.meta()
  Object.assign(defaultMeters, styleMeta.defaultMeters || {})
}

async function loadStyles() {
  const slice = await styleApi.list({ category: categoryFilter.value || undefined, size: 24 })
  styles.value = slice.content.filter((s) => s.onSale)
}

async function loadFabrics(reset) {
  if (reset) fabricPage.value = 0
  const slice = await fabricApi.list({
    composition: fabricComposition.value || undefined,
    keyword: fabricKeyword.value || undefined,
    page: fabricPage.value,
    size: 8
  })
  fabrics.value = reset ? slice.content : fabrics.value.concat(slice.content)
  fabricHasNext.value = slice.hasNext
}

function fabricPercent(item) {
  const max = fabrics.value.reduce((m, f) => Math.max(m, Number(f.stockMeters || 0)), 1)
  return Math.min(100, Math.round((Number(item.stockMeters || 0) / max) * 100))
}

async function pickCustomer(c) {
  customer.value = c
  const list = await measurementApi.byCustomer(c.id)
  historyList.value = list
  if (list.length > 0) {
    order.measurementId = list[0].id
    measureMode.value = 'history'
  } else {
    order.measurementId = null
    measureMode.value = 'new'
  }
  msr.recordNo = `ME-${4013 + Math.floor(Math.random() * 500)}`
}

async function pickStyle(s) {
  if (!s.onSale) {
    ElMessage.warning('该款式已停售，请选择其它款式')
    return
  }
  style.value = s
  order.usageMeters = defaultMeters[s.category] || 3.6
}

async function pickFabric(item) {
  fabric.value = item
  await calcPrice()
}

async function calcPrice() {
  if (!style.value || !fabric.value) return
  try {
    const data = await orderApi.preview({
      styleId: style.value.id,
      fabricId: fabric.value.id,
      usageMeters: order.usageMeters,
      deliveryDate: order.deliveryDate
    })
    Object.assign(preview, data)
  } catch (e) {
    /* 校验失败时保持上一次结果 */
  }
}

function next() {
  if (step.value === 2) {
    calcPrice()
  }
  step.value += 1
}

function prev() {
  step.value -= 1
}

async function submit() {
  submitting.value = true
  try {
    const payload = {
      customerId: customer.value.id,
      styleId: style.value.id,
      fabricId: fabric.value.id,
      usageMeters: order.usageMeters,
      deliveryDate: order.deliveryDate,
      payDeposit: order.payDeposit,
      remark: order.remark,
      measurementId: measureMode.value === 'history' ? order.measurementId : null,
      measurement: measureMode.value === 'new' ? { ...msr, customerId: customer.value.id } : null
    }
    const created = await orderApi.create(payload)
    ElMessage.success(`订单 ${created.orderNo} 已建立`)
    router.push('/board')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadDict()
  customers.value = await orderApi.customers()
  tailors.value = await measurementApi.tailors()
  await loadStyles()
  await loadFabrics(true)
})
</script>

<style scoped>
.wizard-steps {
  padding: 20px 14px 8px;
  margin-bottom: 16px;
}

.pick-row {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(228px, 1fr));
  gap: 12px;
}

.pick-card {
  border: 1px solid var(--gf-line);
  border-radius: 12px;
  padding: 12px 13px;
  cursor: pointer;
  background: #fff;
  transition: all 0.18s ease;
}

.pick-card:hover {
  border-color: var(--el-color-primary-light-5);
  box-shadow: 0 6px 16px rgba(173, 20, 87, 0.1);
}

.pick-card.active {
  border-color: var(--gf-primary);
  background: var(--gf-primary-soft);
}

.pick-card.disabled {
  opacity: 0.55;
}

.pick-title {
  font-size: 15px;
  font-weight: 600;
}

.pick-sub {
  margin-top: 3px;
  font-size: 12px;
  color: var(--gf-ink-3);
}

.pick-foot {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
}

.msr-radio {
  display: block;
  width: 100%;
}

.msr-item {
  display: block;
  border: 1px solid var(--gf-line);
  border-radius: 10px;
  padding: 10px 12px;
  margin: 0 0 10px;
  width: 100%;
  height: auto;
}

.msr-item.is-checked {
  border-color: var(--gf-primary);
  background: var(--gf-primary-soft);
}

.msr-line {
  display: flex;
  gap: 10px;
  align-items: baseline;
  font-size: 13.5px;
}

.msr-nums {
  margin-top: 4px;
  font-size: 12.5px;
  color: var(--gf-ink-2);
}

.gf-part {
  margin-bottom: 12px;
}

.gf-part-label {
  font-size: 12.5px;
  color: var(--gf-ink-2);
  margin-bottom: 5px;
}

.confirm-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 14px;
}

.bill {
  padding: 18px;
  align-self: flex-start;
}

.bill-head {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 1px;
  color: var(--gf-primary);
  margin-bottom: 12px;
}

.bill-line {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 13.5px;
  margin-bottom: 8px;
}

.bill-line span {
  color: var(--gf-ink-3);
}

.bill-line.total {
  font-size: 17px;
}

.bill-line.total b {
  color: var(--gf-primary);
}

.bill-split {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}

.bill-split > div {
  flex: 1;
  background: var(--gf-primary-soft);
  border-radius: 10px;
  padding: 10px 12px;
}

.bill-split .label {
  font-size: 12px;
  color: var(--gf-ink-3);
}

.bill-split .value {
  font-size: 16px;
  font-weight: 600;
  color: var(--gf-primary);
  margin-top: 2px;
}

.wizard-foot {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 18px;
}

.wizard-foot .spacer {
  flex: 1;
}

@media (max-width: 1080px) {
  .confirm-grid {
    grid-template-columns: 1fr;
  }
}
</style>
