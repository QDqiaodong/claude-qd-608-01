<template>
  <div class="gf-page">
    <div class="gf-page-head">
      <div>
        <h2 class="gf-title">版型批量放码</h2>
        <p class="gf-subtitle">
          从基准尺码一次推导完整尺码组 · 固定增量 / 按比例 / 分段规则 · 左右分别放码
        </p>
      </div>
      <div class="head-actions">
        <el-button @click="openSaved">已留存方案（{{ savedList.length }}）</el-button>
        <el-button @click="openCompare">两套方案比较</el-button>
        <el-button type="primary" @click="startNew">新建临时方案</el-button>
      </div>
    </div>

    <!-- 方案头部 -->
    <div class="gf-card">
      <el-form :inline="true" class="grade-head-form">
        <el-form-item label="方案编号">
          <el-input v-model="form.schemeNo" placeholder="如 GR-7002" style="width: 130px" />
        </el-form-item>
        <el-form-item label="方案名称">
          <el-input v-model="form.name" placeholder="如 西装六码放码表" style="width: 200px" />
        </el-form-item>
        <el-form-item label="关联款式">
          <el-select v-model="form.styleId" clearable placeholder="临时计算（不绑定款式）"
                     filterable style="width: 230px">
            <el-option v-for="s in styles" :key="s.id" :label="`${s.styleNo} · ${s.name}`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="基准尺码">
          <el-select v-model="form.baseSizeCode" placeholder="选基准码" style="width: 110px">
            <el-option v-for="s in form.sizes" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="尺码序列">
          <el-select
            v-model="form.sizes"
            multiple
            filterable
            allow-create
            default-first-option
            :reserve-keyword="false"
            placeholder="从小到大输入，回车添加"
            style="width: 300px"
            @change="onSizesChange"
          >
            <el-option v-for="s in form.sizes" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="录入单位">
          <el-radio-group v-model="inputUnitModel">
            <el-radio-button label="CM">厘米 cm</el-radio-button>
            <el-radio-button label="MM">毫米 mm</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="舍入步进">
          <el-input-number v-model="form.roundStepMm" :min="0.1" :max="100" :step="0.1"
                           :precision="1" controls-position="right" style="width: 120px"
                           @change="scheduleCompute" />
          <span class="gf-muted" style="margin-left: 6px">毫米（全组统一，只舍入一次）</span>
        </el-form-item>
        <el-form-item>
          <el-tag v-if="editingId" type="warning" closable @close="startNew">改版中 #{{ editingId }}</el-tag>
          <el-tag v-else type="info">未留存 · 临时计算</el-tag>
        </el-form-item>
      </el-form>
    </div>

    <el-row :gutter="14">
      <!-- 左：基准尺寸与规则 -->
      <el-col :span="9">
        <div class="gf-card">
          <div class="gf-section-title">基准尺寸与放码规则（{{ unitLabel }}录入）</div>
          <p class="gf-muted" style="margin: 4px 0 10px; font-size: 12.5px">
            切换录入单位只是把输入框数值乘除 10，服务端内部统一按毫米计算。
          </p>
          <div v-for="row in partRows" :key="row.key" class="grade-rule-block">
            <div class="grade-rule-head">
              <span class="grade-part-name">{{ row.label }}</span>
              <el-radio-group v-model="row.ruleType" size="small" @change="syncRowType(row)">
                <el-radio-button label="FIXED">固定增量</el-radio-button>
                <el-radio-button label="RATIO">按比例</el-radio-button>
                <el-radio-button label="SEGMENT">分段</el-radio-button>
              </el-radio-group>
            </div>

            <div v-for="side in row.sides" :key="side.key" class="grade-side-row">
              <span class="grade-side-tag" v-if="side.side !== 'CENTER'">{{ sideName(side.side) }}</span>
              <div class="grade-base-input">
                <span class="gf-muted">基准</span>
                <el-input-number v-model="side.baseValue" :min="0.01" :step="inputStep"
                                 :precision="inputPrecision" controls-position="right"
                                 size="small" style="width: 120px" @change="scheduleCompute" />
              </div>

              <template v-if="side.ruleType === 'FIXED'">
                <el-input-number v-model="side.fixedDelta" :step="inputStep"
                                 :precision="inputPrecision" controls-position="right"
                                 size="small" style="width: 130px" @change="scheduleCompute" />
                <span class="gf-muted">{{ unitSymbol }}/级（升码）</span>
              </template>

              <template v-else-if="side.ruleType === 'RATIO'">
                <el-input-number v-model="side.ratioPercent" :step="0.5" :precision="2"
                                 controls-position="right" size="small" style="width: 120px"
                                 @change="scheduleCompute" />
                <span class="gf-muted">% / 级（相对基准）</span>
              </template>

              <template v-else>
                <div class="grade-segments">
                  <div v-for="(seg, idx) in side.segments" :key="idx" class="grade-seg-line">
                    级序
                    <el-input-number v-model="seg.fromLevel" :step="1" :precision="0" size="small"
                                     controls-position="right" style="width: 86px" @change="scheduleCompute" />
                    ~
                    <el-input-number v-model="seg.toLevel" :step="1" :precision="0" size="small"
                                     controls-position="right" style="width: 86px" @change="scheduleCompute" />
                    <el-input-number v-model="seg.delta" :step="inputStep" :precision="inputPrecision"
                                     controls-position="right" size="small" style="width: 116px"
                                     @change="scheduleCompute" />
                    <span class="gf-muted">{{ unitSymbol }}/级</span>
                    <el-button link type="danger" size="small"
                               @click="removeSegment(side, idx)" :disabled="side.segments.length <= 1">删段</el-button>
                  </div>
                  <div class="grade-seg-actions">
                    <el-button link type="primary" size="small" @click="addSegment(side)">＋ 增加一个码段</el-button>
                    <el-button link size="small" @click="autoSegments(side)">按当前尺码范围自动连续分段</el-button>
                    <span class="gf-muted">级序以基准码为 0，当前范围 {{ minLevel }}~{{ maxLevel }}，各段须连续覆盖</span>
                  </div>
                </div>
              </template>
            </div>
          </div>

          <div class="grade-actions">
            <el-button type="primary" plain :loading="computing" @click="computeNow">重新生成整组</el-button>
            <el-button type="primary" :loading="saving" @click="saveScheme">留存方案</el-button>
          </div>
        </div>
      </el-col>

      <!-- 右：整组结果 -->
      <el-col :span="15">
        <div class="gf-card">
          <div class="grade-result-head">
            <div class="gf-section-title">整组放码结果</div>
            <div>
              <el-radio-group v-model="displayUnit" size="small">
                <el-radio-button label="MM">毫米显示</el-radio-button>
                <el-radio-button label="CM">厘米显示</el-radio-button>
              </el-radio-group>
            </div>
          </div>
          <p class="gf-muted" style="margin: 2px 0 8px; font-size: 12.5px">
            显示单位只做毫米↔厘米的十进制移位，不重新舍入；反复切换不会漂移。
            <span v-if="result">基准码「{{ result.baseSizeCode }}」· 服务端计算耗时 {{ result.elapsedMs }}ms
              · 结果版本 v{{ resultVersion }}</span>
          </p>

          <el-alert v-if="computeError" :title="computeError" type="error" show-icon :closable="false"
                    style="margin-bottom: 8px">
            <template #default>
              <div style="font-size: 12.5px">
                本次生成失败，未产生半套新结果；下表仍是上一次完整结果，供核对。
              </div>
            </template>
          </el-alert>

          <el-empty v-if="!result" description="录入左侧基准与规则后将自动生成整组结果" :image-size="70" />

          <template v-else>
            <el-alert v-if="result.issues.length" :title="`检出 ${result.issues.length} 处异常关系，已定位到尺码与部位`"
                      type="warning" show-icon :closable="false" style="margin-bottom: 8px" />
            <div class="grade-table-wrap">
              <table class="grade-table">
                <thead>
                  <tr>
                    <th class="sticky-col">部位</th>
                    <th v-for="size in result.sizes" :key="size"
                        :class="{ base: size === result.baseSizeCode }">{{ size }}</th>
                  </tr>
                  <tr class="level-row">
                    <th class="sticky-col gf-muted">级序</th>
                    <th v-for="(lv, i) in result.levels" :key="i"
                        :class="{ base: result.sizes[i] === result.baseSizeCode }">
                      {{ lv === 0 ? '基准' : (lv > 0 ? '+' + lv : lv) }}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <template v-for="p in result.parts" :key="p.part + p.side">
                    <tr>
                      <td class="sticky-col part-cell">
                        {{ partName(p.part) }}<span v-if="p.side !== 'CENTER'" class="side-mini">{{ sideName(p.side) }}</span>
                      </td>
                      <td v-for="c in p.cells" :key="c.sizeCode"
                          :class="cellClass(c)">
                        {{ fmt(c.valueMm) }}
                        <div class="delta" v-if="c.level !== 0">{{ signed(c.deltaMm) }}</div>
                        <div class="delta base-delta" v-else>基准</div>
                      </td>
                    </tr>
                  </template>
                </tbody>
              </table>
            </div>

            <div v-if="result.issues.length" class="grade-issues">
              <div class="gf-section-title">异常关系定位</div>
              <el-table :data="result.issues" border size="small" style="margin-top: 6px">
                <el-table-column label="类型" width="130">
                  <template #default="{ row }">
                    <el-tag size="small" :type="issueTag(row.code)">{{ issueLabel(row.code) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="尺码" width="90">
                  <template #default="{ row }">{{ row.sizeCode }}（{{ row.level > 0 ? '+' + row.level : row.level }}）</template>
                </el-table-column>
                <el-table-column label="部位" width="120">
                  <template #default="{ row }">{{ partName(row.part) }}{{ row.side !== 'CENTER' ? '·' + sideName(row.side) : '' }}</template>
                </el-table-column>
                <el-table-column prop="message" label="原因说明" />
              </el-table>
            </div>

            <el-collapse style="margin-top: 10px">
              <el-collapse-item title="各部位计算公式（服务端口径）" name="formula">
                <div v-for="p in formulaLines(result)" :key="p.key" class="formula-line">
                  <span class="formula-part">{{ p.label }}</span>
                  <span class="gf-muted">{{ p.formula }}</span>
                </div>
              </el-collapse-item>
            </el-collapse>
          </template>
        </div>
      </el-col>
    </el-row>

    <!-- 已留存方案 -->
    <el-drawer v-model="savedDrawer" title="已留存放码方案" size="620px">
      <el-table :data="savedList" border size="small">
        <el-table-column prop="schemeNo" label="编号" width="100" />
        <el-table-column label="名称" min-width="170">
          <template #default="{ row }">
            {{ row.name }}
            <el-tag v-if="row.legacyFlag" size="small" type="info" style="margin-left: 4px">旧厘米数据</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="基准" width="60">
          <template #default="{ row }">{{ row.baseSizeCode }}</template>
        </el-table-column>
        <el-table-column label="尺码/部位" width="90">
          <template #default="{ row }">{{ row.sizeCount }}码 / {{ row.partCount }}项</template>
        </el-table-column>
        <el-table-column label="异常" width="60">
          <template #default="{ row }">
            <el-tag size="small" :type="row.issueCount ? 'danger' : 'success'">{{ row.issueCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="loadSaved(row.id)">打开</el-button>
            <el-button link type="danger" size="small" @click="removeSaved(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <!-- 两方案比较 -->
    <el-drawer v-model="compareDrawer" title="两套放码方案逐部位比较" size="82%" @open="loadSavedList">
      <div class="compare-pick">
        <el-select v-model="compareA" placeholder="方案 A" filterable style="width: 280px">
          <el-option v-for="s in savedList" :key="s.id" :label="`${s.schemeNo} · ${s.name}`" :value="s.id" />
        </el-select>
        <span class="gf-muted">对照</span>
        <el-select v-model="compareB" placeholder="方案 B" filterable style="width: 280px">
          <el-option v-for="s in savedList" :key="s.id" :label="`${s.schemeNo} · ${s.name}`" :value="s.id" />
        </el-select>
        <el-button type="primary" :disabled="!compareA || !compareB || compareA === compareB"
                   :loading="comparing" @click="runCompare">开始比较</el-button>
      </div>

      <template v-if="compareResult">
        <div class="compare-summary">
          <el-tag type="danger" effect="plain">公式差异 {{ compareResult.formulaDiffCells }} 格</el-tag>
          <el-tag type="warning" effect="plain">仅舍入差异 {{ compareResult.roundingOnlyCells }} 格</el-tag>
          <el-tag type="primary" effect="plain">最终尺寸不同 {{ compareResult.valueDiffCells }} 格</el-tag>
          <el-tag type="success" effect="plain">完全一致 {{ compareResult.identicalCells }} 格</el-tag>
          <span class="gf-muted">共 {{ compareResult.totalCells }} 格（仅比较两方案共有的 {{ compareResult.commonSizes.length }} 个尺码）</span>
        </div>
        <el-table :data="compareResult.rows" border size="small" :row-key="(r) => r.part + r.side"
                  :expand-row-keys="expandedCompare" style="margin-top: 8px">
          <el-table-column type="expand">
            <template #default="{ row }">
              <table class="cmp-cell-table">
                <thead>
                  <tr><th>尺码</th><th>方案A (mm)</th><th>方案B (mm)</th><th>差 A−B (mm)</th><th>差异分类</th></tr>
                </thead>
                <tbody>
                  <tr v-for="c in row.cells" :key="c.sizeCode"
                      :class="{ 'cmp-diff': c.tags.includes('VALUE') }">
                    <td>{{ c.sizeCode }}</td>
                    <td>{{ c.valueMmA ?? '—' }}</td>
                    <td>{{ c.valueMmB ?? '—' }}</td>
                    <td>{{ c.diffMm ?? '—' }}</td>
                    <td>
                      <el-tag v-for="t in c.tags" :key="t" size="small" style="margin-right: 4px"
                              :type="compareTag(t)">{{ compareTagLabel(t) }}</el-tag>
                      <span v-if="!c.tags.length" class="gf-muted">一致</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </template>
          </el-table-column>
          <el-table-column label="部位" width="150">
            <template #default="{ row }">
              {{ partName(row.part) }}<span v-if="row.side !== 'CENTER'" class="side-mini">{{ sideName(row.side) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="公式" min-width="360">
            <template #default="{ row }">
              <div v-if="row.formulaSame" class="gf-muted">公式相同</div>
              <div v-else>
                <div>A：{{ row.formulaA || '—（缺失）' }}</div>
                <div>B：{{ row.formulaB || '—（缺失）' }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="差异尺码数" width="200">
            <template #default="{ row }">
              <el-tag size="small" type="danger" effect="plain" v-if="row.formulaDiffCount">公式 {{ row.formulaDiffCount }}</el-tag>
              <el-tag size="small" type="warning" effect="plain" v-if="row.roundingOnlyCount" style="margin-left:4px">
                仅舍入 {{ row.roundingOnlyCount }}
              </el-tag>
              <el-tag size="small" type="primary" effect="plain" v-if="row.valueDiffCount" style="margin-left:4px">
                尺寸 {{ row.valueDiffCount }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { gradeApi, styleApi } from '../api/index.js'

const PART_ORDER = ['bust', 'waist', 'hip', 'shoulder', 'bodyLength', 'sleeveLength']
const PART_LABEL = {
  bust: '胸围', waist: '腰围', hip: '臀围', shoulder: '肩宽',
  bodyLength: '衣长', sleeveLength: '袖长'
}
const SYMMETRIC = { shoulder: true, sleeveLength: true }
const SIDE_LABEL = { CENTER: '不分左右', LEFT: '左侧', RIGHT: '右侧' }

const styles = ref([])
const savedList = ref([])
const savedDrawer = ref(false)
const compareDrawer = ref(false)

const form = ref(blankForm())
const partRows = ref([])
const result = ref(null)
const resultVersion = ref(0)
const computing = ref(false)
const saving = ref(false)
const computeError = ref('')
const editingId = ref(null)
const displayUnit = ref('MM')
let timer = null
let reqSeq = 0

const compareA = ref(null)
const compareB = ref(null)
const compareResult = ref(null)
const comparing = ref(false)
const expandedCompare = ref([])

function blankForm() {
  return {
    schemeNo: '',
    name: '',
    styleId: null,
    inputUnit: 'CM',
    roundStepMm: 1,
    baseSizeCode: 'M',
    sizes: ['XS', 'S', 'M', 'L', 'XL']
  }
}

const unitLabel = computed(() => (form.value.inputUnit === 'CM' ? '厘米' : '毫米'))
const unitSymbol = computed(() => (form.value.inputUnit === 'CM' ? 'cm' : 'mm'))
const inputStep = computed(() => (form.value.inputUnit === 'CM' ? 0.5 : 5))
const inputPrecision = computed(() => (form.value.inputUnit === 'CM' ? 2 : 0))
// 录入单位切换：已填基准/增量在 cm 与 mm 间精确乘除 10，再触发服务端重算
const inputUnitModel = computed({
  get: () => form.value.inputUnit,
  set: (next) => {
    const prev = form.value.inputUnit
    if (prev === next) return
    convertUnitValues(prev, next)
    form.value.inputUnit = next
    scheduleCompute()
  }
})
const minLevel = computed(() => (partRows.value.length ? -form.value.sizes.indexOf(form.value.baseSizeCode) : 0))
const maxLevel = computed(() => (partRows.value.length
  ? form.value.sizes.length - 1 - form.value.sizes.indexOf(form.value.baseSizeCode) : 0))

function partName(p) {
  return PART_LABEL[p] || p
}
function sideName(s) {
  return SIDE_LABEL[s] || s
}

// ------------------------------------------------------------ 规则行模型

function defaultSegments() {
  const baseIdx = form.value.sizes.indexOf(form.value.baseSizeCode)
  const lo = -baseIdx
  const hi = form.value.sizes.length - 1 - baseIdx
  const d = form.value.inputUnit === 'CM' ? 0.5 : 5
  // 默认两段：小码段（含 0 级，控制降码步与 0→1 步）与大码段（1 起）
  return [
    { fromLevel: lo, toLevel: 0, delta: d },
    { fromLevel: 1, toLevel: hi, delta: d }
  ].filter(s => s.fromLevel <= s.toLevel)
}

// 新增码段：在最后一段中间切一刀；不连续时由服务端给出明确中文报错，版师再调
function addSegment(side) {
  const segs = side.segments
  const last = segs[segs.length - 1]
  if (!last) {
    side.segments = defaultSegments()
    scheduleCompute()
    return
  }
  const mid = Math.floor((last.fromLevel + last.toLevel) / 2)
  if (mid < last.fromLevel) {
    ElMessage.info('当前码段已只有一级，请先手动改区间')
    return
  }
  const cut = { fromLevel: mid + 1, toLevel: last.toLevel, delta: last.delta }
  last.toLevel = mid
  segs.push(cut)
  scheduleCompute()
}

function removeSegment(side, idx) {
  side.segments.splice(idx, 1)
  scheduleCompute()
}

// 自动连续分段：把当前各段增量值尽量保留，区间重排为从 minLevel 连续覆盖到 maxLevel
function autoSegments(side) {
  const baseIdx = form.value.sizes.indexOf(form.value.baseSizeCode)
  const lo = -baseIdx
  const hi = form.value.sizes.length - 1 - baseIdx
  const old = side.segments.length ? side.segments : defaultSegments()
  const n = old.length
  const next = []
  for (let i = 0; i < n; i++) {
    const from = i === 0 ? lo : (next[i - 1].toLevel + 1)
    const size = Math.floor((hi - from + 1) / (n - i))
    const to = i === n - 1 ? hi : from + size - 1
    next.push({ fromLevel: from, toLevel: to, delta: old[i].delta })
  }
  side.segments = next
  scheduleCompute()
}

function makeSide(part, side) {
  return {
    part,
    side,
    baseValue: defaultBase(part),
    ruleType: 'FIXED',
    fixedDelta: defaultDelta(part),
    ratioPercent: 4,
    segments: defaultSegments()
  }
}

function defaultBase(part) {
  const cm = { bust: 90, waist: 74, hip: 97, shoulder: 19.8, bodyLength: 65, sleeveLength: 58 }
  const v = cm[part]
  return form.value.inputUnit === 'CM' ? v : Math.round(v * 10)
}

function defaultDelta(part) {
  const cm = { bust: 4, waist: 3, hip: 3.6, shoulder: 0.5, bodyLength: 2, sleeveLength: 1.5 }
  const v = cm[part]
  return form.value.inputUnit === 'CM' ? v : Math.round(v * 10)
}

function rebuildRows() {
  partRows.value = PART_ORDER.map(part => {
    const sides = SYMMETRIC[part]
      ? [{ side: 'LEFT' }, { side: 'RIGHT' }]
      : [{ side: 'CENTER' }]
    return {
      key: part,
      part,
      label: PART_LABEL[part] + (SYMMETRIC[part] ? '（左右分别放码）' : ''),
      ruleType: 'FIXED',
      sides: sides.map(s => makeSide(part, s.side))
    }
  })
}

// ------------------------------------------------------------ 单位/尺码切换

function onSizesChange() {
  if (!form.value.sizes.includes(form.value.baseSizeCode)) {
    form.value.baseSizeCode = form.value.sizes[Math.floor((form.value.sizes.length - 1) / 2)] || ''
  }
  scheduleCompute()
}

// 切换录入单位：把已填数值在 cm/mm 间精确换算（乘除 10），不让输入框精度丢数据
function convertUnitValues(prevUnit, nextUnit) {
  if (prevUnit === nextUnit) return
  const toMm = (v) => Math.round(v * 10)
  const toCm = (v) => Math.round(v * 100) / 100
  const conv = nextUnit === 'MM' ? toMm : toCm
  for (const row of partRows.value) {
    for (const s of row.sides) {
      s.baseValue = conv(s.baseValue)
      if (s.ruleType === 'FIXED') s.fixedDelta = conv(s.fixedDelta)
      if (s.ruleType === 'SEGMENT') s.segments.forEach(seg => { seg.delta = conv(seg.delta) })
    }
  }
}

// ------------------------------------------------------------ 请求组装

function buildPayload() {
  const bases = []
  const rules = []
  for (const row of partRows.value) {
    for (const s of row.sides) {
      bases.push({ part: row.part, side: s.side, value: s.baseValue })
      const rule = { part: row.part, side: s.side, ruleType: s.ruleType }
      if (s.ruleType === 'FIXED') rule.fixedDelta = s.fixedDelta
      if (s.ruleType === 'RATIO') rule.ratioPercent = s.ratioPercent
      if (s.ruleType === 'SEGMENT') rule.segments = s.segments
      rules.push(rule)
    }
  }
  return {
    id: editingId.value || undefined,
    schemeNo: form.value.schemeNo,
    name: form.value.name,
    styleId: form.value.styleId || null,
    inputUnit: form.value.inputUnit,
    roundStepMm: form.value.roundStepMm,
    baseSizeCode: form.value.baseSizeCode,
    sizes: [...form.value.sizes],
    bases,
    rules
  }
}

function scheduleCompute() {
  clearTimeout(timer)
  timer = setTimeout(computeNow, 350)
}

async function computeNow() {
  if (!form.value.sizes.length || !form.value.baseSizeCode) return
  computing.value = true
  const seq = ++reqSeq
  try {
    const data = await gradeApi.preview(buildPayload())
    if (seq !== reqSeq) return // 已有更新的请求，丢弃旧响应
    result.value = data
    resultVersion.value += 1
    computeError.value = ''
  } catch (e) {
    if (seq !== reqSeq) return
    // 关键口径：失败保留上一次完整结果，只提示原因，不残留半套新结果
    computeError.value = e.message || '本次生成失败，已保留上一次完整结果'
  } finally {
    if (seq === reqSeq) computing.value = false
  }
}

async function saveScheme() {
  if (!form.value.schemeNo || !form.value.name) {
    ElMessage.warning('请先填写方案编号与名称')
    return
  }
  saving.value = true
  try {
    const detail = await gradeApi.save(buildPayload())
    editingId.value = detail.id
    result.value = detail.result
    resultVersion.value = detail.resultVersion
    computeError.value = ''
    ElMessage.success('已留存，结果由服务端固化；再次打开数值一致')
    loadSavedList()
  } catch (e) {
    // GlobalExceptionHandler 已弹错误提示；旧结果保持不变
  } finally {
    saving.value = false
  }
}

// ------------------------------------------------------------ 已留存方案

async function loadSavedList() {
  savedList.value = await gradeApi.list()
}

function openSaved() {
  loadSavedList()
  savedDrawer.value = true
}

async function loadSaved(id) {
  const d = await gradeApi.detail(id)
  editingId.value = d.id
  form.value.schemeNo = d.schemeNo
  form.value.name = d.name
  form.value.styleId = d.styleId
  form.value.inputUnit = d.inputUnit
  form.value.roundStepMm = d.roundStepMm
  form.value.baseSizeCode = d.baseSizeCode
  form.value.sizes = [...d.sizes]
  hydrateRows(d)
  result.value = d.result
  resultVersion.value = d.resultVersion
  computeError.value = ''
  displayUnit.value = d.inputUnit === 'MM' ? 'MM' : 'CM'
  savedDrawer.value = false
  ElMessage.success('已打开留存快照（未重新计算，数值与留存时一致）')
}

function hydrateRows(d) {
  partRows.value = PART_ORDER.map(part => {
    const sides = SYMMETRIC[part] ? ['LEFT', 'RIGHT'] : ['CENTER']
    return {
      key: part,
      part,
      label: PART_LABEL[part] + (SYMMETRIC[part] ? '（左右分别放码）' : ''),
      ruleType: null,
      sides: sides.map(side => {
        const base = d.bases.find(b => b.part === part && b.side === side)
        const rule = d.rules.find(r => r.part === part && r.side === side) || { ruleType: 'FIXED', fixedDelta: 0 }
        return {
          part,
          side,
          baseValue: base ? base.value : 0,
          ruleType: rule.ruleType || 'FIXED',
          fixedDelta: rule.fixedDelta ?? 0,
          ratioPercent: rule.ratioPercent ?? 4,
          segments: rule.segments && rule.segments.length
            ? rule.segments.map(s => ({ ...s }))
            : defaultSegments()
        }
      })
    }
  }).map(row => {
    row.ruleType = row.sides[0].ruleType
    return row
  })
}

async function removeSaved(row) {
  await ElMessageBox.confirm(`确认删除放码方案「${row.name}」？`, '删除确认', { type: 'warning' })
  await gradeApi.remove(row.id)
  ElMessage.success('已删除')
  await loadSavedList()
}

function startNew() {
  editingId.value = null
  result.value = null
  resultVersion.value = 0
  computeError.value = ''
  const unit = form.value.inputUnit
  form.value = blankForm()
  form.value.inputUnit = unit
  rebuildRows()
  scheduleCompute()
}

// ------------------------------------------------------------ 两方案比较

function openCompare() {
  loadSavedList()
  compareResult.value = null
  compareDrawer.value = true
}

async function runCompare() {
  comparing.value = true
  try {
    compareResult.value = await gradeApi.compare(compareA.value, compareB.value)
    expandedCompare.value = compareResult.value.rows
      .filter(r => r.valueDiffCount > 0)
      .map(r => r.part + r.side)
  } finally {
    comparing.value = false
  }
}

function compareTag(t) {
  return { FORMULA: 'danger', ROUNDING: 'warning', VALUE: 'primary', MISSING: 'info' }[t] || ''
}
function compareTagLabel(t) {
  return { FORMULA: '公式差异', ROUNDING: '仅舍入差异', VALUE: '最终尺寸不同', MISSING: '单侧缺失' }[t] || t
}

// ------------------------------------------------------------ 结果显示

function fmt(mm) {
  const v = displayUnit.value === 'CM' ? Number(mm) / 10 : Number(mm)
  // 毫米步进 1 → cm 一位小数；0.1mm 步进 → cm 两位小数；按真实精度展示，不补零也不额外舍入
  return displayUnit.value === 'CM'
    ? String(parseFloat(v.toFixed(2)))
    : String(parseFloat(v.toFixed(1)))
}
function signed(mm) {
  const v = displayUnit.value === 'CM' ? Number(mm) / 10 : Number(mm)
  const text = displayUnit.value === 'CM' ? String(parseFloat(v.toFixed(2))) : String(parseFloat(v.toFixed(1)))
  return (v > 0 ? '+' : '') + text
}

function cellClass(c) {
  return {
    base: c.level === 0,
    neg: Number(c.valueMm) <= 0
  }
}

function issueLabel(code) {
  return {
    NEGATIVE_OR_ZERO: '负数/零长度',
    INVERSION: '相邻倒挂',
    DIRECTION: '方向错误',
    SHOULDER_RATIO: '肩胸比异常'
  }[code] || code
}
function issueTag(code) {
  return {
    NEGATIVE_OR_ZERO: 'danger',
    INVERSION: 'danger',
    DIRECTION: 'warning',
    SHOULDER_RATIO: 'warning'
  }[code] || 'info'
}

function formulaLines(r) {
  return r.parts.map(p => ({
    key: p.part + p.side,
    label: PART_LABEL[p.part] + (p.side !== 'CENTER' ? '·' + SIDE_LABEL[p.side] : ''),
    formula: p.formula
  }))
}

// ------------------------------------------------------------ 初始化

onMounted(async () => {
  styleApi.list({ page: 0, size: 100 }).then(res => { styles.value = res.content || [] }).catch(() => {})
  rebuildRows()
  scheduleCompute()
})

function syncRowType(row) {
  row.sides.forEach(s => {
    s.ruleType = row.ruleType
    if (s.ruleType === 'SEGMENT' && (!s.segments || !s.segments.length)) s.segments = defaultSegments()
  })
  scheduleCompute()
}
</script>

<style scoped>
.head-actions { display: flex; gap: 8px; }
.grade-head-form :deep(.el-form-item) { margin-bottom: 8px; margin-right: 14px; }

.grade-rule-block { border: 1px solid var(--gf-border, #e6e1d8); border-radius: 8px; padding: 8px 10px; margin-bottom: 10px; }
.grade-rule-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; gap: 8px; }
.grade-part-name { font-weight: 600; font-size: 14px; white-space: nowrap; }
.grade-side-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; padding: 4px 0; border-top: 1px dashed #e6e1d8; }
.grade-side-tag { font-size: 12px; color: var(--gf-primary, #8a6d3b); background: #faf4e8; border-radius: 4px; padding: 1px 6px; }
.grade-base-input { display: flex; align-items: center; gap: 6px; }
.grade-segments { width: 100%; display: flex; flex-direction: column; gap: 4px; }
.grade-seg-line { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; font-size: 12.5px; }
.grade-seg-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; font-size: 12px; }
.grade-actions { display: flex; gap: 10px; margin-top: 8px; }

.grade-result-head { display: flex; align-items: center; justify-content: space-between; }
.grade-table-wrap { overflow-x: auto; border: 1px solid #e6e1d8; border-radius: 8px; }
.grade-table { border-collapse: collapse; width: 100%; font-size: 13px; }
.grade-table th, .grade-table td { border: 1px solid #ece7dd; padding: 6px 8px; text-align: center; white-space: nowrap; }
.grade-table thead th { background: #faf7f0; position: sticky; top: 0; }
.grade-table .level-row th { font-weight: 400; font-size: 12px; background: #fdfbf6; }
.grade-table th.base, .grade-table td.base { background: #fff6e0; }
.grade-table td.neg { color: var(--gf-danger, #d14343); font-weight: 700; }
.sticky-col { position: sticky; left: 0; background: #faf7f0; z-index: 2; text-align: left; min-width: 108px; }
td.sticky-col { background: #fff; }
.part-cell { font-weight: 600; }
.side-mini { display: inline-block; margin-left: 4px; font-size: 11px; font-weight: 400; color: #8a6d3b; }
.delta { font-size: 11px; color: #9a8c6b; margin-top: 2px; }
.base-delta { color: #c2a05c; }
.grade-issues { margin-top: 12px; }
.formula-line { display: flex; gap: 10px; padding: 3px 0; font-size: 13px; }
.formula-part { min-width: 96px; font-weight: 600; }

.compare-pick { display: flex; gap: 10px; align-items: center; margin-bottom: 10px; }
.compare-summary { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.cmp-cell-table { border-collapse: collapse; width: 100%; font-size: 12.5px; }
.cmp-cell-table th, .cmp-cell-table td { border: 1px solid #eee; padding: 4px 8px; text-align: center; }
.cmp-cell-table tr.cmp-diff { background: #fff8ec; }
</style>
