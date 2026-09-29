# 模块七：版型批量放码（Pattern Grading）

从**一个基准尺码**推导**完整尺码组**的批量放码工具。计算规则、精度口径与结果完整性全部由
**服务端（Spring Boot）统一保证**，浏览器只做录入与渲染，不在前端做任何放码运算。

## 1. 统一精度口径（为什么单位来回切换不会漂移）

- 引擎内部**唯一口径是「微米」有理数量**（`BigDecimal`，安全精度 60 位）：
  `1 cm = 10 000 μm`、`1 mm = 1 000 μm`，都是整数倍，换单位只改显示、不再舍入。
- 外部长度一律以**字符串**进出（`"90"`/`"900"`/`"900.5"`），杜绝 double/float 二进制误差。
- **只在整组最终输出时统一舍入一次**（四舍五入，半数远离 0），中间连乘/累加绝不舍入。
- 结果文档同时给出微米、cm、mm 三套值；cm/mm 由微米**精确整除**换算，不产生新舍入。
  页面切 cm↔mm 只是取现成字段重新渲染，100 次往返后仍是同一个微米整数（有测试复验）。

## 2. 三种放码规则

| 规则 | 含义 | 实现 |
| --- | --- | --- |
| `FIXED` 固定增量 | 每升降一级变化量相同（可负） | `base + delta×grade` |
| `RATIO` 按比例 | 每级 = 基准 ×(1+rate)^grade | 快速幂连乘，不逐级取整；负级差为倒数；因子≤0 直接判公式失败 |
| `SEGMENT` 分段 | 小码段/大码段不同增量 | 以「边」描述（边 k 连接级差 k 与 k+1），校验**连续覆盖不缺不叠**，O(1) 边索引 |

- **左右不对称**：部位拆成 `side=L/R` 两个独立槽位，基准与规则分别计算，**不自动合并、不平均**；
  对称部位只有一个 `side=null` 槽位。
- 每个尺码格都标出**相对基准版的总变化**（最终值 − 基准最终值）。

## 3. 异常检测（定位到「尺码 × 部位」，并给中文原因）

- `NEGATIVE` 最终尺寸为负
- `ZERO_LENGTH` 零长度
- `INVERSION` 相邻尺码倒挂（看最终尺寸顺序与声明方向）
- `INCREMENT_DIRECTION` 增量方向错误（看规则单级变化量符号与声明 UP/DOWN 是否冲突）
- `RATIO_ABNORMAL` 跨部位比例异常（默认肩宽/胸围区间检查，可配置；不对称时按同一边独立检查）

异常是**结果的一部分**（整组照常产出并标注）；而**结构/公式错误**（分段缺口、比例因子非正、
缺基准等）在产出任何结果前直接失败，见下条。

## 4. 原子性与确定性

- `save-and-generate` / `generate`：**先在内存完整跑通解析→计算→检测，全部成功才落库**。
  任何公式无法计算时整次失败，事务回滚，`result_json` 不被覆盖，版师仍能核对
  **上一次完整结果**，绝不留下半套新结果。
- 留存方案的内容经「按键排序的规范化 JSON」取 **SHA-256 指纹**；同样参数多次生成、
  重新打开后生成，所有 raw/final/delta 与异常逐条一致。
- 参数改了但没重新生成时，旧结果在详情里标记 **stale**，不会冒充新方案结果。
- 临时计算 `/preview` 不落库、每次独立，不残留上一次对象。

## 5. 两方案逐部位比较

`GET /api/grading/compare?schemeAId=&schemeBId=`，差异三分类（可并存，优先级 公式 > 最终 > 舍入）：

- **公式差异**：规则指纹不同（FIXED(40000) vs FIXED(50000)、比例/分段参数不同）
- **舍入差异**：公式相同、未舍入值相同，仅量子不同（如同一份值分别按 1cm / 0.1cm 舍入）
- **最终尺寸差异**：公式相同（如基准尺寸不同）导致最终值不同
- 槽位/尺码仅一侧存在 → ONLY_A / ONLY_B

## 6. 旧数据（v1）兼容

v1 只有厘米小数、固定增量、无单位与舍入配置。`LegacySchemeMigrator` 按固定规则读入：

1. 所有数值按**厘米**解析（毫米是 v2 概念，绝不把旧数放大 10 倍）；
2. 舍入固定 **cm / 0.1**（旧放码表行业口径，四舍五入到毫米）；
3. 迁移只在内存进行（cm→μm 精确 ×10000），重新生成的每个数与旧口径完全一致；
4. 原 v1 记录不改动，显式另存才升级为 v2。

## 7. 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/grading/meta` | 规则类型/方向/单位/舍入预设 |
| GET | `/api/grading/schemes` | 方案列表（temporary 区分留存/临时） |
| GET | `/api/grading/schemes/{id}` | 方案详情 + 上一次完整结果（v1 自动迁移） |
| POST | `/api/grading/schemes` | 仅留存（结构校验通过才存） |
| POST | `/api/grading/schemes/save-and-generate` | 留存并原子生成整组 |
| POST | `/api/grading/schemes/{id}/generate` | 已留存方案重新生成（失败保留旧结果） |
| DELETE | `/api/grading/schemes/{id}` | 删除 |
| POST | `/api/grading/preview` | 临时计算（不落库） |
| GET | `/api/grading/compare` | 两方案逐部位比较 |

## 8. 验收项 → 测试对照

`backend/src/test/java/com/garment/factory/grading/`

| 验收项 | 测试 |
| --- | --- |
| 三种规则 | `GradingEngineTest.fixedRuleUpAndDown / ratioRule / segmentRule` |
| 升码与降码 | `fixedRuleUpAndDown`、`ratioRule` |
| 多段增量 + 缺口定位 | `segmentRule`、`segmentGapRejected` |
| 左右不对称 | `asymmetricSidesCalculatedSeparately`、HTTP 冒烟 |
| cm/mm 往返零漂移 | `cmMmRoundtripExact`、`UnitMixingTest` 全组 |
| 舍入边界（含负数、不同量子） | `roundingBoundaryHalfUp` |
| 异常关系定位 | `anomaliesLocated`（负值/零/倒挂/方向/肩胸比） |
| 两方案差异三分 | `SchemeComparatorTest` 四例 |
| 批量计算性能（2万格<1s） | `performance`（实测约 25ms） |
| 留存确定性 | `GradingSchemeServiceTest.reopenGivesSameValues` |
| 公式失败保留旧结果 | `failedGenerationKeepsPreviousResult`、`saveAndGenerateAtomic` |
| v1 旧数据兼容 | `legacyV1ReadCompatibility`、`GradingHttpSmokeTest` |

运行：`cd backend && mvn test`（本地无 MySQL 时用 H2 test profile；CI/Docker 仍走 MySQL）。

## 9. 关键代码位置

- 引擎：`grading/engine/`（`LengthUnit` `DecimalQuantity` `GradingRounding` `GradingEngine`
  `GradingGenerator` `SchemeComparator`）
- 存储/迁移：`grading/model`、`grading/doc`（`SchemeDoc` `LegacySchemeMigrator` `SchemeDocMapper`）
- 服务/接口：`grading/service/GradingSchemeService`、`grading/controller/GradingController`
- 前端：`frontend/src/views/PatternGradingView.vue`、`frontend/src/utils/grading.js`
