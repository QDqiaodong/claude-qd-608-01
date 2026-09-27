-- ============================================================================
-- 高级服装定制工坊管理系统 · 建表 + 种子数据
-- 库名 garment_factory，由 mysql 镜像的 docker-entrypoint 在【镜像构建期】执行一次。
--
-- 业务规则 → 种子触发点对照：
--   [R1.1] 款号/面料号/订单号/工单号唯一   → 已存在 ST-1001 / FB-3001 / ORD-20260001 / WO-0001
--   [R1.2] 停售款式不可下单                → ST-1011 手绣礼服套装 on_sale = 0
--   [R2.1] 量体部位尺寸合理区间 50~200cm   → ME-4008 的腰围 42.0 越界（页面可复验）
--   [R3.1] 面料低库存预警(<50米)           → FB-3005 库存 18.5、FB-3011 库存 6.0
--   [R3.2] 库存不足不可下单                → 用量超过 FB-3011 剩余库存即被拦
--   [R4.1] 订单状态逐级流转，不可跳级      → ORD-20260001 = ORDERED，不能一步到 SEWING
--   [R4.2] 交期须晚于下单日 >= 7 天        → 见 OrderService 校验
--   [R4.3] 定金 50% / 尾款 50%             → 每单 deposit_paid / balance_paid 分布不同
--   [R4.4] 完成前须全部工单完成            → ORD-202600011(FINISHED) 工单全 DONE
--   [R4.5] 交付前须结清尾款                → ORD-202600012 处于已完成且尾款未结，推进交付会被拦
--   [R5.1] 工单完成须实际工时 > 0          → WO-0003 已 DONE 且 actual_hours=3.5
--   [R5.2] 返工工单须填返工原因            → WO-0024 = REWORK
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS fitting_action_log;
DROP TABLE IF EXISTS fitting_rework_round;
DROP TABLE IF EXISTS fitting_issue;
DROP TABLE IF EXISTS work_order;
DROP TABLE IF EXISTS custom_order;
DROP TABLE IF EXISTS measurement;
DROP TABLE IF EXISTS fabric;
DROP TABLE IF EXISTS style;
DROP TABLE IF EXISTS customer;

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------------
-- 客户（订单与量体的归属对象）
-- ---------------------------------------------------------------------------
CREATE TABLE customer (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    customer_no  VARCHAR(30)  NOT NULL COMMENT '客户编号',
    name         VARCHAR(50)  NOT NULL COMMENT '客户姓名',
    phone        VARCHAR(20)  NOT NULL COMMENT '联系电话',
    gender       VARCHAR(10)  NOT NULL COMMENT '性别',
    vip_level    VARCHAR(20)  NOT NULL DEFAULT 'NORMAL' COMMENT 'VIP 等级 GOLD/SILVER/NORMAL',
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_no (customer_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '客户';

-- ---------------------------------------------------------------------------
-- 模块一：款式管理
-- ---------------------------------------------------------------------------
CREATE TABLE style (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    style_no    VARCHAR(30)   NOT NULL COMMENT '款号',
    name        VARCHAR(80)   NOT NULL COMMENT '款式名称',
    category    VARCHAR(20)   NOT NULL COMMENT '品类 SUIT/OVERCOAT/SHIRT/DRESS/PANTS',
    base_price  DECIMAL(10,2) NOT NULL COMMENT '基础价（不含面料）',
    craft_level INT           NOT NULL COMMENT '工艺难度 1-5',
    on_sale     TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '是否在售',
    remark      VARCHAR(200)  COMMENT '备注',
    PRIMARY KEY (id),
    UNIQUE KEY uk_style_no (style_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '款式';

-- ---------------------------------------------------------------------------
-- 模块二：量体记录
-- ---------------------------------------------------------------------------
CREATE TABLE measurement (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    record_no    VARCHAR(30)   NOT NULL COMMENT '量体单号',
    customer_id  BIGINT        NOT NULL COMMENT '所属客户',
    customer_name VARCHAR(50)  NOT NULL COMMENT '客户姓名（冗余便于列表展示）',
    tailor_name  VARCHAR(50)   NOT NULL COMMENT '量体师',
    measure_date DATE          NOT NULL COMMENT '量体日期',
    bust         DECIMAL(6,2)  NOT NULL COMMENT '胸围 cm',
    waist        DECIMAL(6,2)  NOT NULL COMMENT '腰围 cm',
    hip          DECIMAL(6,2)  NOT NULL COMMENT '臀围 cm',
    shoulder     DECIMAL(6,2)  NOT NULL COMMENT '肩宽 cm',
    sleeve_len   DECIMAL(6,2)  NOT NULL COMMENT '袖长 cm',
    body_len     DECIMAL(6,2)  NOT NULL COMMENT '衣长 cm',
    pants_len    DECIMAL(6,2)  NOT NULL COMMENT '裤长 cm',
    remark       VARCHAR(500)  COMMENT '备注',
    PRIMARY KEY (id),
    UNIQUE KEY uk_record_no (record_no),
    KEY idx_msr_customer (customer_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '量体记录';

-- ---------------------------------------------------------------------------
-- 模块三：面料库存
-- ---------------------------------------------------------------------------
CREATE TABLE fabric (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    fabric_no    VARCHAR(30)   NOT NULL COMMENT '面料编号',
    name         VARCHAR(80)   NOT NULL COMMENT '面料名称',
    composition  VARCHAR(20)   NOT NULL COMMENT '成分 WOOL/CASHMERE/COTTON/SILK/BLEND',
    color        VARCHAR(30)   NOT NULL COMMENT '颜色',
    color_hex    VARCHAR(10)   NOT NULL COMMENT '色卡色值',
    stock_meters DECIMAL(10,2) NOT NULL COMMENT '库存米数',
    unit_price   DECIMAL(10,2) NOT NULL COMMENT '单价（元/米）',
    supplier     VARCHAR(80)   NOT NULL COMMENT '供应商',
    PRIMARY KEY (id),
    UNIQUE KEY uk_fabric_no (fabric_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '面料库存';

-- ---------------------------------------------------------------------------
-- 模块四：定制订单
-- ---------------------------------------------------------------------------
CREATE TABLE custom_order (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    order_no       VARCHAR(30)   NOT NULL COMMENT '订单号',
    customer_id    BIGINT        NOT NULL COMMENT '客户',
    style_id       BIGINT        NOT NULL COMMENT '款式',
    fabric_id      BIGINT        NOT NULL COMMENT '面料',
    measurement_id BIGINT        COMMENT '量体记录',
    usage_meters   DECIMAL(10,2) NOT NULL COMMENT '用量米数',
    order_date     DATE          NOT NULL COMMENT '下单日期',
    delivery_date  DATE          NOT NULL COMMENT '交期',
    fabric_cost    DECIMAL(10,2) NOT NULL COMMENT '面料费用 = 单价 × 用量',
    craft_fee      DECIMAL(10,2) NOT NULL COMMENT '工艺基础价',
    total_price    DECIMAL(10,2) NOT NULL COMMENT '总价',
    deposit        DECIMAL(10,2) NOT NULL COMMENT '定金',
    balance        DECIMAL(10,2) NOT NULL COMMENT '尾款',
    deposit_paid   TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '定金已结',
    balance_paid   TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '尾款已结',
    status         VARCHAR(20)   NOT NULL COMMENT 'ORDERED/MEASURED/CUT/SEWING/FITTING/FINISHED/DELIVERED',
    remark         VARCHAR(500)  COMMENT '备注',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_order_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '定制订单';

-- ---------------------------------------------------------------------------
-- 模块五：工序工单
-- ---------------------------------------------------------------------------
CREATE TABLE work_order (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    wo_no         VARCHAR(30)   NOT NULL COMMENT '工单号',
    order_id      BIGINT        NOT NULL COMMENT '所属订单',
    step          VARCHAR(20)   NOT NULL COMMENT '工序 CUTTING/SEWING/PRESSING/FITTING/OVERLOCK/BUTTON',
    owner         VARCHAR(50)   NOT NULL COMMENT '负责人',
    planned_hours DECIMAL(8,2)  NOT NULL COMMENT '计划工时',
    actual_hours  DECIMAL(8,2)  NOT NULL DEFAULT 0 COMMENT '实际工时',
    status        VARCHAR(20)   NOT NULL COMMENT 'PENDING/RUNNING/DONE/REWORK',
    remark        VARCHAR(500)  COMMENT '备注',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wo_no (wo_no),
    KEY idx_work_order_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工序工单';

-- ---------------------------------------------------------------------------
-- 模块六：试衣整改台 —— 试衣问题单
-- 状态机：PENDING(待处理) → REWORKING(返修中) → REVIEWING(待复核) → CLOSED(已关闭)
-- 普通问题可不经返修直接进入待复核；阻断交付的问题必须关联责任工序并发起返修。
-- released = 1 表示普通问题在订单完工时经明确确认后放行（仍保留未关闭事实与放行记录）。
-- ---------------------------------------------------------------------------
CREATE TABLE fitting_issue (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    issue_no        VARCHAR(30)  NOT NULL COMMENT '问题单号',
    order_id        BIGINT       NOT NULL COMMENT '所属订单（仅试衣中订单可建单）',
    body_part       VARCHAR(30)  NOT NULL COMMENT '问题部位 SHOULDER/CHEST/WAIST/HIP/SLEEVE/COLLAR/LAPEL/HEM/OTHER',
    severity        VARCHAR(20)  NOT NULL COMMENT '严重程度 MINOR(普通)/BLOCKER(阻断交付)',
    description     VARCHAR(500) NOT NULL COMMENT '问题描述',
    responsible_step VARCHAR(20) NOT NULL COMMENT '责任工序 CUTTING/SEWING/OVERLOCK/PRESSING/FITTING/BUTTON',
    raised_by       VARCHAR(50)  NOT NULL COMMENT '提出人',
    raised_at       DATETIME     NOT NULL COMMENT '提出时间',
    status          VARCHAR(20)  NOT NULL COMMENT 'PENDING/REWORKING/REVIEWING/CLOSED',
    released        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '普通问题完工时是否已确认放行',
    closed_at       DATETIME     COMMENT '关闭时间',
    reopen_count    INT          NOT NULL DEFAULT 0 COMMENT '被重新打开的次数',
    version         INT          NOT NULL DEFAULT 0 COMMENT '乐观版本（刷新提示用）',
    remark          VARCHAR(500) COMMENT '备注',
    PRIMARY KEY (id),
    UNIQUE KEY uk_issue_no (issue_no),
    KEY idx_fi_order (order_id),
    KEY idx_fi_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '试衣问题单';

-- ---------------------------------------------------------------------------
-- 模块六：试衣返修轮次
-- 每发起一次返修（含复核不通过开启下一轮、关闭后重新打开）生成独立轮次，
-- 原工序工单的已完成事实与历史工时保持不变，返修工时只记在轮次上。
-- status：RUNNING(返修中) / DONE(已返修待复核) / PASSED(复核通过) / REJECTED(复核不通过)
-- ---------------------------------------------------------------------------
CREATE TABLE fitting_rework_round (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    round_no         VARCHAR(30)   NOT NULL COMMENT '返修工单号',
    issue_id         BIGINT        NOT NULL COMMENT '由哪张试衣问题单触发',
    order_id         BIGINT        NOT NULL COMMENT '所属订单（冗余便于汇总）',
    round_seq        INT           NOT NULL COMMENT '该问题的第几轮返修（从 1 起）',
    responsible_step VARCHAR(20)   NOT NULL COMMENT '责任工序',
    owner            VARCHAR(50)   NOT NULL COMMENT '返修负责人',
    planned_hours    DECIMAL(8,2)  NOT NULL COMMENT '计划工时',
    actual_hours     DECIMAL(8,2)  NOT NULL DEFAULT 0 COMMENT '实际工时',
    rework_reason    VARCHAR(500)  NOT NULL COMMENT '返修原因',
    status           VARCHAR(20)   NOT NULL COMMENT 'RUNNING/DONE/PASSED/REJECTED',
    started_at       DATETIME      NOT NULL COMMENT '发起返修时间',
    finished_at      DATETIME      COMMENT '返修完成时间',
    reviewed_at      DATETIME      COMMENT '复核时间',
    reviewer         VARCHAR(50)   COMMENT '复核人',
    review_comment   VARCHAR(500)  COMMENT '复核意见（通过/不通过原因）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_round_no (round_no),
    KEY idx_frr_issue (issue_id),
    KEY idx_frr_order (order_id),
    -- 数据库兜底：同一问题至多存在一张【有效当前返修工单】（RUNNING/DONE 即仍在返修闭环里）。
    -- active_issue 为生成列：轮次有效时等于 issue_id，终态(PASSED/REJECTED)时为 NULL，唯一索引忽略 NULL。
    active_issue BIGINT GENERATED ALWAYS AS
        (CASE WHEN status IN ('RUNNING','DONE') THEN issue_id ELSE NULL END) STORED,
    UNIQUE KEY uk_frr_active (active_issue)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '试衣返修轮次';

-- ---------------------------------------------------------------------------
-- 模块六：试衣整改操作流水
-- 建单 / 发起返修 / 返修完成 / 提交复核 / 复核通过 / 复核不通过 / 重新打开 / 普通问题放行 / 订单完工 全留痕。
-- ---------------------------------------------------------------------------
CREATE TABLE fitting_action_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    issue_id    BIGINT       COMMENT '关联问题单（订单级动作为空）',
    order_id    BIGINT       NOT NULL COMMENT '所属订单',
    round_id    BIGINT       COMMENT '关联返修轮次',
    action      VARCHAR(30)  NOT NULL COMMENT 'CREATE/START_REWORK/FINISH_REWORK/SUBMIT_REVIEW/REVIEW_PASS/REVIEW_REJECT/REOPEN/RELEASE/FINISH_ORDER',
    operator    VARCHAR(50)  NOT NULL COMMENT '操作人',
    from_status VARCHAR(20)  COMMENT '操作前问题单状态',
    to_status   VARCHAR(20)  COMMENT '操作后问题单状态',
    reason      VARCHAR(500) COMMENT '原因/说明',
    acted_at    DATETIME     NOT NULL COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_fal_issue (issue_id),
    KEY idx_fal_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '试衣整改操作流水';

-- ---------- 客户 ----------
INSERT INTO customer (id, customer_no, name, phone, gender, vip_level) VALUES
  (1, 'KH-2001', '沈曼青', '13801010101', '女', 'GOLD'),
  (2, 'KH-2002', '顾知微', '13801010102', '女', 'GOLD'),
  (3, 'KH-2003', '林婉仪', '13801010103', '女', 'SILVER'),
  (4, 'KH-2004', '裴盈盈', '13801010104', '女', 'NORMAL'),
  (5, 'KH-2005', '苏未晚', '13801010105', '女', 'GOLD'),
  (6, 'KH-2006', '周静姝', '13801010106', '女', 'SILVER'),
  (7, 'KH-2007', '何以柔', '13801010107', '女', 'NORMAL'),
  (8, 'KH-2008', '白锦瑟', '13801010108', '女', 'GOLD'),
  (9, 'KH-2009', '傅清让', '13801010109', '男', 'SILVER'),
  (10, 'KH-2010', '江宴如', '13801010110', '女', 'NORMAL'),
  (11, 'KH-2011', '宋亦舒', '13801010111', '女', 'SILVER'),
  (12, 'KH-2012', '陆时宜', '13801010112', '女', 'GOLD');

-- ---------- 款式 ----------
INSERT INTO style (id, style_no, name, category, base_price, craft_level, on_sale, remark) VALUES
  (1, 'ST-1001', '经典双排扣西装', 'SUIT', 3880.00, 3, 1, '戗驳领，全麻衬工艺'),
  (2, 'ST-1002', '修身单排扣西装', 'SUIT', 3280.00, 3, 1, '两粒扣，半麻衬'),
  (3, 'ST-1003', '英伦格纹大衣', 'OVERCOAT', 5680.00, 4, 1, '双面呢手工缝制'),
  (4, 'ST-1004', '羊绒双面呢大衣', 'OVERCOAT', 6980.00, 5, 1, '手工缲边，穿珠工艺'),
  (5, 'ST-1005', '意式衬衣', 'SHIRT', 980.00, 2, 1, '那不勒斯肩'),
  (6, 'ST-1006', '法式立领衬衣', 'SHIRT', 1180.00, 2, 1, '法式袖口，可做袖扣'),
  (7, 'ST-1007', '晚宴长礼服', 'DRESS', 8800.00, 5, 1, '鱼骨塑身，手钉亮片'),
  (8, 'ST-1008', '轻纱短款礼服', 'DRESS', 5600.00, 4, 1, '多层欧根纱'),
  (9, 'ST-1009', '高腰阔腿裤', 'PANTS', 1280.00, 2, 1, '免烫处理'),
  (10, 'ST-1010', '直筒西裤', 'PANTS', 1080.00, 2, 1, '含防走光衬'),
  (11, 'ST-1011', '手绣礼服套装', 'DRESS', 12800.00, 5, 0, '已停售：绣娘档期排满'),
  (12, 'ST-1012', '亚麻休闲西装', 'SUIT', 2680.00, 2, 1, '轻薄无衬');

-- ---------- 量体记录 ----------
INSERT INTO measurement (id, record_no, customer_id, customer_name, tailor_name, measure_date, bust, waist, hip, shoulder, sleeve_len, body_len, pants_len, remark) VALUES
  (1, 'ME-4001', 1, '沈曼青', '方绮', '2026-08-22', 86.00, 68.00, 92.00, 38.00, 57.00, 62.00, 96.00, '常穿高跟鞋，裤长加 2cm'),
  (2, 'ME-4002', 2, '顾知微', '方绮', '2026-08-23', 90.00, 74.00, 97.00, 39.50, 58.00, 65.00, 99.00, '肩部略偏，需调版'),
  (3, 'ME-4003', 3, '林婉仪', '罗予', '2026-08-24', 82.00, 63.00, 88.00, 36.50, 55.00, 58.00, 92.00, '偏好收腰明显'),
  (4, 'ME-4004', 4, '裴盈盈', '罗予', '2026-08-25', 88.00, 70.00, 95.00, 38.50, 56.50, 63.00, 97.00, '第一次定制，留放余量'),
  (5, 'ME-4005', 5, '苏未晚', '方绮', '2026-08-27', 92.00, 76.00, 99.00, 40.00, 59.00, 66.00, 101.00, '孕期后复尺'),
  (6, 'ME-4006', 6, '周静姝', '罗予', '2026-08-29', 84.00, 66.00, 90.00, 37.00, 55.50, 60.00, 94.00, '袖长偏短 1cm'),
  (7, 'ME-4007', 7, '何以柔', '莫岑', '2026-08-31', 87.00, 71.00, 94.00, 38.00, 57.50, 61.00, 95.00, '要求加薄垫肩'),
  (8, 'ME-4008', 8, '白锦瑟', '莫岑', '2026-09-02', 80.00, 42.00, 86.00, 35.50, 54.00, 56.00, 90.00, '腰围超小，需单独开版'),
  (9, 'ME-4009', 9, '傅清让', '莫岑', '2026-09-04', 98.00, 85.00, 100.00, 45.00, 62.00, 72.00, 104.00, '男士西服套装'),
  (10, 'ME-4010', 10, '江宴如', '方绮', '2026-09-06', 85.00, 67.00, 91.00, 37.50, 56.00, 61.00, 93.00, '偏好宽松版型'),
  (11, 'ME-4011', 11, '宋亦舒', '罗予', '2026-09-08', 89.00, 73.00, 96.00, 39.00, 57.00, 64.00, 98.00, '二次复尺，数据稳定'),
  (12, 'ME-4012', 12, '陆时宜', '莫岑', '2026-09-10', 83.00, 65.00, 89.00, 36.00, 55.00, 59.00, 91.00, '礼服专用净体尺寸');

-- ---------- 面料库存 ----------
INSERT INTO fabric (id, fabric_no, name, composition, color, color_hex, stock_meters, unit_price, supplier) VALUES
  (1, 'FB-3001', '藏青精纺羊毛', 'WOOL', '藏青', '#1f2a44', 186.50, 480.00, '苏州久光毛纺'),
  (2, 'FB-3002', '炭灰羊绒面料', 'CASHMERE', '炭灰', '#4a4a4a', 92.00, 860.00, '鄂尔多斯绒业'),
  (3, 'FB-3003', '米白真丝缎', 'SILK', '米白', '#f2e9dc', 128.00, 320.00, '杭州经纬丝绸'),
  (4, 'FB-3004', '雾霾蓝棉麻布', 'COTTON', '雾霾蓝', '#7d97ab', 240.00, 96.00, '南通家纺一厂'),
  (5, 'FB-3005', '勃艮第红丝绒', 'BLEND', '勃艮第红', '#7d1f35', 18.50, 268.00, '意大利 Baruffa'),
  (6, 'FB-3006', '浅驼格纹呢', 'WOOL', '浅驼', '#c19a6b', 156.00, 410.00, '英国 Lovat Mill'),
  (7, 'FB-3007', '墨绿真丝乔其', 'SILK', '墨绿', '#17503a', 88.00, 356.00, '吴江盛泽绸厂'),
  (8, 'FB-3008', '珍珠白醋酸缎', 'BLEND', '珍珠白', '#efeef2', 210.00, 152.00, '上海纺织控股'),
  (9, 'FB-3009', '烟灰法兰绒', 'WOOL', '烟灰', '#8f9096', 64.00, 388.00, '江苏阳光集团'),
  (10, 'FB-3010', '杏仁奶粉棉布', 'COTTON', '杏仁粉', '#f0d3cd', 300.00, 88.00, '鲁泰纺织'),
  (11, 'FB-3011', '香槟金珠光纱', 'SILK', '香槟金', '#e2c78f', 6.00, 520.00, '法国 Sophie Hallette'),
  (12, 'FB-3012', '深咖羊绒混纺', 'BLEND', '深咖', '#5b3a2b', 132.00, 496.00, '浙江新澳纺织');

-- ---------- 定制订单 ----------
INSERT INTO custom_order (id, order_no, customer_id, style_id, fabric_id, measurement_id, usage_meters, order_date, delivery_date, fabric_cost, craft_fee, total_price, deposit, balance, deposit_paid, balance_paid, status, remark) VALUES
  (1, 'ORD-20260001', 1, 1, 1, 1, 3.60, '2026-09-20', '2026-10-17', 1728.00, 3880.00, 5608.00, 2804.00, 2804.00, 1, 0, 'ORDERED', '客户希望出差前交付'),
  (2, 'ORD-20260002', 2, 3, 6, 2, 4.20, '2026-09-19', '2026-10-20', 1722.00, 5680.00, 7402.00, 3701.00, 3701.00, 1, 0, 'ORDERED', '双面呢手工分部费时'),
  (3, 'ORD-20260003', 3, 5, 3, 3, 2.40, '2026-09-16', '2026-10-14', 768.00, 980.00, 1748.00, 874.00, 874.00, 1, 0, 'MEASURED', '衬衣两件同版'),
  (4, 'ORD-20260004', 4, 6, 4, 4, 2.60, '2026-09-15', '2026-10-16', 249.60, 1180.00, 1429.60, 714.80, 714.80, 1, 0, 'MEASURED', '配母贝袖扣'),
  (5, 'ORD-20260005', 5, 2, 9, 5, 3.80, '2026-09-10', '2026-10-12', 1474.40, 3280.00, 4754.40, 2377.20, 2377.20, 1, 0, 'CUT', '已出纸样'),
  (6, 'ORD-20260006', 6, 10, 10, 6, 1.90, '2026-09-09', '2026-10-10', 167.20, 1080.00, 1247.20, 623.60, 623.60, 1, 0, 'CUT', '西裤配同料腰衬'),
  (7, 'ORD-20260007', 7, 4, 2, 7, 4.50, '2026-09-06', '2026-10-08', 3870.00, 6980.00, 10850.00, 5425.00, 5425.00, 1, 0, 'SEWING', '羊绒大衣要注意绒面'),
  (8, 'ORD-20260008', 8, 7, 7, 8, 5.20, '2026-09-04', '2026-10-13', 1851.20, 8800.00, 10651.20, 5325.60, 5325.60, 1, 1, 'SEWING', '礼服加层网纱'),
  (9, 'ORD-20260009', 9, 12, 12, 9, 3.40, '2026-09-08', '2026-10-06', 1686.40, 2680.00, 4366.40, 2183.20, 2183.20, 1, 0, 'SEWING', '亚麻西装要留沉水余量'),
  (10, 'ORD-20260010', 10, 8, 11, 10, 3.00, '2026-09-01', '2026-10-04', 1560.00, 5600.00, 7160.00, 3580.00, 3580.00, 1, 0, 'FITTING', '一次白坯试身已完成'),
  (11, 'ORD-20260011', 11, 9, 8, 11, 2.10, '2026-08-29', '2026-10-02', 319.20, 1280.00, 1599.20, 799.60, 799.60, 1, 1, 'FITTING', '阔腿裤垂感要够'),
  (12, 'ORD-20260012', 12, 3, 6, 12, 4.00, '2026-08-25', '2026-09-28', 1640.00, 5680.00, 7320.00, 3660.00, 3660.00, 1, 0, 'FINISHED', '整体完成待出仓，尾款未结'),
  (13, 'ORD-20260013', 1, 2, 9, 1, 3.60, '2026-08-19', '2026-09-25', 1396.80, 3280.00, 4676.80, 2338.40, 2338.40, 1, 1, 'DELIVERED', '已交付，客户很满意'),
  (14, 'ORD-20260014', 5, 7, 3, 5, 5.60, '2026-08-13', '2026-09-19', 1792.00, 8800.00, 10592.00, 5296.00, 5296.00, 1, 1, 'DELIVERED', '婚礼已办完');

-- ---------- 工序工单 ----------
INSERT INTO work_order (id, wo_no, order_id, step, owner, planned_hours, actual_hours, status, remark) VALUES
  (1, 'WO-0001', 3, 'CUTTING', '陈师傅', 3.50, 0.00, 'PENDING', '等待排产'),
  (2, 'WO-0002', 4, 'CUTTING', '陈师傅', 3.50, 0.00, 'PENDING', '等待排产'),
  (3, 'WO-0003', 5, 'CUTTING', '陈师傅', 3.50, 3.20, 'DONE', '按工艺要求完成'),
  (4, 'WO-0004', 5, 'SEWING', '王姐', 8.50, 0.00, 'PENDING', '等待排产'),
  (5, 'WO-0005', 5, 'OVERLOCK', '刘工', 2.00, 0.00, 'PENDING', '等待排产'),
  (6, 'WO-0006', 6, 'CUTTING', '陈师傅', 3.50, 3.20, 'DONE', '按工艺要求完成'),
  (7, 'WO-0007', 6, 'SEWING', '王姐', 8.50, 0.00, 'PENDING', '等待排产'),
  (8, 'WO-0008', 6, 'OVERLOCK', '刘工', 2.00, 0.00, 'PENDING', '等待排产'),
  (9, 'WO-0009', 7, 'CUTTING', '陈师傅', 3.50, 3.20, 'DONE', '按工艺要求完成'),
  (10, 'WO-0010', 7, 'SEWING', '王姐', 8.50, 4.23, 'RUNNING', '进行中，进度约 45%'),
  (11, 'WO-0011', 7, 'OVERLOCK', '刘工', 2.00, 0.00, 'PENDING', '等待排产'),
  (12, 'WO-0012', 8, 'CUTTING', '陈师傅', 3.50, 3.20, 'DONE', '按工艺要求完成'),
  (13, 'WO-0013', 8, 'SEWING', '王姐', 8.50, 4.23, 'RUNNING', '进行中，进度约 45%'),
  (14, 'WO-0014', 8, 'OVERLOCK', '刘工', 2.00, 0.00, 'PENDING', '等待排产'),
  (15, 'WO-0015', 9, 'CUTTING', '陈师傅', 3.50, 3.20, 'DONE', '按工艺要求完成'),
  (16, 'WO-0016', 9, 'SEWING', '王姐', 8.50, 4.23, 'RUNNING', '进行中，进度约 45%'),
  (17, 'WO-0017', 9, 'OVERLOCK', '刘工', 2.00, 2.60, 'REWORK', '锁边线迹不匀，需拆线重锁'),
  (18, 'WO-0018', 10, 'CUTTING', '陈师傅', 3.50, 3.20, 'DONE', '按工艺要求完成'),
  (19, 'WO-0019', 10, 'SEWING', '王姐', 8.50, 9.40, 'DONE', '按工艺要求完成'),
  (20, 'WO-0020', 10, 'OVERLOCK', '刘工', 2.00, 2.10, 'DONE', '按工艺要求完成'),
  (21, 'WO-0021', 10, 'PRESSING', '孙阿姨', 1.50, 1.50, 'DONE', '按工艺要求完成'),
  (22, 'WO-0022', 10, 'FITTING', '方绮', 2.00, 1.08, 'RUNNING', '进行中，进度约 45%'),
  (23, 'WO-0023', 11, 'CUTTING', '陈师傅', 3.50, 3.20, 'DONE', '按工艺要求完成'),
  (24, 'WO-0024', 11, 'SEWING', '王姐', 8.50, 9.40, 'DONE', '按工艺要求完成'),
  (25, 'WO-0025', 11, 'OVERLOCK', '刘工', 2.00, 2.10, 'DONE', '按工艺要求完成'),
  (26, 'WO-0026', 11, 'PRESSING', '孙阿姨', 1.50, 1.50, 'DONE', '按工艺要求完成'),
  (27, 'WO-0027', 11, 'FITTING', '方绮', 2.00, 1.08, 'RUNNING', '进行中，进度约 45%'),
  (28, 'WO-0028', 12, 'CUTTING', '陈师傅', 3.50, 3.20, 'DONE', '按工艺要求完成'),
  (29, 'WO-0029', 12, 'SEWING', '王姐', 8.50, 9.40, 'DONE', '按工艺要求完成'),
  (30, 'WO-0030', 12, 'OVERLOCK', '刘工', 2.00, 2.10, 'DONE', '按工艺要求完成'),
  (31, 'WO-0031', 12, 'PRESSING', '孙阿姨', 1.50, 1.50, 'DONE', '按工艺要求完成'),
  (32, 'WO-0032', 12, 'FITTING', '方绮', 2.00, 2.40, 'DONE', '按工艺要求完成'),
  (33, 'WO-0033', 12, 'BUTTON', '王姐', 1.00, 1.10, 'DONE', '按工艺要求完成'),
  (34, 'WO-0034', 13, 'CUTTING', '陈师傅', 3.50, 3.20, 'DONE', '按工艺要求完成'),
  (35, 'WO-0035', 13, 'SEWING', '王姐', 8.50, 9.40, 'DONE', '按工艺要求完成'),
  (36, 'WO-0036', 13, 'OVERLOCK', '刘工', 2.00, 2.10, 'DONE', '按工艺要求完成'),
  (37, 'WO-0037', 13, 'PRESSING', '孙阿姨', 1.50, 1.50, 'DONE', '按工艺要求完成'),
  (38, 'WO-0038', 13, 'FITTING', '方绮', 2.00, 2.40, 'DONE', '按工艺要求完成'),
  (39, 'WO-0039', 13, 'BUTTON', '王姐', 1.00, 1.10, 'DONE', '按工艺要求完成'),
  (40, 'WO-0040', 14, 'CUTTING', '陈师傅', 3.50, 3.20, 'DONE', '按工艺要求完成'),
  (41, 'WO-0041', 14, 'SEWING', '王姐', 8.50, 9.40, 'DONE', '按工艺要求完成'),
  (42, 'WO-0042', 14, 'OVERLOCK', '刘工', 2.00, 2.10, 'DONE', '按工艺要求完成'),
  (43, 'WO-0043', 14, 'PRESSING', '孙阿姨', 1.50, 1.50, 'DONE', '按工艺要求完成'),
  (44, 'WO-0044', 14, 'FITTING', '方绮', 2.00, 2.40, 'DONE', '按工艺要求完成'),
  (45, 'WO-0045', 14, 'BUTTON', '王姐', 1.00, 1.10, 'DONE', '按工艺要求完成');

-- ---------- 试衣问题单（仅试衣中订单 ORD-20260010 / ORD-20260011） ----------
-- FI-5001：阻断问题，第 1 轮返修复核不通过，第 2 轮已返修完成、当前待复核
INSERT INTO fitting_issue
  (id, issue_no, order_id, body_part, severity, description, responsible_step, raised_by, raised_at,
   status, released, closed_at, reopen_count, version, remark) VALUES
  (1, 'FI-5001', 10, 'SLEEVE', 'BLOCKER', '左臂袖长偏短约 1.2cm，抬臂时吊紧，无法交付', 'SEWING', '方绮',
   '2026-09-22 10:20:00', 'REVIEWING', 0, NULL, 0, 3, '一次白坯试身发现'),
  (2, 'FI-5002', 10, 'COLLAR', 'MINOR', '领座后中轻微起绺，熨烫可改善，不影响穿着交付', 'PRESSING', '方绮',
   '2026-09-22 10:25:00', 'PENDING', 0, NULL, 0, 0, '普通问题，可完工时确认放行'),
  (3, 'FI-5003', 11, 'WAIST', 'BLOCKER', '裤腰左右高低差 0.8cm，需重新缝制腰头', 'SEWING', '方绮',
   '2026-09-21 15:00:00', 'REWORKING', 0, NULL, 0, 1, '返修工单进行中'),
  (4, 'FI-5004', 11, 'HEM', 'MINOR', '裤脚撬边有两处线迹外露，建议钉扣工序顺手补暗针', 'BUTTON', '罗予',
   '2026-09-21 15:08:00', 'CLOSED', 0, '2026-09-23 11:40:00', 0, 2, '复核通过后关闭，已无新缺陷');

-- ---------- 试衣返修轮次（每轮独立，原工单工时不动） ----------
INSERT INTO fitting_rework_round
  (id, round_no, issue_id, order_id, round_seq, responsible_step, owner, planned_hours, actual_hours,
   rework_reason, status, started_at, finished_at, reviewed_at, reviewer, review_comment) VALUES
  (1, 'RR-6001', 1, 10, 1, 'SEWING', '王姐', 2.00, 2.20, '左袖放长 1.2cm，重新上袖口', 'REJECTED',
   '2026-09-22 10:40:00', '2026-09-22 16:10:00', '2026-09-23 09:30:00', '方绮', '袖长改到位，但袖衩起吊，需打开重放平服'),
  (2, 'RR-6002', 1, 10, 2, 'SEWING', '王姐', 1.50, 1.40, '打开左袖袖衩重新放平，顺袖窿微调', 'DONE',
   '2026-09-23 09:50:00', '2026-09-23 17:20:00', NULL, NULL, NULL),
  (3, 'RR-6003', 3, 11, 1, 'SEWING', '王姐', 2.50, 0.00, '拆腰头重缝，校正左右高低', 'RUNNING',
   '2026-09-22 09:00:00', NULL, NULL, NULL, NULL),
  (4, 'RR-6004', 4, 11, 1, 'BUTTON', '王姐', 0.50, 0.40, '裤脚两处外露线迹补暗针', 'PASSED',
   '2026-09-22 14:00:00', '2026-09-22 17:30:00', '2026-09-23 11:40:00', '方绮', '复验线迹隐蔽，通过');

-- ---------- 试衣整改操作流水 ----------
INSERT INTO fitting_action_log
  (id, issue_id, order_id, round_id, action, operator, from_status, to_status, reason, acted_at) VALUES
  (1, 1, 10, NULL, 'CREATE', '方绮', NULL, 'PENDING', '一次白坯试身发现左袖偏短', '2026-09-22 10:20:00'),
  (2, 1, 10, 1, 'START_REWORK', '方绮', 'PENDING', 'REWORKING', '左袖放长 1.2cm，重新上袖口', '2026-09-22 10:40:00'),
  (3, 1, 10, 1, 'FINISH_REWORK', '王姐', 'REWORKING', 'REWORKING', '第 1 轮返修完成，实耗 2.2 小时', '2026-09-22 16:10:00'),
  (4, 1, 10, 1, 'SUBMIT_REVIEW', '王姐', 'REWORKING', 'REVIEWING', '提交复核', '2026-09-22 16:15:00'),
  (5, 1, 10, 1, 'REVIEW_REJECT', '方绮', 'REVIEWING', 'REWORKING', '袖衩起吊，需打开重放平服', '2026-09-23 09:30:00'),
  (6, 1, 10, 2, 'START_REWORK', '方绮', 'REWORKING', 'REWORKING', '复核不通过，开启第 2 轮返修', '2026-09-23 09:50:00'),
  (7, 1, 10, 2, 'FINISH_REWORK', '王姐', 'REWORKING', 'REWORKING', '第 2 轮返修完成，实耗 1.4 小时', '2026-09-23 17:20:00'),
  (8, 1, 10, 2, 'SUBMIT_REVIEW', '王姐', 'REWORKING', 'REVIEWING', '提交复核', '2026-09-23 17:25:00'),
  (9, 2, 10, NULL, 'CREATE', '方绮', NULL, 'PENDING', '领座后中轻微起绺', '2026-09-22 10:25:00'),
  (10, 3, 11, NULL, 'CREATE', '方绮', NULL, 'PENDING', '裤腰左右高低差 0.8cm', '2026-09-21 15:00:00'),
  (11, 3, 11, 3, 'START_REWORK', '方绮', 'PENDING', 'REWORKING', '拆腰头重缝，校正左右高低', '2026-09-22 09:00:00'),
  (12, 4, 11, NULL, 'CREATE', '罗予', NULL, 'PENDING', '裤脚两处线迹外露', '2026-09-21 15:08:00'),
  (13, 4, 11, 4, 'START_REWORK', '罗予', 'PENDING', 'REWORKING', '裤脚补暗针', '2026-09-22 14:00:00'),
  (14, 4, 11, 4, 'FINISH_REWORK', '王姐', 'REWORKING', 'REWORKING', '返修完成，实耗 0.4 小时', '2026-09-22 17:30:00'),
  (15, 4, 11, 4, 'SUBMIT_REVIEW', '王姐', 'REWORKING', 'REVIEWING', '提交复核', '2026-09-22 17:35:00'),
  (16, 4, 11, 4, 'REVIEW_PASS', '方绮', 'REVIEWING', 'CLOSED', '复验线迹隐蔽，通过', '2026-09-23 11:40:00');
