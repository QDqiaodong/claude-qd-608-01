package com.garment.factory.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.garment.factory.common.BizException;
import com.garment.factory.domain.Customer;
import com.garment.factory.domain.GarmentDict;
import com.garment.factory.domain.Measurement;
import com.garment.factory.dto.MeasurementListItemDto;
import com.garment.factory.dto.MeasurementSaveReq;
import com.garment.factory.dto.SliceView;
import com.garment.factory.repository.CustomerRepository;
import com.garment.factory.repository.MeasurementRepository;

/**
 * 量体记录：部位尺寸的合理区间在录入时强校验。
 */
@Service
public class MeasurementService {

    private final MeasurementRepository measurementRepository;
    private final CustomerRepository customerRepository;

    public MeasurementService(MeasurementRepository measurementRepository, CustomerRepository customerRepository) {
        this.measurementRepository = measurementRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public SliceView<MeasurementListItemDto> list(Long customerId, String tailorName, int page, int size) {
        Slice<MeasurementListItemDto> slice = measurementRepository.sliceBy(customerId, trim(tailorName),
                PageRequest.of(page, size));
        return SliceView.of(slice);
    }

    @Transactional(readOnly = true)
    public List<MeasurementListItemDto> listByCustomer(Long customerId) {
        return measurementRepository.sliceByCustomer(customerId, PageRequest.of(0, 50)).getContent();
    }

    @Transactional(readOnly = true)
    public List<String> tailors() {
        return measurementRepository.sliceBy(null, "", PageRequest.of(0, 500)).getContent()
                .stream().map(MeasurementListItemDto::getTailorName).distinct().sorted().toList();
    }

    @Transactional(readOnly = true)
    public Measurement detail(Long id) {
        return measurementRepository.findById(id).orElseThrow(() -> new BizException("量体记录不存在"));
    }

    @Transactional
    public Measurement save(MeasurementSaveReq req) {
        validate(req);
        Measurement record = req.getId() == null ? new Measurement() : require(req.getId());
        if (req.getId() == null && measurementRepository.existsByRecordNo(req.getRecordNo())) {
            throw new BizException("量体单号已存在：" + req.getRecordNo());
        }
        if (req.getId() != null && measurementRepository.existsByRecordNoAndIdNot(req.getRecordNo(), req.getId())) {
            throw new BizException("量体单号已被其它记录占用：" + req.getRecordNo());
        }
        String customerName = requireCustomer(req.getCustomerId()).getName();
        fill(record, req, customerName);
        return measurementRepository.save(record);
    }

    @Transactional
    public void remove(Long id) {
        measurementRepository.delete(require(id));
    }

    /**
     * 校验并新建一条量体记录，供下单向导第 2 步复用。
     */
    public Measurement createForOrder(MeasurementSaveReq req, Customer customer) {
        validate(req);
        if (measurementRepository.existsByRecordNo(req.getRecordNo())) {
            throw new BizException("量体单号已存在：" + req.getRecordNo());
        }
        Measurement record = new Measurement();
        fill(record, req, customer.getName());
        return measurementRepository.save(record);
    }

    private void fill(Measurement record, MeasurementSaveReq req, String customerName) {
        record.setRecordNo(req.getRecordNo());
        record.setCustomerId(req.getCustomerId());
        record.setCustomerName(customerName);
        record.setTailorName(req.getTailorName());
        record.setMeasureDate(req.getMeasureDate());
        record.setBust(req.getBust());
        record.setWaist(req.getWaist());
        record.setHip(req.getHip());
        record.setShoulder(req.getShoulder());
        record.setSleeveLength(req.getSleeveLength());
        record.setBodyLength(req.getBodyLength());
        record.setPantsLength(req.getPantsLength());
        record.setRemark(req.getRemark());
    }

    private void validate(MeasurementSaveReq req) {
        if (req.getCustomerId() == null) {
            throw new BizException("请先选择客户");
        }
        Customer customer = requireCustomer(req.getCustomerId());
        // 客户存在性校验后回填，避免下单向导里漏传
        req.setCustomerId(customer.getId());
        if (req.getRecordNo() == null || req.getRecordNo().isBlank()) {
            throw new BizException("量体单号不能为空");
        }
        if (req.getTailorName() == null || req.getTailorName().isBlank()) {
            throw new BizException("请填写量体师");
        }
        if (req.getMeasureDate() == null) {
            throw new BizException("请选择量体日期");
        }
        if (req.getMeasureDate().isAfter(LocalDate.now())) {
            throw new BizException("量体日期不能晚于今天");
        }
        checkPart("bust", req.getBust());
        checkPart("waist", req.getWaist());
        checkPart("hip", req.getHip());
        checkPart("shoulder", req.getShoulder());
        checkPart("sleeveLength", req.getSleeveLength());
        checkPart("bodyLength", req.getBodyLength());
        checkPart("pantsLength", req.getPantsLength());
        if (req.getHip().compareTo(req.getWaist()) < 0) {
            throw new BizException("臀围不应该小于腰围，请复核尺寸");
        }
        if (req.getBust().compareTo(req.getWaist()) < 0) {
            throw new BizException("胸围不应该小于腰围，请复核尺寸");
        }
    }

    private void checkPart(String part, BigDecimal value) {
        if (value == null) {
            throw new BizException(GarmentDict.partName(part) + "不能为空");
        }
        if (!GarmentDict.validPart(part, value)) {
            throw new BizException(GarmentDict.partName(part) + "应在 " + GarmentDict.partMin(part)
                    + " ~ " + GarmentDict.partMax(part) + " cm 之间");
        }
    }

    private Customer requireCustomer(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> new BizException("客户不存在，请先在客户列表中确认"));
    }

    private Measurement require(Long id) {
        return measurementRepository.findById(id).orElseThrow(() -> new BizException("量体记录不存在"));
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    /** 各部位合理区间，给前端提示用 */
    public Map<String, Map<String, BigDecimal>> partRange() {
        return GarmentDict.partRanges();
    }
}
