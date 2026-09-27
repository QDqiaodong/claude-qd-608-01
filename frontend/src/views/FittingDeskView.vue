<template>
  <div class="gf-page">
    <div class="gf-page-head">
      <div>
        <h2 class="gf-title">试衣整改台</h2>
        <p class="gf-subtitle">试衣问题单 → 返修轮次 → 复核收口；阻断问题不关闭、返修工单不完成，订单不能完工</p>
      </div>
      <el-button type="primary" @click="openCreate">＋ 登记试衣问题</el-button>
    </div>

    <div class="gf-metric-row">
      <div class="gf-metric">
        <div class="label">未关闭问题</div>
        <div class="value">{{ metrics.open }}</div>
      </div>
      <div class="gf-metric">
        <div class="label">阻断交付</div>
        <div class="value" style="color: var(--gf-danger)">{{ metrics.blocker }}</div>
      </div>
      <div class="gf-metric">
        <div class="label">返修中工单</div>
        <div class="value" style="color: var(--gf-primary)">{{ metrics.rounds }}</div>
      </div>
      <div class="gf-metric">
        <div class="label">待复核</div>
        <div class="value">{{ metrics.reviewing }}</div>
      </div>
      <div class="gf-metric">
        <div class="label">已放行普通问题</div>
        <div class="value" style="color: var(--gf-warning, #b8860b)">{{ metrics.released }}</div>
      </div>
    </div>

    <div class="gf-toolbar">
      <el-select v-model="query.orderId" placeholder="全部订单" clearable filterable style="width: 210px"
                 @change="reload">
        <el-option v-for="o in fittingOrders" :key="o.id"
                   :label="`${o.orderNo} · ${o.customerName}`" :value="o.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="全部阶段" clearable style="width: 130px" @change="reload">
        <el-option v-for="(name, code) in issueStatuses" :key="code" :label="name" :value="code" />
      </el-select>
      <el-select v-model="query.severity" placeholder="全部程度" clearable style="width: 130px" @change="reload">
        <el-option v-for="(name, code) in severities" :key="code" :label="name" :value="code" />
      </el-select>
      <el-checkbox v-model="query.openOnly" @change="reload">只看未关闭</el-checkbox>
      <el-input v-model="query.keyword" placeholder="搜问题单号 / 订单号 / 客户" clearable style="width: 220px"
                @keyup.enter="reload" />
      <el-button type="primary" plain @click="reload">查询</el-button>
      <span class="spacer" />
      <el-input v-model="operator" placeholder="当前操作人（必填）" style="width: 170px" />
    </div>

    <div class="gf-card" style="padding: 6px 6px 10px">
      <el-table :data="rows" border stripe size="small">
        <el-table-column prop="issueNo" label="问题单号" width="96" />
        <el-table-column label="订单 / 客户" min-width="170">
          <template #default="{ row }">
            <div>{{ row.orderNo }}</div>
            <div class="gf-muted" style="font-size: 12px">{{ row.customerName }}</div>
          </template>
        </el-table-column>
        <el-table-column label="部位" width="90">
          <template #default="{ row }">{{ bodyParts[row.bodyPart] }}</template>
        </el-table-column>
        <el-table-column label="严重程度" width="104">
          <template #default="{ row }">
            <el-tag size="small" :type="row.severity === 'BLOCKER' ? 'danger' : 'info'"
                    :effect="row.severity === 'BLOCKER' ? 'dark' : 'plain'">
              {{ severities[row.severity] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="责任工序" width="86">
          <template #default="{ row }">{{ steps[row.responsibleStep] }}</template>
        </el-table-column>
        <el-table-column prop="description" label="问题描述" min-width="170" show-overflow-tooltip />
        <el-table-column label="当前返修轮次" min-width="170">
          <template #default="{ row }">
            <template v-if="currentRounds[row.id]">
              <div>{{ currentRounds[row.id].roundNo }} · 第{{ currentRounds[row.id].roundSeq }}轮</div>
              <div class="gf-muted" style="font-size: 12px">
                {{ currentRounds[row.id].owner }} ·
                {{ num(currentRounds[row.id].plannedHours, 1) }}/{{ num(currentRounds[row.id].actualHours, 1) }}h
                <el-tag size="small" :type="roundTag(currentRounds[row.id].status)" style="margin-left:2px">
                  {{ reworkStatuses[currentRounds[row.id].status] }}
                </el-tag>
              </div>
            </template>
            <span v-else class="gf-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="提出" width="124">
          <template #default="{ row }">
            <div>{{ row.raisedBy }}</div>
            <div class="gf-muted" style="font-size: 11.5px">{{ dt(row.raisedAt) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="当前阶段" width="108">
          <template #default="{ row }">
            <el-tag size="small" :type="issueTag(row.status)">{{ issueStatuses[row.status] }}</el-tag>
            <el-tag v-if="row.released" size="small" type="warning" effect="plain" style="margin-top: 2px">
              已放行
            </el-tag>
            <el-tag v-if="row.reopenCount > 0" size="small" type="danger" effect="plain" style="margin-top: 2px">
              重开×{{ row.reopenCount }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情/流水</el-button>
            <el-button v-if="canStartRework(row)" link type="warning" @click="startRework(row)">发起返修</el-button>
            <el-button v-if="canSubmitReview(row)" link type="primary" @click="submitReview(row)">提交复核</el-button>
            <el-button v-if="row.status === 'REVIEWING'" link type="success" @click="passReview(row)">复核通过</el-button>
            <el-button v-if="row.status === 'REVIEWING'" link type="danger" @click="rejectReview(row)">不通过</el-button>
            <el-button v-if="row.status === 'CLOSED'" link type="warning" @click="reopen(row)">重新打开</el-button>
            <el-button v-if="canRelease(row)" link type="warning" @click="release(row)">放行</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="gf-load-more">
        <el-button v-if="hasNext" :loading="loading" @click="loadMore">加载更多</el-button>
        <span v-else class="gf-muted">没有更多了</span>
      </div>
    </div>

    <!-- 登记问题单 -->
    <el-dialog v-model="createVisible" title="登记试衣问题" width="520px">
      <el-form label-width="96px">
        <el-form-item label="所属订单" required>
          <el-select v-model="form.orderId" filterable placeholder="仅「试衣中」订单可登记" style="width: 100%">
            <el-option v-for="o in fittingOrders" :key="o.id"
                       :label="`${o.orderNo} · ${o.customerName} · ${o.styleName || ''}`" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题部位" required>
          <el-select v-model="form.bodyPart" style="width: 100%">
            <el-option v-for="(name, code) in bodyParts" :key="code" :label="name" :value="code" />
          </el-select>
        </el-form-item>
        <el-form-item label="严重程度" required>
          <el-radio-group v-model="form.severity">
            <el-radio value="MINOR">普通问题（可复核关闭或完工时确认放行）</el-radio>
            <el-radio value="BLOCKER">阻断交付（必须返修并复核关闭）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="责任工序" required>
          <el-select v-model="form.responsibleStep" style="width: 100%">
            <el-option v-for="(name, code) in steps" :key="code" :label="name" :value="code" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题描述" required>
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit
                    placeholder="如：左臂袖长偏短约 1.2cm，抬臂吊紧" />
        </el-form-item>
        <el-form-item label="提出人" required>
          <el-input v-model="form.raisedBy" placeholder="默认使用右上角当前操作人" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCreate">登记问题单</el-button>
      </template>
    </el-dialog>

    <!-- 问题详情：轮次 + 流水 -->
    <el-drawer v-model="detailVisible" :title="`${detail?.issue?.issueNo || ''} · 整改轨迹`" size="640px">
      <template v-if="detail">
        <div class="gf-card" style="padding: 14px; margin-bottom: 14px">
          <div style="display: flex; gap: 8px; align-items: center; flex-wrap: wrap">
            <el-tag :type="detail.issue.severity === 'BLOCKER' ? 'danger' : 'info'"
                    :effect="detail.issue.severity === 'BLOCKER' ? 'dark' : 'plain'">
              {{ severities[detail.issue.severity] }}
            </el-tag>
            <el-tag :type="issueTag(detail.issue.status)">{{ issueStatuses[detail.issue.status] }}</el-tag>
            <el-tag v-if="detail.issue.released" type="warning" effect="plain">已放行</el-tag>
            <span class="gf-muted" style="font-size: 12.5px">
              {{ bodyParts[detail.issue.bodyPart] }} · 责任工序 {{ steps[detail.issue.responsibleStep] }}
            </span>
          </div>
          <div style="margin-top: 8px">{{ detail.issue.description }}</div>
          <div class="gf-muted" style="font-size: 12.5px; margin-top: 6px">
            {{ detail.issue.raisedBy }} 于 {{ dt(detail.issue.raisedAt) }} 提出
            <template v-if="detail.issue.reopenCount"> · 已重开 {{ detail.issue.reopenCount }} 次</template>
          </div>
          <div v-if="detail.summary.blockingReason" class="block-reason">
            ⛔ {{ detail.summary.blockingReason }}
          </div>
        </div>

        <h3 class="gf-section-title">返修轮次（独立工单，原工序工时不变）</h3>
        <el-table :data="detail.rounds" border size="small" style="margin-bottom: 14px">
          <el-table-column label="轮次" width="64">
            <template #default="{ row }">第{{ row.roundSeq }}轮</template>
          </el-table-column>
          <el-table-column prop="roundNo" label="返修单号" width="98" />
          <el-table-column prop="owner" label="负责人" width="80" />
          <el-table-column label="工时" width="96">
            <template #default="{ row }">{{ num(row.plannedHours, 1) }} / {{ num(row.actualHours, 1) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="92">
            <template #default="{ row }">
              <el-tag size="small" :type="roundTag(row.status)">{{ reworkStatuses[row.status] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="88">
            <template #default="{ row }">
              <el-button v-if="row.status === 'RUNNING'" link type="success" @click="finishRound(row)">
                完成
              </el-button>
            </template>
          </el-table-column>
          <el-table-column prop="reworkReason" label="返修原因" min-width="150" show-overflow-tooltip />
          <el-table-column label="复核结论" min-width="150">
            <template #default="{ row }">
              <span v-if="row.reviewedAt">
                {{ row.reviewer }} {{ dt(row.reviewedAt) }}：{{ row.reviewComment }}
              </span>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="detail.rounds.length === 0" class="gf-muted" style="margin-bottom: 12px">
          该问题尚未发起返修（普通问题可直接提交复核）
        </div>

        <h3 class="gf-section-title">操作流水</h3>
        <el-timeline>
          <el-timeline-item v-for="log in detail.logs" :key="log.id" :timestamp="`${log.operator} · ${dt(log.actedAt)}`"
                            :type="logTag(log.action)" placement="top">
            <b>{{ log.actionName }}</b>
            <span v-if="log.fromStatusName || log.toStatusName" class="gf-muted" style="font-size: 12px">
              （{{ log.fromStatusName || '—' }} → {{ log.toStatusName || '—' }}）
            </span>
            <div v-if="log.reason" style="font-size: 13px">{{ log.reason }}</div>
          </el-timeline-item>
        </el-timeline>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fittingApi, orderApi, metaApi } from '../api/index.js'
import { num, dt } from '../utils/format.js'

const issueStatuses = reactive({})
const severities = reactive({})
const bodyParts = reactive({})
const steps = reactive({})
const reworkStatuses = reactive({})

const operator = ref('方绮')
const fittingOrders = ref([])
const rows = ref([])
const page = ref(0)
const size = 10
const hasNext = ref(false)
const loading = ref(false)
const saving = ref(false)
const query = reactive({ orderId: null, status: '', severity: '', openOnly: false, keyword: '' })

const createVisible = ref(false)
const detailVisible = ref(false)
const detail = ref(null)
const currentRounds = reactive({})
const form = reactive({
  orderId: null,
  bodyPart: 'SLEEVE',
  severity: 'MINOR',
  responsibleStep: 'SEWING',
  description: '',
  raisedBy: '',
  remark: ''
})

const metrics = ref({ open: 0, blocker: 0, rounds: 0, reviewing: 0, released: 0 })

async function refreshMetrics() {
  const orderIds = [...new Set(rows.value.map((r) => r.orderId))]
  const summaries = await Promise.all(orderIds.map((oid) => fittingApi.orderSummary(oid)))
  const open = rows.value.filter((r) => r.status !== 'CLOSED')
  metrics.value = {
    open: open.length,
    blocker: open.filter((r) => r.severity === 'BLOCKER').length,
    rounds: summaries.reduce((sum, s) => sum + Number(s.activeRoundCount || 0), 0),
    reviewing: open.filter((r) => r.status === 'REVIEWING').length,
    released: rows.value.filter((r) => r.released).length
  }
}

function issueTag(status) {
  if (status === 'CLOSED') return 'success'
  if (status === 'REVIEWING') return 'warning'
  if (status === 'REWORKING') return 'danger'
  return 'info'
}

function roundTag(status) {
  if (status === 'PASSED') return 'success'
  if (status === 'REJECTED') return 'danger'
  if (status === 'DONE') return 'warning'
  return 'primary'
}

function logTag(action) {
  if (action === 'REVIEW_PASS') return 'success'
  if (action === 'REVIEW_REJECT') return 'danger'
  if (action === 'RELEASE' || action === 'REOPEN') return 'warning'
  return 'primary'
}

function canStartRework(row) {
  return (row.status === 'PENDING' || row.status === 'REWORKING') && !row.released
}
function canSubmitReview(row) {
  return row.status === 'PENDING' || row.status === 'REWORKING'
}
function canRelease(row) {
  return row.severity === 'MINOR' && row.status !== 'CLOSED' && !row.released
}

async function fetchPage(reset) {
  if (reset) {
    page.value = 0
    rows.value = []
  }
  loading.value = true
  try {
    const slice = await fittingApi.list({
      orderId: query.orderId || undefined,
      status: query.status || undefined,
      severity: query.severity || undefined,
      openOnly: query.openOnly || undefined,
      keyword: query.keyword || undefined,
      page: page.value,
      size
    })
    rows.value = rows.value.concat(slice.content)
    hasNext.value = slice.hasNext
    await Promise.all([refreshMetrics(), fillCurrentRounds()])
  } finally {
    loading.value = false
  }
}

async function fillCurrentRounds() {
  const orderIds = [...new Set(rows.value.map((r) => r.orderId))]
  const all = await Promise.all(orderIds.map((oid) => fittingApi.rounds({ orderId: oid })))
  Object.keys(currentRounds).forEach((k) => delete currentRounds[k])
  all.flat().forEach((r) => {
    if (r.status === 'RUNNING' || r.status === 'DONE') {
      currentRounds[r.issueId] = r
    }
  })
}

function reload() {
  fetchPage(true)
}

function loadMore() {
  page.value += 1
  fetchPage(false)
}

async function loadOrders() {
  const slice = await orderApi.list({ status: 'FITTING', size: 50 })
  fittingOrders.value = slice.content
}

function openCreate() {
  Object.assign(form, {
    orderId: query.orderId,
    bodyPart: 'SLEEVE',
    severity: 'MINOR',
    responsibleStep: 'SEWING',
    description: '',
    raisedBy: operator.value,
    remark: ''
  })
  createVisible.value = true
}

async function submitCreate() {
  if (!form.orderId) return ElMessage.warning('请选择试衣中的订单')
  if (!form.description || !form.description.trim()) return ElMessage.warning('请填写问题描述')
  saving.value = true
  try {
    await fittingApi.create({ ...form, raisedBy: form.raisedBy || operator.value })
    ElMessage.success('试衣问题单已登记')
    createVisible.value = false
    reload()
  } finally {
    saving.value = false
  }
}

async function openDetail(row) {
  detail.value = await fittingApi.detail(row.id)
  detailVisible.value = true
}

async function refreshDetail(id) {
  detail.value = await fittingApi.detail(id)
}

function ensureOperator() {
  if (!operator.value.trim()) {
    ElMessage.warning('请先在右上角填写当前操作人')
    return false
  }
  return true
}

async function startRework(row) {
  if (!ensureOperator()) return
  const { value } = await ElMessageBox.prompt(
    `${row.issueNo} 发起返修（责任工序：${steps[row.responsibleStep]}）\n请填写：负责人 | 计划工时 | 返修原因`,
    '发起返修（生成独立返修工单）',
    {
      inputType: 'textarea',
      inputPlaceholder: '如：王姐 | 2 | 左袖放长 1.2cm，重新上袖口',
      inputValidator: (v) => (v && v.split('|').length === 3 && v.split('|').every((p) => p.trim()))
        || '请按「负责人 | 计划工时 | 返修原因」三段填写'
    }
  )
  const [owner, hours, reason] = value.split('|').map((p) => p.trim())
  await fittingApi.startRework(row.id, {
    operator: operator.value,
    owner,
    plannedHours: Number(hours),
    reworkReason: reason,
    expectedVersion: row.version
  })
  ElMessage.success('返修工单已发起')
  await afterAction(row.id)
}

async function finishRound(row) {
  if (!ensureOperator()) return
  const { value } = await ElMessageBox.prompt(
    `${row.roundNo} 实际工时（小时），计划 ${num(row.plannedHours, 1)} 小时`,
    '返修完成（完成后才能提交复核）',
    { inputPattern: /^\d+(\.\d+)?$/, inputErrorMessage: '请填写数字', inputValue: String(row.plannedHours) }
  )
  await fittingApi.finishRound(row.id, { operator: operator.value, actualHours: Number(value) })
  ElMessage.success(`${row.roundNo} 已完成`)
  if (detail.value) await refreshDetail(detail.value.issue.id)
  reload()
}

async function submitReview(row) {
  if (!ensureOperator()) return
  let reason = '返修完成，申请复核'
  if (row.status === 'PENDING') {
    const r = await ElMessageBox.prompt(`${row.issueNo} 普通问题直接提交复核，请填写复核说明`, '提交复核', {
      inputType: 'textarea',
      inputValidator: (v) => (v && v.trim()) || '复核说明不能为空'
    })
    reason = r.value
  }
  await fittingApi.submitReview(row.id, { operator: operator.value, reason, expectedVersion: row.version })
  ElMessage.success('已提交复核')
  await afterAction(row.id)
}

async function passReview(row) {
  if (!ensureOperator()) return
  const { value } = await ElMessageBox.prompt(`${row.issueNo} 复核意见（可留空，默认「复核通过」）`, '复核通过将关闭问题', {
    inputValue: '复核通过，缺陷已消除',
    confirmButtonText: '复核通过并关闭'
  })
  await fittingApi.reviewPass(row.id, {
    operator: operator.value,
    reason: value || '复核通过',
    expectedVersion: row.version
  })
  ElMessage.success('复核通过，问题已关闭')
  await afterAction(row.id)
}

async function rejectReview(row) {
  if (!ensureOperator()) return
  const { value } = await ElMessageBox.prompt(
    `${row.issueNo} 复核不通过原因；下一轮返修将立即开启。\n如需调整下轮安排，可追加：|| 负责人 | 计划工时`,
    '复核不通过并开启下一轮返修',
    {
      inputType: 'textarea',
      inputPlaceholder: '如：袖衩起吊，需打开重放平服',
      inputValidator: (v) => (v && v.trim()) || '不通过原因不能为空'
    }
  )
  const [reason, tail] = value.split('||').map((p) => p && p.trim())
  const payload = { operator: operator.value, reason, expectedVersion: row.version }
  if (tail) {
    const [owner, hours] = tail.split('|').map((p) => p && p.trim())
    if (owner) payload.owner = owner
    if (hours) payload.plannedHours = Number(hours)
  }
  await fittingApi.reviewReject(row.id, payload)
  ElMessage.success('已登记不通过并开启下一轮返修')
  await afterAction(row.id)
}

async function reopen(row) {
  if (!ensureOperator()) return
  const { value } = await ElMessageBox.prompt(
    `${row.issueNo} 交付前复检发现同一部位缺陷？请填写：原因 || 负责人 | 计划工时`,
    '重新打开并发起新一轮返修（历史轮次保留）',
    {
      inputType: 'textarea',
      inputPlaceholder: '如：复穿后左肩仍吊 || 王姐 | 1.5',
      inputValidator: (v) => {
        if (!v || !v.trim()) return '请填写原因与下轮安排'
        const tail = v.split('||')[1]
        return (tail && tail.split('|').length === 2) || '请按「原因 || 负责人 | 计划工时」填写'
      }
    }
  )
  const [reason, tail] = value.split('||').map((p) => p && p.trim())
  const [owner, hours] = tail.split('|').map((p) => p && p.trim())
  await fittingApi.reopen(row.id, {
    operator: operator.value,
    reason,
    owner,
    plannedHours: Number(hours),
    expectedVersion: row.version
  })
  ElMessage.success('问题已重新打开，新一轮返修已发起')
  await afterAction(row.id)
}

async function release(row) {
  if (!ensureOperator()) return
  const { value } = await ElMessageBox.prompt(
    `${row.issueNo} 为普通问题，确认随单放行后将留下放行记录，问题仍标记为未关闭。请写明确认说明`,
    '普通问题放行确认',
    {
      inputType: 'textarea',
      inputPlaceholder: '如：客户已试穿确认不影响穿着，经主管同意随单交付',
      inputValidator: (v) => (v && v.trim()) || '放行确认说明不能为空'
    }
  )
  await fittingApi.release(row.id, { operator: operator.value, reason: value, expectedVersion: row.version })
  ElMessage.success('已记录放行确认')
  await afterAction(row.id)
}

async function afterAction(id) {
  if (detailVisible.value && detail.value) {
    try {
      await refreshDetail(id)
    } catch (e) {
      detailVisible.value = false
    }
  }
  reload()
}

onMounted(async () => {
  const dict = await metaApi.dict()
  Object.assign(issueStatuses, dict.issueStatuses)
  Object.assign(severities, dict.severities)
  Object.assign(bodyParts, dict.bodyParts)
  Object.assign(steps, dict.steps)
  Object.assign(reworkStatuses, dict.reworkStatuses)
  await loadOrders()
  await fetchPage(true)
})
</script>

<style scoped>
.block-reason {
  margin-top: 10px;
  padding: 8px 10px;
  border-radius: 6px;
  background: var(--gf-danger-bg, #fdecec);
  color: var(--gf-danger);
  font-size: 13px;
}
</style>
