<template>
  <div class="gf-page">
    <div class="gf-page-head">
      <div>
        <h2 class="gf-title">工序工单</h2>
        <p class="gf-subtitle">裁剪 / 缝制 / 锁边 / 熨烫 / 试衣 / 钉扣，逐个推进并记录实际工时</p>
      </div>
      <el-button type="primary" @click="openCreate">＋ 派发工单</el-button>
    </div>

    <div class="gf-metric-row">
      <div class="gf-metric">
        <div class="label">工单总数</div>
        <div class="value">{{ stats.total || 0 }}</div>
      </div>
      <div class="gf-metric">
        <div class="label">计划工时合计</div>
        <div class="value">{{ num(stats.plannedHours, 1) }}</div>
      </div>
      <div class="gf-metric">
        <div class="label">实际工时合计</div>
        <div class="value" style="color: var(--gf-primary)">{{ num(stats.actualHours, 1) }}</div>
      </div>
      <div class="gf-metric">
        <div class="label">工时偏差</div>
        <div class="value" :style="{ color: Number(stats.diffHours) > 0 ? 'var(--gf-danger)' : 'var(--gf-ok)' }">
          {{ Number(stats.diffHours) > 0 ? '+' : '' }}{{ num(stats.diffHours, 1) }}
        </div>
      </div>
      <div class="gf-metric">
        <div class="label">返工单数</div>
        <div class="value" style="color: var(--gf-danger)">{{ (stats.statusCount && stats.statusCount.REWORK) || 0 }}</div>
      </div>
    </div>

    <div class="gf-toolbar">
      <el-select v-model="query.orderId" placeholder="全部订单" clearable filterable style="width: 200px"
                 @change="reload">
        <el-option v-for="o in orders" :key="o.id" :label="`${o.orderNo} · ${o.customerName}`" :value="o.id" />
      </el-select>
      <el-select v-model="query.step" placeholder="全部工序" clearable style="width: 130px" @change="reload">
        <el-option v-for="(name, code) in steps" :key="code" :label="name" :value="code" />
      </el-select>
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="reload">
        <el-option v-for="(name, code) in statuses" :key="code" :label="name" :value="code" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="搜工单号 / 订单号 / 客户" clearable style="width: 210px"
                @keyup.enter="reload" />
      <el-button type="primary" plain @click="reload">查询</el-button>
      <span class="spacer" />
      <span class="gf-muted">已加载 {{ rows.length }} 张</span>
    </div>

    <div class="desk-grid">
      <div class="gf-card" style="padding: 6px 6px 10px">
        <el-table :data="rows" border stripe size="small">
          <el-table-column prop="woNo" label="工单号" width="100" />
          <el-table-column label="订单 / 客户" min-width="180">
            <template #default="{ row }">
              <div>{{ row.orderNo }}</div>
              <div class="gf-muted" style="font-size: 12px">{{ row.customerName }} · {{ row.styleName }}</div>
            </template>
          </el-table-column>
          <el-table-column label="工序" width="90">
            <template #default="{ row }">
              <el-tag size="small" effect="plain" type="danger">{{ steps[row.step] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="owner" label="负责人" width="90" />
          <el-table-column label="工时(计划/实际)" width="140" align="center">
            <template #default="{ row }">
              <span class="gf-num">{{ num(row.plannedHours, 1) }}</span>
              <span class="gf-muted"> / </span>
              <b class="gf-num" :style="{ color: overHours(row) ? 'var(--gf-danger)' : 'inherit' }">
                {{ num(row.actualHours, 1) }}
              </b>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="96">
            <template #default="{ row }">
              <el-tag size="small" :type="tagType(row.status)">{{ statuses[row.status] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
          <el-table-column label="操作" width="190" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" :disabled="row.status === 'DONE'" @click="start(row)">开工</el-button>
              <el-button link type="success" :disabled="row.status === 'DONE'" @click="finish(row)">完成</el-button>
              <el-button link type="warning" :disabled="row.status !== 'DONE'" @click="rework(row)">返工</el-button>
              <el-button link type="danger" @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="gf-load-more">
          <el-button v-if="hasNext" :loading="loading" @click="loadMore">加载更多</el-button>
          <span v-else class="gf-muted">没有更多了</span>
        </div>
      </div>

      <div class="gf-card" style="padding: 16px">
        <h3 class="gf-section-title">工时统计</h3>
        <div class="gf-muted" style="font-size: 12.5px; margin-bottom: 10px">
          深色为实际工时，浅色为计划工时
        </div>
        <div class="gf-chart">
          <div v-for="row in stats.byStep || []" :key="row.step" class="gf-chart-row">
            <div class="name">{{ row.stepName }}</div>
            <div class="gf-chart-bars">
              <div class="gf-chart-bar plan">
                <i :style="{ width: barWidth(row.plannedHours) + '%' }" />
              </div>
              <div class="gf-chart-bar" :class="{ real: true, over: Number(row.diffHours) > 0 }">
                <i :style="{ width: barWidth(row.actualHours) + '%' }" />
              </div>
              <div class="gf-muted" style="font-size: 11.5px">
                计划 {{ num(row.plannedHours, 1) }}h · 实际 {{ num(row.actualHours, 1) }}h
                <span :style="{ color: Number(row.diffHours) > 0 ? 'var(--gf-danger)' : 'var(--gf-ok)' }">
                  （{{ Number(row.diffHours) > 0 ? '+' : '' }}{{ num(row.diffHours, 1) }}h）
                </span>
              </div>
            </div>
          </div>
        </div>
        <div v-if="!(stats.byStep || []).length" class="gf-empty">暂无工时数据</div>
      </div>
    </div>

    <div class="gf-card" style="margin-top: 14px; padding: 14px 14px 18px">
      <div style="display:flex; align-items:center; gap:10px; margin-bottom: 10px">
        <h3 class="gf-section-title" style="margin:0">试衣返修工单（独立轮次）</h3>
        <span class="gf-muted" style="font-size: 12.5px">
          由试衣问题单触发，每轮独立记录负责人 / 计划 / 实际工时 / 复核结论，原工序工单的完成事实与历史工时不变
        </span>
      </div>
      <el-table :data="rounds" border stripe size="small">
        <el-table-column prop="roundNo" label="返修单号" width="100" />
        <el-table-column label="订单 / 问题" min-width="170">
          <template #default="{ row }">
            <div>{{ row.orderNo }} · {{ row.issueNo }}</div>
            <div class="gf-muted" style="font-size: 12px">
              {{ row.bodyPartName }}
              <el-tag size="small" :type="row.severity === 'BLOCKER' ? 'danger' : 'info'" style="margin-left:4px">
                {{ row.severityName }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="轮次" width="64">
          <template #default="{ row }">第{{ row.roundSeq }}轮</template>
        </el-table-column>
        <el-table-column label="责任工序" width="86">
          <template #default="{ row }">{{ row.responsibleStepName }}</template>
        </el-table-column>
        <el-table-column prop="owner" label="负责人" width="84" />
        <el-table-column label="计划/实际工时" width="130">
          <template #default="{ row }">
            {{ num(row.plannedHours, 1) }} / <b>{{ num(row.actualHours, 1) }}</b>
          </template>
        </el-table-column>
        <el-table-column label="返修状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="rrTag(row.status)">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="问题单阶段" width="96">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="fiTag(row.issueStatus)">{{ row.issueStatusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reworkReason" label="返修原因" min-width="160" show-overflow-tooltip />
        <el-table-column label="复核结论" min-width="170">
          <template #default="{ row }">
            <span v-if="row.reviewedAt" class="gf-muted" style="font-size: 12px">
              {{ row.reviewer }}：{{ row.reviewComment }}
            </span>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="rounds.length === 0" class="gf-empty">暂无试衣返修工单</div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑工序工单' : '派发工序工单'" width="460px">
      <el-form label-width="100px">
        <el-form-item label="工单号"><el-input v-model="form.woNo" placeholder="如 WO-0046" /></el-form-item>
        <el-form-item label="所属订单">
          <el-select v-model="form.orderId" filterable clearable placeholder="按订单号 / 客户搜索" style="width: 100%">
            <el-option v-for="o in orders" :key="o.id" :label="`${o.orderNo} · ${o.customerName} · ${o.styleName}`"
                       :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="工序">
          <el-select v-model="form.step" style="width: 100%">
            <el-option v-for="(name, code) in steps" :key="code" :label="name" :value="code" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人"><el-input v-model="form.owner" /></el-form-item>
        <el-form-item label="计划工时">
          <el-input-number v-model="form.plannedHours" :min="0.5" :max="100" :step="0.5" :precision="1"
                           style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { workOrderApi, orderApi, metaApi, fittingApi } from '../api/index.js'
import { num } from '../utils/format.js'

const query = reactive({ orderId: null, step: '', status: '', keyword: '' })
const steps = reactive({})
const statuses = reactive({})
const orders = ref([])
const rows = ref([])
const page = ref(0)
const size = 10
const hasNext = ref(false)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const stats = reactive({ total: 0, plannedHours: 0, actualHours: 0, diffHours: 0, statusCount: {}, byStep: [] })
const rounds = ref([])

function rrTag(status) {
  if (status === 'PASSED') return 'success'
  if (status === 'REJECTED') return 'danger'
  if (status === 'DONE') return 'warning'
  return 'primary'
}

function fiTag(status) {
  if (status === 'CLOSED') return 'success'
  if (status === 'REVIEWING') return 'warning'
  if (status === 'REWORKING') return 'danger'
  return 'info'
}

async function loadRounds() {
  rounds.value = await fittingApi.rounds({ orderId: query.orderId || undefined })
}

const form = reactive({
  id: null,
  woNo: '',
  orderId: null,
  step: 'SEWING',
  owner: '王姐',
  plannedHours: 4,
  remark: ''
})

const maxHours = computed(() => {
  let max = 1
  ;(stats.byStep || []).forEach((row) => {
    max = Math.max(max, Number(row.plannedHours || 0), Number(row.actualHours || 0))
  })
  return max
})

function barWidth(value) {
  return Math.min(100, Math.round((Number(value || 0) / maxHours.value) * 100))
}

function overHours(row) {
  return Number(row.actualHours) > Number(row.plannedHours)
}

function tagType(status) {
  if (status === 'DONE') return 'success'
  if (status === 'RUNNING') return 'primary'
  if (status === 'REWORK') return 'danger'
  return 'info'
}

async function fetchPage(reset) {
  if (reset) {
    page.value = 0
    rows.value = []
  }
  loading.value = true
  try {
    const slice = await workOrderApi.list({
      orderId: query.orderId || undefined,
      step: query.step || undefined,
      status: query.status || undefined,
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

async function loadStats() {
  const data = await workOrderApi.stats({ orderId: query.orderId || undefined })
  Object.assign(stats, data)
}

async function loadOrders() {
  const slice = await orderApi.list({ size: 50 })
  orders.value = slice.content
}

function reload() {
  fetchPage(true)
  loadStats()
  loadRounds()
}

function loadMore() {
  page.value += 1
  fetchPage(false)
}

function openCreate() {
  Object.assign(form, { id: null, woNo: '', orderId: null, step: 'SEWING', owner: '王姐', plannedHours: 4, remark: '' })
  dialogVisible.value = true
}

async function submit() {
  saving.value = true
  try {
    await workOrderApi.save({ ...form })
    ElMessage.success('工序工单已保存')
    dialogVisible.value = false
    reload()
  } finally {
    saving.value = false
  }
}

async function start(row) {
  await workOrderApi.advance(row.id, { targetStatus: 'RUNNING' })
  ElMessage.success(`${row.woNo} 已开工`)
  reload()
}

async function finish(row) {
  const { value } = await ElMessageBox.prompt(
    `${row.woNo} 实际工时（小时），计划 ${num(row.plannedHours, 1)} 小时`,
    '标记完成',
    { inputPattern: /^\d+(\.\d+)?$/, inputErrorMessage: '请填写数字', inputValue: String(row.plannedHours) }
  )
  await workOrderApi.advance(row.id, { targetStatus: 'DONE', actualHours: Number(value) })
  ElMessage.success(`${row.woNo} 已完成`)
  reload()
}

async function rework(row) {
  const { value } = await ElMessageBox.prompt(`${row.woNo} 返工原因`, '判返工', {
    inputType: 'textarea',
    inputPlaceholder: '如：门襟不平整，需重新熨烫',
    inputValidator: (v) => (v && v.trim().length > 0) || '返工原因不能为空'
  })
  await workOrderApi.advance(row.id, { targetStatus: 'REWORK', remark: value })
  ElMessage.success(`${row.woNo} 已判返工`)
  reload()
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除工单「${row.woNo}」？`, '删除确认', { type: 'warning' })
  await workOrderApi.remove(row.id)
  ElMessage.success('已删除')
  reload()
}

onMounted(async () => {
  const dict = await metaApi.dict()
  Object.assign(steps, dict.steps)
  Object.assign(statuses, dict.woStatuses)
  await loadOrders()
  await fetchPage(true)
  await loadStats()
  await loadRounds()
})
</script>

<style scoped>
.desk-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 14px;
  align-items: start;
}

@media (max-width: 1180px) {
  .desk-grid {
    grid-template-columns: 1fr;
  }
}
</style>
