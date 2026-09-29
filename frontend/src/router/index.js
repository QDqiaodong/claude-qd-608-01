import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/wizard' },
  {
    path: '/wizard',
    name: 'wizard',
    component: () => import('../views/OrderWizardView.vue'),
    meta: { title: '向导式下单', icon: '✦', desc: '选客户与款式 → 录量体 → 选面料 → 确认订单' }
  },
  {
    path: '/board',
    name: 'board',
    component: () => import('../views/OrderBoardView.vue'),
    meta: { title: '订单进度看板', icon: '❖', desc: '按状态分列，卡片可点开看工序工单' }
  },
  {
    path: '/fabrics',
    name: 'fabrics',
    component: () => import('../views/FabricWallView.vue'),
    meta: { title: '面料库存', icon: '▤', desc: '卡片墙 + 低库存预警' }
  },
  {
    path: '/work-orders',
    name: 'work-orders',
    component: () => import('../views/WorkOrderDeskView.vue'),
    meta: { title: '工序工单', icon: '⧉', desc: '状态推进 + 工时统计' }
  },
  {
    path: '/fitting',
    name: 'fitting',
    component: () => import('../views/FittingDeskView.vue'),
    meta: { title: '试衣整改台', icon: '⚯', desc: '试衣问题单 → 返修轮次 → 复核收口闭环' }
  },
  {
    path: '/styles',
    name: 'styles',
    component: () => import('../views/StyleAtelierView.vue'),
    meta: { title: '款式管理', icon: '❧', desc: '款号 / 品类 / 基础价 / 工艺难度' }
  },
  {
    path: '/measurements',
    name: 'measurements',
    component: () => import('../views/MeasureRoomView.vue'),
    meta: { title: '量体记录', icon: '⌖', desc: '分部位尺寸录入与复核' }
  },
  {
    path: '/grade',
    name: 'grade',
    component: () => import('../views/GradeStudioView.vue'),
    meta: { title: '版型批量放码', icon: '⇲', desc: '基准码推导完整尺码组 · 固定/比例/分段规则 · 毫米级精度' }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.afterEach((to) => {
  document.title = `${to.meta.title || ''} · 高级服装定制工坊`
})

export default router
