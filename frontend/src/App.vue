<template>
  <div class="gf-shell">
    <aside class="gf-aside">
      <div class="gf-brand">
        <div class="logo">绫</div>
        <div class="name">绫 · 高级定制工坊</div>
        <div class="slogan">ATELIER LING · BESPOKE</div>
      </div>
      <nav class="gf-nav">
        <router-link
          v-for="item in menus"
          :key="item.path"
          :to="item.path"
          class="gf-nav-item"
          :class="{ active: route.path === item.path }"
        >
          <span class="ico">{{ item.icon }}</span>
          <span>{{ item.title }}</span>
        </router-link>
      </nav>
      <div class="gf-aside-foot">
        女装手工定制 · 一人一版<br />
        今日 {{ today }}
      </div>
    </aside>

    <div class="gf-main">
      <header class="gf-topbar">
        <div>
          <div style="font-size: 15px; font-weight: 600">{{ current.title }}</div>
          <div class="crumb">{{ current.desc }}</div>
        </div>
        <div class="right">
          <span class="gf-chip">低库存面料 {{ lowStockCount }} 卷</span>
          <span class="gf-chip">在产订单 {{ boardTotal }} 单</span>
          <span>驻店量体师 3 人</span>
        </div>
      </header>
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { orderApi, fabricApi } from './api/index.js'

const route = useRoute()

const menus = [
  { path: '/wizard', icon: '✦', title: '向导式下单', desc: '选客户与款式 → 录量体 → 选面料 → 确认订单' },
  { path: '/board', icon: '❖', title: '订单进度看板', desc: '按状态分列，卡片可点开看工序工单' },
  { path: '/fabrics', icon: '▤', title: '面料库存', desc: '卡片墙 + 低库存预警' },
  { path: '/work-orders', icon: '⧉', title: '工序工单', desc: '状态推进 + 工时统计' },
  { path: '/fitting', icon: '⚯', title: '试衣整改台', desc: '试衣问题单 → 返修轮次 → 复核收口闭环' },
  { path: '/styles', icon: '❧', title: '款式管理', desc: '款号 / 品类 / 基础价 / 工艺难度' },
  { path: '/measurements', icon: '⌖', title: '量体记录', desc: '分部位尺寸录入与复核' },
  { path: '/grade', icon: '⇲', title: '版型批量放码', desc: '基准码推导完整尺码组 · 固定/比例/分段规则 · 毫米级精度' }
]

const current = computed(() => menus.find((m) => m.path === route.path) || menus[0])
const today = new Date().toLocaleDateString('zh-CN')
const lowStockCount = ref(0)
const boardTotal = ref(0)

async function loadSummary() {
  try {
    const meta = await fabricApi.meta()
    lowStockCount.value = meta?.lowStockCount || 0
  } catch (e) {
    lowStockCount.value = 0
  }
  try {
    const board = await orderApi.board()
    boardTotal.value = board?.meta?.total || 0
  } catch (e) {
    boardTotal.value = 0
  }
}

onMounted(loadSummary)
</script>
