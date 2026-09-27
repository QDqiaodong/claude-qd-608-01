package com.garment.factory.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.garment.factory.common.BizException;
import com.garment.factory.domain.GarmentDict;
import com.garment.factory.domain.Style;
import com.garment.factory.dto.SliceView;
import com.garment.factory.dto.StyleListItemDto;
import com.garment.factory.dto.StyleSaveReq;
import com.garment.factory.repository.CustomOrderRepository;
import com.garment.factory.repository.StyleRepository;

/**
 * 款式档案：品类 / 基础价 / 工艺难度 / 是否在售。
 */
@Service
public class StyleService {

    private final StyleRepository styleRepository;
    private final CustomOrderRepository orderRepository;

    public StyleService(StyleRepository styleRepository, CustomOrderRepository orderRepository) {
        this.styleRepository = styleRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public SliceView<StyleListItemDto> list(String category, Boolean onSale, String keyword, int page, int size) {
        Slice<StyleListItemDto> slice = styleRepository.sliceBy(category, onSale, trim(keyword),
                PageRequest.of(page, size));
        return SliceView.of(slice);
    }

    @Transactional(readOnly = true)
    public Style detail(Long id) {
        return require(id);
    }

    @Transactional(readOnly = true)
    public List<Style> onSaleStyles() {
        return styleRepository.findAll().stream().filter(s -> Boolean.TRUE.equals(s.getOnSale())).toList();
    }

    @Transactional
    public Style save(StyleSaveReq req) {
        requireValid(req);
        Style style = req.getId() == null ? new Style() : require(req.getId());
        if (req.getId() == null && styleRepository.existsByStyleNo(req.getStyleNo())) {
            throw new BizException("款号已存在：" + req.getStyleNo());
        }
        if (req.getId() != null && styleRepository.existsByStyleNoAndIdNot(req.getStyleNo(), req.getId())) {
            throw new BizException("款号已被其它款式占用：" + req.getStyleNo());
        }
        style.setStyleNo(req.getStyleNo());
        style.setName(req.getName());
        style.setCategory(req.getCategory());
        style.setBasePrice(req.getBasePrice());
        style.setCraftLevel(req.getCraftLevel());
        style.setOnSale(req.getOnSale() == null ? Boolean.TRUE : req.getOnSale());
        style.setRemark(req.getRemark());
        return styleRepository.save(style);
    }

    @Transactional
    public void remove(Long id) {
        long used = orderRepository.countByStyleId(id);
        if (used > 0) {
            throw new BizException("该款式已被 " + used + " 张定制订单引用，不能删除");
        }
        styleRepository.delete(require(id));
    }

    @Transactional
    public Style toggleSale(Long id) {
        Style style = require(id);
        style.setOnSale(!Boolean.TRUE.equals(style.getOnSale()));
        return styleRepository.save(style);
    }

    private void requireValid(StyleSaveReq req) {
        if (req.getStyleNo() == null || req.getStyleNo().isBlank()) {
            throw new BizException("款号不能为空");
        }
        if (req.getName() == null || req.getName().isBlank()) {
            throw new BizException("款式名称不能为空");
        }
        if (!GarmentDict.isValidCategory(req.getCategory())) {
            throw new BizException("品类不合法，只能是 西装/大衣/衬衫/礼服/裤装");
        }
        BigDecimal minPrice = GarmentDict.categoryMinPrice(req.getCategory());
        if (req.getBasePrice() == null || req.getBasePrice().compareTo(minPrice) < 0) {
            throw new BizException(GarmentDict.categoryName(req.getCategory()) + "的基础价不能低于 " + minPrice + " 元");
        }
        if (req.getCraftLevel() == null || req.getCraftLevel() < 1 || req.getCraftLevel() > GarmentDict.MAX_CRAFT_LEVEL) {
            throw new BizException("工艺难度只能是 1 到 " + GarmentDict.MAX_CRAFT_LEVEL + " 级");
        }
    }

    private Style require(Long id) {
        return styleRepository.findById(id).orElseThrow(() -> new BizException("款式不存在"));
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    /** 品类 → 推荐用料（米），向导第三步默认值 */
    public Map<String, BigDecimal> defaultMeters() {
        Map<String, BigDecimal> map = new java.util.LinkedHashMap<>();
        GarmentDict.categories().forEach((code, name) ->
                map.put(code, GarmentDict.categoryDefaultMeters(code)));
        return map;
    }
}
