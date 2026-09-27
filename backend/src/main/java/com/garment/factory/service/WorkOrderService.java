package com.garment.factory.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.garment.factory.common.BizException;
import com.garment.factory.domain.CustomOrder;
import com.garment.factory.domain.GarmentDict;
import com.garment.factory.domain.WorkOrder;
import com.garment.factory.dto.SliceView;
import com.garment.factory.dto.WorkOrderAdvanceReq;
import com.garment.factory.dto.WorkOrderListItemDto;
import com.garment.factory.dto.WorkOrderSaveReq;
import com.garment.factory.repository.CustomOrderRepository;
import com.garment.factory.repository.WorkOrderRepository;

/**
 * 工序工单：待开工 → 进行中 → 已完成，已完成的可以判返工。
 */
@Service
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final CustomOrderRepository orderRepository;

    public WorkOrderService(WorkOrderRepository workOrderRepository, CustomOrderRepository orderRepository) {
        this.workOrderRepository = workOrderRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public SliceView<WorkOrderListItemDto> list(Long orderId, String status, String step, String owner,
                                                String keyword, int page, int size) {
        Slice<WorkOrderListItemDto> slice = workOrderRepository.sliceBy(orderId, trim(status), trim(step),
                trim(owner), trim(keyword), PageRequest.of(page, size));
        return SliceView.of(slice);
    }

    @Transactional(readOnly = true)
    public WorkOrder detail(Long id) {
        return workOrderRepository.findById(id).orElseThrow(() -> new BizException("工单不存在"));
    }

    @Transactional
    public WorkOrder save(WorkOrderSaveReq req) {
        requireValid(req);
        WorkOrder workOrder = req.getId() == null ? new WorkOrder() : require(req.getId());
        if (req.getId() == null && workOrderRepository.existsByWoNo(req.getWoNo())) {
            throw new BizException("工单号已存在：" + req.getWoNo());
        }
        if (req.getId() != null && workOrderRepository.existsByWoNoAndIdNot(req.getWoNo(), req.getId())) {
            throw new BizException("工单号已被其它工单占用：" + req.getWoNo());
        }
        long dup = workOrderRepository.countByOrderIdAndStep(req.getOrderId(), req.getStep());
        if (dup > 0 && (req.getId() == null || !req.getStep().equals(workOrder.getStep()))) {
            throw new BizException("该订单已经有「" + GarmentDict.stepName(req.getStep()) + "」工序工单，不能重复派工");
        }
        workOrder.setWoNo(req.getWoNo());
        workOrder.setOrderId(req.getOrderId());
        workOrder.setStep(req.getStep());
        workOrder.setOwner(req.getOwner());
        workOrder.setPlannedHours(req.getPlannedHours());
        workOrder.setRemark(req.getRemark());
        if (req.getId() == null) {
            workOrder.setActualHours(BigDecimal.ZERO);
            workOrder.setStatus("PENDING");
        }
        return workOrderRepository.save(workOrder);
    }

    @Transactional
    public void remove(Long id) {
        WorkOrder workOrder = require(id);
        if ("RUNNING".equals(workOrder.getStatus())) {
            throw new BizException("进行中的工单不能删除，请先停工或推进到已完成");
        }
        workOrderRepository.delete(workOrder);
    }

    /**
     * 状态推进：待开工 → 进行中 → 已完成；已完成的工单可判返工，返工后重新进入进行中。
     */
    @Transactional
    public WorkOrder advance(Long id, WorkOrderAdvanceReq req) {
        WorkOrder workOrder = require(id);
        String target = req.getTargetStatus();
        if (!GarmentDict.isValidWoStatus(target)) {
            throw new BizException("目标状态不合法");
        }
        if (target.equals(workOrder.getStatus())) {
            throw new BizException("工单已经是「" + GarmentDict.woStatusName(target) + "」了");
        }
        CustomOrder order = orderRepository.findById(workOrder.getOrderId())
                .orElseThrow(() -> new BizException("所属订单不存在"));
        if ("DELIVERED".equals(order.getStatus())) {
            throw new BizException("订单已交付，不能再变更工序工单");
        }
        switch (target) {
            case "RUNNING" -> {
                if (!"PENDING".equals(workOrder.getStatus()) && !"REWORK".equals(workOrder.getStatus())) {
                    throw new BizException("只有待开工或返工的工单可以开工");
                }
            }
            case "DONE" -> {
                if (!"RUNNING".equals(workOrder.getStatus())) {
                    throw new BizException("只有进行中的工单可以标记完成");
                }
                BigDecimal hours = req.getActualHours() == null ? workOrder.getActualHours() : req.getActualHours();
                if (hours == null || hours.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BizException("标记完成前必须填写实际工时，且必须大于 0");
                }
                BigDecimal limit = workOrder.getPlannedHours().multiply(GarmentDict.MAX_HOURS_RATE);
                if (hours.compareTo(limit) > 0) {
                    throw new BizException("实际工时不能超过计划工时的 " + GarmentDict.MAX_HOURS_RATE + " 倍（上限 "
                            + limit + " 小时）");
                }
                workOrder.setActualHours(hours);
            }
            case "REWORK" -> {
                if (!"DONE".equals(workOrder.getStatus())) {
                    throw new BizException("只有已完成的工单可以判返工");
                }
                if (req.getRemark() == null || req.getRemark().isBlank()) {
                    throw new BizException("判返工必须填写返工原因");
                }
                workOrder.setRemark("[返工] " + req.getRemark());
            }
            case "PENDING" -> throw new BizException("不能把工单退回待开工，请先走返工流程");
            default -> throw new BizException("不支持的目标状态：" + target);
        }
        if (req.getRemark() != null && !req.getRemark().isBlank() && !"REWORK".equals(target)) {
            workOrder.setRemark(req.getRemark());
        }
        workOrder.setStatus(target);
        return workOrderRepository.save(workOrder);
    }

    /**
     * 工时统计：按状态汇总单数与工时，并给出各工序的计划/实际工时对比。
     */
    @Transactional(readOnly = true)
    public Map<String, Object> stats(Long orderId) {
        List<WorkOrderListItemDto> rows = workOrderRepository.sliceBy(orderId, "", "", "", "",
                PageRequest.of(0, 2000)).getContent();
        Map<String, Object> result = new LinkedHashMap<>();
        long total = rows.size();
        BigDecimal planned = BigDecimal.ZERO;
        BigDecimal actual = BigDecimal.ZERO;
        Map<String, Long> statusCount = new LinkedHashMap<>();
        Map<String, BigDecimal> stepPlanned = new LinkedHashMap<>();
        Map<String, BigDecimal> stepActual = new LinkedHashMap<>();
        for (String code : GarmentDict.woStatuses().keySet()) {
            statusCount.put(code, 0L);
        }
        for (WorkOrderListItemDto row : rows) {
            statusCount.merge(row.getStatus(), 1L, Long::sum);
            planned = planned.add(row.getPlannedHours());
            actual = actual.add(row.getActualHours());
            stepPlanned.merge(row.getStep(), row.getPlannedHours(), BigDecimal::add);
            stepActual.merge(row.getStep(), row.getActualHours(), BigDecimal::add);
        }
        result.put("total", total);
        result.put("plannedHours", planned);
        result.put("actualHours", actual);
        result.put("diffHours", actual.subtract(planned));
        result.put("statusCount", statusCount);
        List<Map<String, Object>> byStep = new ArrayList<>();
        for (String step : GarmentDict.steps().keySet()) {
            BigDecimal p = stepPlanned.getOrDefault(step, BigDecimal.ZERO);
            BigDecimal a = stepActual.getOrDefault(step, BigDecimal.ZERO);
            if (p.compareTo(BigDecimal.ZERO) == 0 && a.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("step", step);
            row.put("stepName", GarmentDict.stepName(step));
            row.put("plannedHours", p);
            row.put("actualHours", a);
            row.put("diffHours", a.subtract(p));
            byStep.add(row);
        }
        result.put("byStep", byStep);
        return result;
    }

    /**
     * 订单投产：按标准工序序列一次性生成待开工工单。
     */
    @Transactional
    public List<WorkOrder> generateForOrder(CustomOrder order) {
        List<WorkOrder> created = new ArrayList<>();
        long seq = workOrderRepository.count();
        for (String step : GarmentDict.stepSequence()) {
            WorkOrder workOrder = new WorkOrder();
            seq++;
            workOrder.setWoNo(String.format("WO-%04d", seq));
            workOrder.setOrderId(order.getId());
            workOrder.setStep(step);
            workOrder.setOwner(GarmentDict.stepOwner(step));
            workOrder.setPlannedHours(GarmentDict.stepHours(step));
            workOrder.setActualHours(BigDecimal.ZERO);
            workOrder.setStatus("PENDING");
            workOrder.setRemark("按订单投产自动生成");
            created.add(workOrderRepository.save(workOrder));
        }
        return created;
    }

    private void requireValid(WorkOrderSaveReq req) {
        if (req.getWoNo() == null || req.getWoNo().isBlank()) {
            throw new BizException("工单号不能为空");
        }
        if (req.getOrderId() == null) {
            throw new BizException("请选择所属订单");
        }
        CustomOrder order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> new BizException("所属订单不存在"));
        if ("DELIVERED".equals(order.getStatus())) {
            throw new BizException("订单已交付，不能再派工");
        }
        if (!GarmentDict.isValidStep(req.getStep())) {
            throw new BizException("工序不合法");
        }
        if (req.getOwner() == null || req.getOwner().isBlank()) {
            throw new BizException("请填写负责人");
        }
        if (req.getPlannedHours() == null || req.getPlannedHours().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("计划工时必须大于 0");
        }
        if (req.getPlannedHours().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BizException("单个工序计划工时不能超过 100 小时");
        }
    }

    private WorkOrder require(Long id) {
        return workOrderRepository.findById(id).orElseThrow(() -> new BizException("工单不存在"));
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
