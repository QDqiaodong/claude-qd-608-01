<template>
  <div class="gf-page">
    <div class="gf-page-head">
      <div>
        <h2 class="gf-title">订单进度看板</h2>
        <p class="gf-subtitle">横向看板：按状态分列，点开卡片可推进工序与查看工单</p>
      </div>
      <el-button type="primary" plain @click="load">刷新看板</el-button>
    </div>

    <div class="gf-metric-row">
      <div class="gf-metric">
        <div class="label">在产订单</div>
        <div class="value">{{ meta.total }}</div>
      </div>
      <div class="gf-metric">
        <div class="label">本月待交付</div>
        <div class="value">{{ dueSoon }}</div>
      </div>
      <div class="gf-metric">
        <div class="label">合同金额（元）</div>
        <div class="value" style="color: var(--gf-primary)">¥{{ yuan(totalAmount) }}</div>
      </div>
      <div class="gf-metric">
        <div class="label">尾款未结</div>
        <div class="value" style="color: var(--gf-danger)">{{ unpaidCount }}</div>
      </div>
    </div>

    <div class="gf-board">
      <div v-for="(cards, status) in columns" :key="status" class="gf-board-col">
        <div class="gf-board-col-head">
          <div class="title">{{ statusNames[status] }}</div>
          <div class="count">{{ cards.length }}</div>
        </div>
        <div v-if="cards.length === 0" class="gf-empty" style="padding: 16px">暂无</div>
        <div v-for="card in cards" :key="card.id" class="gf-board-card" @click="open(card)">
          <div class="row1">
            <i class="gf-swatch" :style="{ background: card.colorHex }" />
            <span class="cust">{{ card.customerName }}</span>
            <span class="ono">{{ card.orderNo }}</span>
          </div>
          <div class="style">{{ card.styleName }} · {{ card.fabricName }}</div>
          <div class="style gf-muted">
            用量 {{ num(card.usageMeters, 1) }} 米 · 交期 {{ day(card.deliveryDate) }}
          </div>
          <div class="gf-mini-bar">
            <i :style="{ width: progress(card) + '%' }" />
          </div>
          <div style="margin-top: 7px; display: flex; gap: 6px; align-items: center">
            <span class="gf-money">¥{{ yuan(card.totalPrice) }}</span>
            <span class="gf-muted" style="font-size: 12px">
              工序 {{ card.workOrderDone }}/{{ card.workOrderTotal }}
            </span>
            <el-tag v-if="!card.balancePaid" size="small" type="warning" style="margin-left: auto">尾款未结</el-tag>
          </div>
          <div v-if="card.fitting && Number(card.fitting.issueTotal) > 0" class="fit-line">
            <el-tag size="small" :type="fitTag(card.fitting)" effect="plain">
              试衣整改 · {{ card.fitting.currentStageName }}
            </el-tag>
            <span v-if="Number(card.fitting.openBlockerCount) > 0" class="fit-danger">
              阻断×{{ card.fitting.openBlockerCount }}
            </span>
            <span v-else-if="Number(card.fitting.activeRoundCount) > 0" class="fit-warn">
              返修×{{ card.fitting.activeRoundCount }}
            </span>
            <span v-else-if="Number(card.fitting.openCount) > 0" class="fit-muted">
              未关闭×{{ card.fitting.openCount }}
            </span>
            <span v-else class="fit-ok">问题均已关闭</span>
          </div>
        </div>
      </div>
    </div>

    <el-drawer v-model="visible" :title="`${current?.orderNo || ''} · 工序工单`" size="560px">
      <template v-if="current">
        <div class="gf-card" style="padding: 14px; margin-bottom: 14px">
          <div style="display: flex; gap: 10px; align-items: center">
            <i class="gf-swatch" :style="{ background: current.colorHex }" />
            <div>
              <div style="font-weight: 600">{{ current.customerName }} · {{ current.styleName }}</div>
              <div class="gf-muted" style="font-size: 12.5px">
                {{ current.fabricName }} · 用量 {{ num(current.usageMeters, 1) }} 米 · 量体师
                {{ current.tailorName || '—' }}
              </div>
            </div>
            <span class="spacer" style="flex: 1" />
            <span class="gf-money">¥{{ yuan(current.totalPrice) }}</span>
          </div>
          <div style="margin-top: 12px; display: flex; gap: 8px; align-items: center">
            <el-tag size="small" type="danger" effect="plain">{{ statusNames[current.status] }}</el-tag>
            <span class="gf-muted" style="font-size: 12.5px">交期 {{ day(current.deliveryDate) }}</span>
            <el-button size="small" type="primary" plain @click="advanceOrder" style="margin-left: auto">
              推进订单状态
            </el-button>
            <el-button v-if="!current.balancePaid" size="small" type="success" plain @click="payBalance">
              结清尾款
            </el-button>
          </div>
        </div>

        <el-table :data="workOrders" border size="small">
          <el-table-column prop="woNo" label="工单号" width="96" />
          <el-table-column label="工序" width="90">
            <template #default="{ row }">{{ stepNames[row.step] }}</template>
          </el-table-column>
          <el-table-column prop="owner" label="负责人" width="90" />
          <el-table-column label="计划/实际" width="120">
            <template #default="{ row }">
              {{ num(row.plannedHours, 1) }} / <b>{{ num(row.actualHours, 1) }}</b>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="96">
            <template #default="{ row }">
              <el-tag size="small" :type="woTag(row.status)">{{ woNames[row.status] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="150">
            <template #default="{ row }">
              <el-button link type="primary" @click="start(row)">开工</el-button>
              <el-button link type="success" @click="finish(row)">完成</el-button>
              <el-button link type="warning" @click="rework(row)">返工</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="workOrders.length === 0" class="gf-empty">该订单暂无工序工单</div>

        <template v-if="current.status === 'FITTING' || fittingIssues.length">
          <div style="display:flex; align-items:center; gap:8px; margin: 16px 0 8px">
            <h3 class="gf-section-title" style="margin:0">试衣整改</h3>
            <span class="gf-muted" style="font-size: 12.5px">
              未关闭 {{ fittingSummary.openCount || 0 }} · 阻断 {{ fittingSummary.openBlockerCount || 0 }}
              · 返修工单 {{ fittingSummary.activeRoundCount || 0 }} · {{ fittingSummary.currentStageName || '暂无试衣问题' }}
            </span>
          </div>
          <el-table :data="fittingIssues" border size="small" style="margin-bottom: 10px">
            <el-table-column prop="issueNo" label="问题单号" width="92" />
            <el-table-column label="部位/程度" width="120">
              <template #default="{ row }">
                {{ bodyParts[row.bodyPart] }}
                <el-tag size="small" :type="row.severity === 'BLOCKER' ? 'danger' : 'info'" style="margin-left:4px">
                  {{ row.severity === 'BLOCKER' ? '阻断' : '普通' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述" min-width="140" show-overflow-tooltip />
            <el-table-column label="阶段" width="92">
              <template #default="{ row }">
                <el-tag size="small" :type="fiTag(row.status)">{{ issueNames[row.status] }}</el-tag>
                <el-tag v-if="row.released" size="small" type="warning" effect="plain">已放行</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-table v-if="reworkRounds.length" :data="reworkRounds" border size="small">
            <el-table-column prop="roundNo" label="返修单" width="92" />
            <el-table-column label="轮次" width="60">
              <template #default="{ row }">第{{ row.roundSeq }}轮</template>
            </el-table-column>
            <el-table-column prop="owner" label="负责人" width="72" />
            <el-table-column label="工时" width="90">
              <template #default="{ row }">{{ num(row.plannedHours, 1) }} / {{ num(row.actualHours, 1) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="92">
              <template #default="{ row }">
                <el-tag size="small" :type="rrTag(row.status)">{{ row.statusName }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reworkReason" label="返修原因 / 复核结论" min-width="150" show-overflow-tooltip />
          </el-table>
        </template>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { orderApi, workOrderApi, metaApi, fittingApi } from '../api/index.js'
import { yuan, num, day, daysFromToday } from '../utils/format.js'

const columns = reactive({})
const statusNames = reactive({})
const woNames = reactive({})
const stepNames = reactive({})
const issueNames = reactive({})
const bodyParts = reactive({})
const meta = reactive({ total: 0 })
const allCards = ref([])
const workOrders = ref([])
const fittingIssues = ref([])
const reworkRounds = ref([])
const fittingSummary = ref({})
const visible = ref(false)
const current = ref(null)

const dueSoon = computed(
  () => allCards.value.filter((c) => {
    const d = daysFromToday(c.deliveryDate)
    return d !== null && d <= 14
  }).length
)
const totalAmount = computed(() =>
  allCards.value.reduce((sum, c) => sum + Number(c.totalPrice || 0), 0)
)
const unpaidCount = computed(() => allCards.value.filter((c) => !c.balancePaid).length)

function progress(card) {
  if (!card.workOrderTotal) return 0
  return Math.round((card.workOrderDone / card.workOrderTotal) * 100)
}

function woTag(status) {
  if (status === 'DONE') return 'success'
  if (status === 'RUNNING') return 'primary'
  if (status === 'REWORK') return 'danger'
  return 'info'
}

function fiTag(status) {
  if (status === 'CLOSED') return 'success'
  if (status === 'REVIEWING') return 'warning'
  if (status === 'REWORKING') return 'danger'
  return 'info'
}

function rrTag(status) {
  if (status === 'PASSED') return 'success'
  if (status === 'REJECTED') return 'danger'
  if (status === 'DONE') return 'warning'
  return 'primary'
}

function fitTag(fit) {
  if (Number(fit.openBlockerCount) > 0 || Number(fit.activeRoundCount) > 0) return 'danger'
  if (Number(fit.openCount) > 0) return 'warning'
  return 'success'
}

async function load() {
  const board = await orderApi.board()
  Object.keys(columns).forEach((key) => delete columns[key])
  allCards.value = []
  Object.assign(statusNames, board.meta.columns)
  Object.entries(board.columns).forEach(([status, cards]) => {
    columns[status] = cards
    allCards.value = allCards.value.concat(cards)
  })
  meta.total = board.meta.total
}

async function open(card) {
  current.value = card
  visible.value = true
  await Promise.all([loadWorkOrders(), loadFitting()])
}

async function loadWorkOrders() {
  const slice = await workOrderApi.list({ orderId: current.value.id, size: 50 })
  workOrders.value = slice.content
}

async function loadFitting() {
  const [issueSlice, rounds, summary] = await Promise.all([
    fittingApi.list({ orderId: current.value.id, size: 50 }),
    fittingApi.rounds({ orderId: current.value.id }),
    fittingApi.orderSummary(current.value.id)
  ])
  fittingIssues.value = issueSlice.content
  reworkRounds.value = rounds
  fittingSummary.value = summary
}

async function reloadCardAfterAction() {
  await Promise.all([loadWorkOrders(), loadFitting()])
  const fresh = await orderApi.board()
  const found = Object.values(fresh.columns).flat().find((c) => c.id === current.value.id)
  if (found) current.value = found
  await load()
}

async function act(row, targetStatus, extra = {}) {
  try {
    await workOrderApi.advance(row.id, { targetStatus, ...extra })
    ElMessage.success(`${row.woNo} 已${woNames[targetStatus]}`)
    await reloadCardAfterAction()
  } catch (e) {
    /* 拦截器已提示 */
  }
}

async function start(row) {
  await act(row, 'RUNNING')
}

async function finish(row) {
  const { value } = await ElMessageBox.prompt(
    `${row.woNo} 实际工时（小时），计划 ${num(row.plannedHours, 1)} 小时`,
    '标记完成',
    { inputPattern: /^\d+(\.\d+)?$/, inputErrorMessage: '请填写数字', inputValue: String(row.plannedHours) }
  )
  await act(row, 'DONE', { actualHours: Number(value) })
}

async function rework(row) {
  const { value } = await ElMessageBox.prompt(`${row.woNo} 返工原因`, '判返工', {
    inputType: 'textarea',
    inputPlaceholder: '如：门襟不平整，需重新熨烫',
    inputValidator: (v) => (v && v.trim().length > 0) || '返工原因不能为空'
  })
  await act(row, 'REWORK', { remark: value })
}

async function advanceOrder() {
  try {
    const payload = {}
    // 试衣 → 已完成：先取整改汇总，普通问题未关闭必须显式确认放行
    if (current.value.status === 'FITTING') {
      const summary = fittingSummary.value
      if (summary.finishBlocked) {
        await ElMessageBox.alert(summary.blockingReason, '无法完工：试衣整改未收口', { type: 'error' })
        return
      }
      const unreleased = fittingIssues.value.filter(
        (i) => i.status !== 'CLOSED' && !i.released
      )
      if (unreleased.length || summary.releaseRequired) {
        const list = unreleased.length
          ? unreleased.map((i) => `· ${i.issueNo} ${bodyParts[i.bodyPart]}：${i.description}`).join('\n')
          : '有普通问题尚未关闭'
        const { value } = await ElMessageBox.prompt(
          `以下普通问题仍未关闭，必须明确确认随单放行并写明原因：\n${list}`,
          '普通问题放行确认（不能悄悄略过）',
          {
            inputType: 'textarea',
            inputPlaceholder: '如：客户试穿确认不影响穿着，经主管同意随单交付',
            confirmButtonText: '确认放行并完工',
            inputValidator: (v) => (v && v.trim()) || '请填写放行确认说明'
          }
        )
        const { value: who } = await ElMessageBox.prompt('请填写本次放行人/操作人', '操作人', {
          inputValue: '方绮',
          inputValidator: (v) => (v && v.trim()) || '操作人不能为空'
        })
        payload.confirmRelease = true
        payload.releaseReason = value
        payload.operator = who
      }
    }
    const updated = await orderApi.advance(current.value.id, payload)
    ElMessage.success(`订单已推进到「${statusNames[updated.status]}」`)
    visible.value = false
    await load()
  } catch (e) {
    if (e === 'cancel' || e?.message === 'cancel') return
    ElNotification.error({ message: e.message || '操作失败', title: '无法推进' })
  }
}

async function payBalance() {
  await orderApi.payBalance(current.value.id)
  ElMessage.success('尾款已结清')
  await load()
  const fresh = allCards.value.find((c) => c.id === current.value.id)
  if (fresh) current.value = fresh
}

onMounted(async () => {
  const dict = await metaApi.dict()
  Object.assign(woNames, dict.woStatuses)
  Object.assign(stepNames, dict.steps)
  Object.assign(issueNames, dict.issueStatuses)
  Object.assign(bodyParts, dict.bodyParts)
  await load()
})
</script>

<style scoped>
.fit-line {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}
.fit-danger {
  color: var(--gf-danger);
  font-weight: 600;
}
.fit-warn {
  color: #b8860b;
  font-weight: 600;
}
.fit-ok {
  color: var(--gf-ok);
}
.fit-muted {
  color: var(--gf-muted, #999);
}
</style>
