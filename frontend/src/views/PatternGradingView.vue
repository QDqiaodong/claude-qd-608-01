<template>
  <div class="gf-page">
    <div class="gf-page-head">
      <div>
        <h2 class="gf-title">版型批量放码</h2>
        <p class="gf-subtitle">从基准版推导完整尺码组 · 固定增量 / 按比例 / 分段规则 · 左右分侧 · 厘米毫米零漂移（服务端统一口径）</p>
      </div>
      <div class="head-actions">
        <el-radio-group v-model="displayUnit" size="small" style="margin-right: 10px">
          <el-radio-button label="cm">厘米 cm</el-radio-button>
          <el-radio-button label="mm">毫米 mm</el-radio-button>
        </el-radio-group>
        <el-button @click="tab = 'compare'">两方案比较</el-button>
        <el-button type="primary" @click="newScheme">＋ 新建方案</el-button>
      </div>
    </div>

    <!-- ============================ 工作台 ============================ -->
    <div v-if="tab === 'workbench'" class="grading-layout">
      <!-- 左：方案列表 -->
      <div class="gf-card scheme-list">
        <el-input v-model="listQuery.keyword" placeholder="搜索方案名" clearable size="small"
                  style="margin-bottom: 8px" @keyup.enter="loadList" />
        <el-radio-group v-model="listQuery.temporary" size="small" style="margin-bottom: 10px">
          <el-radio-button :label="false">留存方案</el-radio-button>
          <el-radio-button :label="true">临时方案</el-radio-button>
        </el-radio-group>
        <div v-for="row in schemeRows" :key="row.id"
             class="scheme-item" :class="{ active: current && current.id === row.id }"
             @click="openScheme(row.id)">
          <div class="si-name">{{ row.name }}</div>
          <div class="si-no">{{ row.schemeNo }}</div>
          <div class="si-meta">
            <el-tag v-if="row.temporary" size="small" type="warning">临时</el-tag>
            <el-tag v-else size="small" type="success">已留存</el-tag>
            <el-tag v-if="row.hasResult" size="small">已有结果</el-tag>
            <el-tag v-else size="small" type="info">未生成</el-tag>
          </div>
        </div>
        <div v-if="!schemeRows.length" class="gf-muted" style="padding: 12px 4px">暂无方案，点右上角新建</div>
      </div>

      <!-- 右：编辑 + 结果 -->
      <div class="workbench-main">
        <template v-if="current">
          <!-- 方案头 -->
          <div class="gf-card editor-card">
            <div class="editor-head">
              <el-input v-model="current.name" placeholder="方案名称" size="default"
                        style="width: 240px; font-weight: 600" />
              <el-select v-model="current.styleId" placeholder="关联款式（可选）" clearable filterable
                         size="small" style="width: 220px">
                <el-option v-for="st in styleOptions" :key="st.id"
                           :label="`${st.styleNo} · ${st.name}`" :value="st.id" />
              </el-select>
              <el-checkbox v-model="current.temporary">临时计算方案</el-checkbox>
              <span class="spacer" />
              <el-button size="small" @click="previewCurrent" :loading="previewing">临时试算（不落库）</el-button>
              <el-button size="small" @click="saveOnly" :loading="saving">仅留存</el-button>
              <el-button size="small" type="primary" @click="saveAndGenerate" :loading="saving">
                保存并生成整组
              </el-button>
              <el-button v-if="current.id" size="small" type="danger" plain @click="removeCurrent">删除</el-button>
            </div>

            <el-alert v-if="result && result.stale" type="warning" show-icon :closable="false"
                      style="margin: 10px 0"
                      title="参数已修改，当前展示的是上一次完整结果，已不匹配最新参数，请重新生成。" />

            <!-- 尺码序列 + 舍入 -->
            <div class="block">
              <div class="block-title">目标尺码序列（级差：基准=0，升码正、降码负）</div>
              <div class="size-rows">
                <div v-for="(sz, i) in spec.sizes" :key="i" class="size-row">
                  <el-input-number v-model="sz.grade" :step="1" size="small" controls-position="right"
                                   style="width: 105px" />
                  <el-input v-model="sz.name" placeholder="尺码名 如 M / 165-88A" size="small"
                            style="width: 160px" />
                  <el-button link type="danger" size="small" @click="spec.sizes.splice(i, 1)">移除</el-button>
                </div>
                <el-button size="small" plain @click="addSize">＋ 加尺码</el-button>
                <span class="gf-muted" style="margin-left: 14px">基准级差：</span>
                <el-input-number v-model="spec.baseGrade" :step="1" size="small" controls-position="right"
                                 style="width: 110px" />
              </div>
            </div>

            <div class="block">
              <div class="block-title">统一舍入口径（全组结果在最终输出时统一舍入，中间过程不舍入）</div>
              <el-radio-group v-model="roundingPreset" size="small" @change="applyRoundingPreset">
                <el-radio-button v-for="(p, key) in roundingPresets" :key="key" :label="key">{{ p.label }}</el-radio-button>
              </el-radio-group>
            </div>

            <!-- 部位规则 -->
            <div class="block">
              <div class="block-title">
                部位与放码规则
                <span class="gf-muted">（侧别留空为整体对称部位；选 L/R 时左右分别计算、不自动合并）</span>
              </div>
              <el-table :data="spec.parts" border size="small" style="width: 100%">
                <el-table-column label="部位" width="150">
                  <template #default="{ row }">
                    <el-input v-model="row.partName" size="small" placeholder="名称" />
                  </template>
                </el-table-column>
                <el-table-column label="编码" width="110">
                  <template #default="{ row }">
                    <el-input v-model="row.partId" size="small" placeholder="如 bust" />
                  </template>
                </el-table-column>
                <el-table-column label="侧" width="80">
                  <template #default="{ row }">
                    <el-select v-model="row.side" size="small" clearable placeholder="整体">
                      <el-option label="左 L" value="L" />
                      <el-option label="右 R" value="R" />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column :label="`基准尺寸 (${displayUnit})`" width="120">
                  <template #default="{ row }">
                    <el-input v-model="row.base" size="small" placeholder="如 90" />
                  </template>
                </el-table-column>
                <el-table-column label="方向" width="92">
                  <template #default="{ row }">
                    <el-select v-model="row.direction" size="small">
                      <el-option label="升码增大" value="UP" />
                      <el-option label="升码减小" value="DOWN" />
                      <el-option label="不限" value="ANY" />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column label="规则" width="108">
                  <template #default="{ row }">
                    <el-select v-model="row.rule.type" size="small">
                      <el-option label="固定增量" value="FIXED" />
                      <el-option label="按比例" value="RATIO" />
                      <el-option label="分段" value="SEGMENT" />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column label="规则参数">
                  <template #default="{ row }">
                    <div v-if="row.rule.type === 'FIXED'" class="rule-inline">
                      每级 <el-input v-model="row.rule.delta" size="small" style="width: 90px"
                                    :placeholder="displayUnit === 'mm' ? '如 40' : '如 4'" />
                      <span class="gf-muted">{{ displayUnit }}</span>
                    </div>
                    <div v-else-if="row.rule.type === 'RATIO'" class="rule-inline">
                      每级 <el-input v-model="row.rule.rate" size="small" style="width: 90px"
                                    placeholder="如 4% 或 0.04" />
                      <span class="gf-muted">（相对基准连乘，不逐级舍入）</span>
                    </div>
                    <div v-else class="segments-box">
                      <div v-for="(seg, j) in row.rule.segments" :key="j" class="seg-row">
                        边
                        <el-input-number v-model="seg.fromGrade" size="small" controls-position="right"
                                         style="width: 95px" />
                        →
                        <el-input-number v-model="seg.toGrade" size="small" controls-position="right"
                                         style="width: 95px" />
                        增量
                        <el-input v-model="seg.delta" size="small" style="width: 80px"
                                  :placeholder="displayUnit === 'mm' ? 'mm' : 'cm'" />
                        <el-button link type="danger" size="small"
                                   @click="row.rule.segments.splice(j, 1)">删</el-button>
                      </div>
                      <el-button size="small" plain @click="addSegment(row)">＋ 区段</el-button>
                      <span class="gf-muted">区段必须连续覆盖全部相邻尺码，否则生成整次失败</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="" width="54">
                  <template #default="{ $index }">
                    <el-button link type="danger" size="small" @click="spec.parts.splice($index, 1)">删</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div class="part-actions">
                <el-button size="small" plain @click="addPart(null)">＋ 添加对称部位</el-button>
                <el-button size="small" plain @click="addAsymmetric">＋ 添加左右不对称部位（L/R 一对）</el-button>
              </div>
            </div>

            <!-- 肩胸比检查 -->
            <div class="block">
              <div class="block-title">跨部位比例检查（可选）</div>
              <div v-for="(chk, i) in spec.ratioChecks" :key="i" class="ratio-row">
                <el-input v-model="chk.label" size="small" style="width: 130px" placeholder="名称 如 肩宽/胸围" />
                分子槽位
                <el-input v-model="chk.numeratorSlot" size="small" style="width: 130px" placeholder="如 shoulder" />
                分母槽位
                <el-input v-model="chk.denominatorSlot" size="small" style="width: 120px" placeholder="如 bust" />
                区间
                <el-input v-model="chk.minRatio" size="small" style="width: 80px" placeholder="0.35" />
                ~
                <el-input v-model="chk.maxRatio" size="small" style="width: 80px" placeholder="0.50" />
                <el-button link type="danger" size="small" @click="spec.ratioChecks.splice(i, 1)">删</el-button>
              </div>
              <el-button size="small" plain @click="addRatioCheck">＋ 添加比例检查</el-button>
            </div>
          </div>

          <!-- 结果区 -->
          <div class="gf-card result-card">
            <div class="result-head">
              <span class="block-title" style="margin: 0">整组放码结果（{{ displayUnit === 'mm' ? '毫米' : '厘米' }}）</span>
              <span class="gf-muted">括号内为相对基准版的总变化</span>
              <span v-if="result?.meta" class="gf-muted">
                {{ result.meta.cellCount }} 格 · 引擎耗时 {{ (result.meta.elapsedMicros / 1000).toFixed(2) }} ms
              </span>
            </div>

            <el-alert v-if="!result" type="info" show-icon :closable="false"
                      title="尚未生成结果。点「保存并生成整组」或「临时试算」后在此查看。" style="margin: 8px 0" />

            <template v-else>
              <el-alert v-if="result.anomalies.length" :title="`检出 ${result.anomalies.length} 处异常（已定位到尺码与部位）`"
                        type="error" show-icon :closable="false" style="margin: 8px 0" />
              <el-table :data="result.slots" border size="small" style="width: 100%"
                        :row-class-name="slotRowClass">
                <el-table-column prop="partName" label="部位" width="120" fixed />
                <el-table-column v-for="sz in result.sizes" :key="sz.grade"
                                 :label="`${sz.name}${sz.grade === result.baseGrade ? '（基准）' : ''}`" min-width="120">
                  <template #default="{ row }">
                    <ResultCell :cell="cellOf(row, sz.grade)" :unit="displayUnit" />
                  </template>
                </el-table-column>
              </el-table>

              <div v-if="result.anomalies.length" class="anomaly-list">
                <div v-for="(a, i) in result.anomalies" :key="i" class="anomaly-item">
                  <el-tag size="small" type="danger" style="margin-right: 8px">{{ a.typeLabel }}</el-tag>
                  <b>{{ a.sizeName }}</b> · {{ a.slotName }}
                  <span class="gf-muted"> — {{ a.reason }}</span>
                </div>
              </div>
            </template>
          </div>
        </template>

        <el-empty v-else description="从左侧选择方案，或新建一套放码方案" />
      </div>
    </div>

    <!-- ============================ 两方案比较 ============================ -->
    <div v-else class="gf-card" style="padding: 18px">
      <div class="compare-head">
        <h3 style="margin: 0 16px 0 0">两套放码方案逐部位比较</h3>
        <el-select v-model="compareA" placeholder="方案 A" filterable style="width: 260px">
          <el-option v-for="r in savedRows" :key="r.id" :label="`${r.schemeNo} · ${r.name}`" :value="r.id" />
        </el-select>
        <el-select v-model="compareB" placeholder="方案 B" filterable style="width: 260px">
          <el-option v-for="r in savedRows" :key="r.id" :label="`${r.schemeNo} · ${r.name}`" :value="r.id" />
        </el-select>
        <el-button type="primary" :disabled="!compareA || !compareB" @click="runCompare">开始比较</el-button>
        <el-button link @click="tab = 'workbench'">返回工作台</el-button>
      </div>

      <template v-if="compareResult">
        <div class="compare-counts">
          <el-tag type="danger" effect="plain">公式差异 {{ compareResult.counts.FORMULA || 0 }}</el-tag>
          <el-tag type="warning" effect="plain">舍入差异 {{ compareResult.counts.ROUNDING || 0 }}</el-tag>
          <el-tag type="primary" effect="plain">最终尺寸差异 {{ compareResult.counts.FINAL || 0 }}</el-tag>
          <el-tag type="info" effect="plain">仅 A {{ compareResult.counts.ONLY_A || 0 }}</el-tag>
          <el-tag type="info" effect="plain">仅 B {{ compareResult.counts.ONLY_B || 0 }}</el-tag>
        </div>
        <el-table :data="compareResult.slots" border size="small">
          <el-table-column prop="nameA" label="部位（A）" width="150" />
          <el-table-column label="差异分类" width="240">
            <template #default="{ row }">
              <el-tag v-if="row.onlyInA" size="small" type="info">仅 A</el-tag>
              <el-tag v-else-if="row.onlyInB" size="small" type="info">仅 B</el-tag>
              <template v-else>
                <el-tag v-if="row.formulaDifferent" size="small" type="danger" style="margin-right:4px">公式</el-tag>
                <el-tag v-if="row.roundingDifferent" size="small" type="warning" style="margin-right:4px">舍入</el-tag>
                <el-tag v-if="row.finalDifferent" size="small" type="primary">尺寸</el-tag>
                <el-tag v-if="!row.formulaDifferent && !row.roundingDifferent && !row.finalDifferent"
                        size="small" type="success">一致</el-tag>
              </template>
            </template>
          </el-table-column>
          <el-table-column prop="summary" label="说明" width="260" />
          <el-table-column label="逐尺码差异（B − A，cm）">
            <template #default="{ row }">
              <div class="compare-cells">
                <span v-for="c in row.cells.filter((x) => x.kind !== 'NONE')" :key="c.grade"
                      class="compare-chip" :class="kindClass(c.kind)">
                  {{ c.sizeName }}：{{ c.finalACm || '—' }} → {{ c.finalBCm || '—' }}
                  <em v-if="c.finalDeltaCm">（{{ c.finalDeltaCm }}）</em>
                </span>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </div>
  </div>
</template>

<script setup>
import { h, nextTick, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { gradingApi, styleApi } from '../api/index.js'
import { timesTen, divideTen, cellValue, deltaValue } from '../utils/grading.js'

const tab = ref('workbench')
const displayUnit = ref('cm')
const schemeRows = ref([])
const savedRows = ref([])
const styleOptions = ref([])
const current = ref(null)
const spec = ref(blankSpec())
const result = ref(null)
const previewing = ref(false)
const saving = ref(false)
const listQuery = ref({ keyword: '', temporary: false })
const roundingPresets = ref({})
const roundingPreset = ref('cm_0_1')

const compareA = ref(null)
const compareB = ref(null)
const compareResult = ref(null)

function blankSpec() {
  return {
    schemaVersion: 2,
    baseGrade: 0,
    inputUnit: 'cm',
    rounding: { unit: 'cm', step: '0.1' },
    sizes: [
      { grade: -2, name: 'S' }, { grade: -1, name: 'M-' }, { grade: 0, name: 'M' },
      { grade: 1, name: 'L' }, { grade: 2, name: 'XL' }
    ],
    parts: [],
    ratioChecks: []
  }
}

function defaultParts() {
  const defs = [
    ['bust', '胸围', '90', '4', 'UP'],
    ['waist', '腰围', '72', '3', 'UP'],
    ['hip', '臀围', '96', '3.5', 'UP'],
    ['shoulder', '肩宽', '38', '0.8', 'UP'],
    ['bodyLen', '衣长', '62', '1.5', 'UP'],
    ['sleeveLen', '袖长', '57', '1.2', 'UP']
  ]
  return defs.map(([partId, partName, base, delta, direction]) => ({
    partId, partName, side: null, base, unit: 'cm', direction,
    rule: { type: 'FIXED', delta, unit: 'cm', rate: '', segments: [] }
  }))
}

function newScheme() {
  current.value = { id: null, name: '未命名放码方案', styleId: null, temporary: false }
  spec.value = blankSpec()
  spec.value.parts = defaultParts()
  spec.value.ratioChecks.push({
    checkCode: 'SHOULDER_CHEST', numeratorSlot: 'shoulder', denominatorSlot: 'bust',
    minRatio: '0.35', maxRatio: '0.50', label: '肩宽/胸围'
  })
  result.value = null
  compareResult.value = null
  // 新方案是带 cm 默认值的空白模板；若当前停留在 mm，需无移位地切回 cm（模板本身就是 cm 写法）
  if (displayUnit.value !== 'cm') {
    suppressUnitWatch = true
    displayUnit.value = 'cm'
    nextTick(() => { suppressUnitWatch = false })
  }
}

function addSize() {
  const grades = spec.value.sizes.map((s) => s.grade)
  const next = grades.length ? Math.max(...grades) + 1 : 0
  spec.value.sizes.push({ grade: next, name: '尺码' + next })
}

function addPart(side) {
  spec.value.parts.push({
    partId: '', partName: '', side, base: '', unit: displayUnit.value, direction: 'UP',
    rule: { type: 'FIXED', delta: '', unit: displayUnit.value, rate: '', segments: [] }
  })
}

function addAsymmetric() {
  for (const side of ['L', 'R']) {
    spec.value.parts.push({
      partId: 'shoulder', partName: '肩宽', side, base: '', unit: displayUnit.value, direction: 'UP',
      rule: { type: 'FIXED', delta: '', unit: displayUnit.value, rate: '', segments: [] }
    })
  }
}

function addSegment(row) {
  row.rule.segments.push({ fromGrade: 0, toGrade: 1, delta: '', unit: displayUnit.value })
}

function addRatioCheck() {
  spec.value.ratioChecks.push({
    checkCode: 'CUSTOM', numeratorSlot: '', denominatorSlot: '',
    minRatio: '', maxRatio: '', label: ''
  })
}

function applyRoundingPreset(key) {
  const p = roundingPresets.value[key]
  if (!p) return
  spec.value.rounding = { unit: p.unit, step: p.step }
}

/* ---------- 单位切换：只做精确移位（录入值），结果直接取服务端两套字段，不重算不舍入 ---------- */
function switchUnit(target, oldUnit) {
  if (target === oldUnit) return
  const fn = target === 'mm' ? timesTen : divideTen
  spec.value.inputUnit = target
  const shiftRule = (rule) => {
    rule.unit = target
    if (rule.type === 'FIXED' && rule.delta) rule.delta = fn(rule.delta)
    if (rule.type === 'SEGMENT') {
      rule.segments.forEach((seg) => {
        seg.unit = target
        if (seg.delta) seg.delta = fn(seg.delta)
      })
    }
  }
  spec.value.parts.forEach((p) => {
    p.unit = target
    if (p.base) p.base = fn(p.base)
    shiftRule(p.rule)
  })
  // 不触发 preview：已有结果来自服务端（内部微米不变），切单位只是重新渲染
}

// 切单位时旧单位由 watch 回调的 o 给出；结果数值取自服务端 cm/mm 现成字段。
// suppressUnitWatch：打开方案时 spec 已是该单位的写法，不能再触发一次移位，否则会重复换算。
let suppressUnitWatch = false
watch(displayUnit, (n, o) => {
  if (suppressUnitWatch || n === o) return
  switchUnit(n, o)
})

/* ---------- 结果格组件 ---------- */
const ResultCell = {
  props: ['cell', 'unit'],
  setup(props) {
    return () => {
      if (!props.cell) return h('span', { class: 'gf-muted' }, '—')
      const v = cellValue(props.cell, props.unit)
      const d = deltaValue(props.cell, props.unit)
      const numD = Number(d)
      const cls = numD > 0 ? 'delta-up' : numD < 0 ? 'delta-down' : 'delta-zero'
      return h('div', { class: 'result-cell' }, [
        h('div', { class: 'rc-val' }, v),
        h('div', { class: 'rc-delta ' + cls }, numD === 0 ? '基准' : `${numD > 0 ? '+' : ''}${d}`)
      ])
    }
  }
}

function cellOf(slotRow, grade) {
  return slotRow.cells.find((c) => c.grade === grade)
}

function slotRowClass({ row }) {
  return (result.value?.anomalies || []).some((a) => a.slotKey === row.slotKey) ? 'row-anomaly' : ''
}

function kindClass(kind) {
  return {
    FORMULA: 'kf-formula', ROUNDING: 'kf-rounding', FINAL: 'kf-final',
    ONLY_A: 'kf-only', ONLY_B: 'kf-only'
  }[kind] || ''
}

/* ---------- 接口 ---------- */
async function loadList() {
  const data = await gradingApi.list({
    temporary: listQuery.value.temporary, keyword: listQuery.value.keyword || undefined, size: 100
  })
  schemeRows.value = data.content
  const saved = await gradingApi.list({ temporary: false, size: 100 })
  savedRows.value = saved.content
}

async function openScheme(id) {
  const data = await gradingApi.detail(id)
  current.value = {
    id: data.id, name: data.name, styleId: data.styleId, temporary: data.temporary
  }
  spec.value = data.spec
  roundingPreset.value = matchPreset(data.spec.rounding)
  result.value = data.result
  compareResult.value = null
  // 以方案保存时的录入单位呈现（内部数值是微米；切单位只是精确移位写法，不触发重算）
  suppressUnitWatch = true
  displayUnit.value = data.spec.inputUnit === 'mm' ? 'mm' : 'cm'
  await nextTick()
  suppressUnitWatch = false
}

function matchPreset(r) {
  const hit = Object.entries(roundingPresets.value)
    .find(([, p]) => p.unit === r.unit && p.step === r.step)
  return hit ? hit[0] : ''
}

async function previewCurrent() {
  previewing.value = true
  try {
    const data = await gradingApi.preview(buildPayload(false))
    result.value = data.result
  } finally {
    previewing.value = false
  }
}

function buildPayload(withId) {
  return {
    id: withId ? current.value.id : undefined,
    name: current.value.name,
    styleId: current.value.styleId,
    temporary: current.value.temporary,
    spec: JSON.parse(JSON.stringify(spec.value))
  }
}

async function saveOnly() {
  saving.value = true
  try {
    await gradingApi.save(buildPayload(true))
    ElMessage.success('方案已留存')
    await loadList()
  } finally {
    saving.value = false
  }
}

async function saveAndGenerate() {
  saving.value = true
  try {
    const data = await gradingApi.saveAndGenerate(buildPayload(true))
    current.value.id = data.id
    result.value = data.result
    ElMessage.success('已留存并生成完整放码结果')
    await loadList()
  } finally {
    saving.value = false
  }
}

async function removeCurrent() {
  if (!current.value.id) return
  await ElMessageBox.confirm('确认删除该放码方案？', '删除确认', { type: 'warning' })
  await gradingApi.remove(current.value.id)
  ElMessage.success('已删除')
  current.value = null
  result.value = null
  await loadList()
}

async function runCompare() {
  compareResult.value = await gradingApi.compare(compareA.value, compareB.value)
}

onMounted(async () => {
  const meta = await gradingApi.meta()
  roundingPresets.value = meta.roundingPresets
  styleOptions.value = await styleApi.list({ size: 100 }).then((d) => d.content).catch(() => [])
  await loadList()
  newScheme()
})
</script>

<style scoped>
.head-actions { display: flex; align-items: center; }
.grading-layout { display: flex; gap: 14px; align-items: flex-start; }
.scheme-list { width: 250px; padding: 12px; flex-shrink: 0; max-height: calc(100vh - 150px); overflow-y: auto; }
.scheme-item { padding: 9px 10px; border-radius: 10px; cursor: pointer; border: 1px solid transparent; margin-bottom: 6px; }
.scheme-item:hover { background: var(--gf-primary-soft); }
.scheme-item.active { background: var(--gf-primary-soft); border-color: var(--gf-primary); }
.si-name { font-weight: 600; font-size: 13.5px; }
.si-no { font-size: 11.5px; color: var(--gf-ink-3); }
.si-meta { display: flex; gap: 4px; margin-top: 5px; flex-wrap: wrap; }

.workbench-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 14px; }
.editor-card, .result-card { padding: 16px 18px; }
.editor-head { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.spacer { flex: 1; }
.block { margin-top: 16px; }
.block-title { font-size: 13.5px; font-weight: 600; margin-bottom: 9px; }
.size-rows { display: flex; flex-direction: column; gap: 8px; align-items: flex-start; }
.size-row { display: flex; gap: 8px; align-items: center; }
.rule-inline { display: flex; gap: 6px; align-items: center; flex-wrap: wrap; }
.segments-box { display: flex; flex-direction: column; gap: 6px; align-items: flex-start; }
.seg-row { display: flex; gap: 6px; align-items: center; font-size: 12px; color: var(--gf-ink-2); }
.part-actions { margin-top: 10px; display: flex; gap: 10px; }
.ratio-row { display: flex; gap: 6px; align-items: center; margin-bottom: 8px; flex-wrap: wrap; }

.result-head { display: flex; align-items: baseline; gap: 12px; margin-bottom: 8px; flex-wrap: wrap; }
.result-cell .rc-val { font-weight: 600; font-size: 14px; }
.result-cell .rc-delta { font-size: 11.5px; }
.delta-up { color: var(--gf-primary); }
.delta-down { color: #1f6feb; }
.delta-zero { color: var(--gf-ink-3); }
:deep(.row-anomaly) { background: #fff3f2; }
.anomaly-list { margin-top: 12px; display: flex; flex-direction: column; gap: 7px; }
.anomaly-item { font-size: 13px; padding: 7px 10px; background: #fff7f6; border: 1px solid #f3cfcc; border-radius: 8px; }

.compare-head { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.compare-counts { display: flex; gap: 10px; margin-bottom: 14px; }
.compare-cells { display: flex; flex-wrap: wrap; gap: 6px; }
.compare-chip { font-size: 12px; padding: 3px 8px; border-radius: 999px; background: #f5f0f2; }
.compare-chip em { color: var(--gf-ink-2); font-style: normal; margin-left: 2px; }
.kf-formula { background: #fde8e6; }
.kf-rounding { background: #fdf2de; }
.kf-final { background: #e7f0fd; }
.kf-only { background: #eee; }
</style>
