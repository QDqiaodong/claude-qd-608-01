package com.garment.factory.service;

import java.math.BigDecimal;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.garment.factory.common.BizException;
import com.garment.factory.domain.Fabric;
import com.garment.factory.domain.GarmentDict;
import com.garment.factory.dto.FabricCardDto;
import com.garment.factory.dto.FabricSaveReq;
import com.garment.factory.dto.SliceView;
import com.garment.factory.repository.CustomOrderRepository;
import com.garment.factory.repository.FabricRepository;

/**
 * 面料库存：按米管理，低于预警线在卡片墙上标红。
 */
@Service
public class FabricService {

    private final FabricRepository fabricRepository;
    private final CustomOrderRepository orderRepository;

    public FabricService(FabricRepository fabricRepository, CustomOrderRepository orderRepository) {
        this.fabricRepository = fabricRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public SliceView<FabricCardDto> list(String composition, Boolean lowOnly, String keyword, int page, int size) {
        Slice<FabricCardDto> slice = fabricRepository.sliceBy(composition, lowOnly == null ? Boolean.FALSE : lowOnly,
                trim(keyword), GarmentDict.LOW_STOCK_LINE, PageRequest.of(page, size));
        return SliceView.of(slice);
    }

    @Transactional(readOnly = true)
    public Fabric detail(Long id) {
        return fabricRepository.findById(id).orElseThrow(() -> new BizException("面料不存在"));
    }

    @Transactional(readOnly = true)
    public long lowStockCount() {
        return fabricRepository.sliceBy("", Boolean.TRUE, "", GarmentDict.LOW_STOCK_LINE,
                PageRequest.of(0, 500)).getContent().size();
    }

    @Transactional
    public Fabric save(FabricSaveReq req) {
        requireValid(req);
        Fabric fabric = req.getId() == null ? new Fabric() : require(req.getId());
        if (req.getId() == null && fabricRepository.existsByFabricNo(req.getFabricNo())) {
            throw new BizException("面料编号已存在：" + req.getFabricNo());
        }
        if (req.getId() != null && fabricRepository.existsByFabricNoAndIdNot(req.getFabricNo(), req.getId())) {
            throw new BizException("面料编号已被其它面料占用：" + req.getFabricNo());
        }
        fabric.setFabricNo(req.getFabricNo());
        fabric.setName(req.getName());
        fabric.setComposition(req.getComposition());
        fabric.setColor(req.getColor());
        fabric.setColorHex(req.getColorHex());
        fabric.setStockMeters(req.getStockMeters());
        fabric.setUnitPrice(req.getUnitPrice());
        fabric.setSupplier(req.getSupplier());
        return fabricRepository.save(fabric);
    }

    @Transactional
    public Fabric restock(Long id, BigDecimal delta) {
        if (delta == null || delta.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("补货数量必须大于 0");
        }
        if (delta.compareTo(BigDecimal.valueOf(10000)) > 0) {
            throw new BizException("单次补货不能超过 10000 米");
        }
        require(id);
        fabricRepository.restock(id, delta);
        return require(id);
    }

    @Transactional
    public void remove(Long id) {
        long used = orderRepository.countByFabricId(id);
        if (used > 0) {
            throw new BizException("该面料已被 " + used + " 张定制订单选用，不能删除");
        }
        fabricRepository.delete(require(id));
    }

    private void requireValid(FabricSaveReq req) {
        if (req.getFabricNo() == null || req.getFabricNo().isBlank()) {
            throw new BizException("面料编号不能为空");
        }
        if (req.getName() == null || req.getName().isBlank()) {
            throw new BizException("面料名称不能为空");
        }
        if (!GarmentDict.isValidComposition(req.getComposition())) {
            throw new BizException("成分不合法，只能是 羊毛/羊绒/棉/真丝/混纺");
        }
        if (req.getColor() == null || req.getColor().isBlank()) {
            throw new BizException("请填写颜色");
        }
        if (req.getColorHex() == null || !req.getColorHex().matches("^#[0-9A-Fa-f]{6}$")) {
            throw new BizException("色卡色值格式应为 #RRGGBB");
        }
        if (req.getStockMeters() == null || req.getStockMeters().compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("库存米数不能为负");
        }
        if (req.getUnitPrice() == null || req.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("单价（元/米）必须大于 0");
        }
        if (req.getSupplier() == null || req.getSupplier().isBlank()) {
            throw new BizException("请填写供应商");
        }
    }

    private Fabric require(Long id) {
        return fabricRepository.findById(id).orElseThrow(() -> new BizException("面料不存在"));
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
