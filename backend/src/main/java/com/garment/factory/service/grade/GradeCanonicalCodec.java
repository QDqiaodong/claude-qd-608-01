package com.garment.factory.service.grade;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.garment.factory.common.BizException;
import com.garment.factory.domain.GradeDict;

/**
 * 规范化方案与 JSON 列互转（sizes / bases / rules 三列都以毫米存储）。
 * 读取时严格按毫米解释：旧厘米数据会在迁移层先乘 10 再写入，这里永远不会把厘米误当毫米。
 */
@Component
public class GradeCanonicalCodec {

    private final ObjectMapper mapper = new ObjectMapper();

    public String writeSizes(List<String> sizes) {
        try {
            return mapper.writeValueAsString(sizes);
        } catch (Exception e) {
            throw new BizException("尺码序列序列化失败：" + e.getMessage());
        }
    }

    public List<String> readSizes(String json) {
        try {
            List<String> sizes = new ArrayList<>();
            for (JsonNode node : mapper.readTree(json)) {
                sizes.add(node.asText());
            }
            return sizes;
        } catch (Exception e) {
            throw new BizException("尺码序列已损坏，无法读取");
        }
    }

    public String writeBases(Map<PartSide, BigDecimal> basesMm) {
        ArrayNode arr = mapper.createArrayNode();
        for (Map.Entry<PartSide, BigDecimal> e : basesMm.entrySet()) {
            ObjectNode node = arr.addObject();
            node.put("part", e.getKey().part());
            node.put("side", e.getKey().side());
            node.put("valueMm", e.getValue().stripTrailingZeros().toPlainString());
        }
        return arr.toString();
    }

    public Map<PartSide, BigDecimal> readBases(String json) {
        try {
            Map<PartSide, BigDecimal> map = new LinkedHashMap<>();
            for (JsonNode node : mapper.readTree(json)) {
                PartSide key = new PartSide(node.get("part").asText(), node.get("side").asText());
                map.put(key, new BigDecimal(node.get("valueMm").asText()));
            }
            return map;
        } catch (Exception e) {
            throw new BizException("基准尺寸数据已损坏，无法读取");
        }
    }

    public String writeRules(Map<PartSide, NormRule> rules) {
        ArrayNode arr = mapper.createArrayNode();
        for (NormRule rule : rules.values()) {
            ObjectNode node = arr.addObject();
            node.put("part", rule.part());
            node.put("side", rule.side());
            node.put("ruleType", rule.ruleType());
            if (rule.fixedDeltaMm() != null) {
                node.put("fixedDeltaMm", rule.fixedDeltaMm().stripTrailingZeros().toPlainString());
            }
            if (rule.ratioPercent() != null) {
                node.put("ratioPercent", rule.ratioPercent().stripTrailingZeros().toPlainString());
            }
            if (rule.segments() != null && !rule.segments().isEmpty()) {
                ArrayNode segs = node.putArray("segments");
                for (NormSegment seg : rule.segments()) {
                    ObjectNode s = segs.addObject();
                    s.put("fromLevel", seg.fromLevel());
                    s.put("toLevel", seg.toLevel());
                    s.put("deltaMm", seg.deltaMm().stripTrailingZeros().toPlainString());
                }
            }
        }
        return arr.toString();
    }

    public Map<PartSide, NormRule> readRules(String json) {
        try {
            Map<PartSide, NormRule> map = new LinkedHashMap<>();
            for (JsonNode node : mapper.readTree(json)) {
                String part = node.get("part").asText();
                String side = node.get("side").asText();
                String type = node.get("ruleType").asText();
                BigDecimal fixed = node.hasNonNull("fixedDeltaMm") ? new BigDecimal(node.get("fixedDeltaMm").asText()) : null;
                BigDecimal ratio = node.hasNonNull("ratioPercent") ? new BigDecimal(node.get("ratioPercent").asText()) : null;
                List<NormSegment> segments = new ArrayList<>();
                if (node.hasNonNull("segments")) {
                    for (JsonNode s : node.get("segments")) {
                        segments.add(new NormSegment(s.get("fromLevel").asInt(), s.get("toLevel").asInt(),
                                new BigDecimal(s.get("deltaMm").asText())));
                    }
                }
                map.put(new PartSide(part, side), new NormRule(part, side, type, fixed, ratio, List.copyOf(segments)));
            }
            return map;
        } catch (Exception e) {
            throw new BizException("放码规则数据已损坏，无法读取");
        }
    }

    /** 回显：把毫米规范值换算回方案录入单位（厘米值保留两位小数） */
    public List<com.garment.factory.dto.GradeBaseReq> toBaseReqs(Map<PartSide, BigDecimal> basesMm, String unit) {
        List<com.garment.factory.dto.GradeBaseReq> list = new ArrayList<>();
        basesMm.forEach((key, valueMm) -> list.add(new com.garment.factory.dto.GradeBaseReq(
                key.part(), key.side(),
                GradeDict.UNIT_CM.equals(unit)
                        ? valueMm.divide(BigDecimal.TEN, 2, java.math.RoundingMode.HALF_UP)
                        : valueMm)));
        return list;
    }

    /** 回显：规则参数换算回录入单位，厘米保留两位小数 */
    public List<com.garment.factory.dto.GradeRuleReq> toRuleReqs(Map<PartSide, NormRule> rules, String unit) {
        List<com.garment.factory.dto.GradeRuleReq> list = new ArrayList<>();
        rules.values().forEach(rule -> {
            com.garment.factory.dto.GradeRuleReq req = new com.garment.factory.dto.GradeRuleReq();
            req.setPart(rule.part());
            req.setSide(rule.side());
            req.setRuleType(rule.ruleType());
            boolean cm = GradeDict.UNIT_CM.equals(unit);
            if (rule.fixedDeltaMm() != null) {
                req.setFixedDelta(cm
                        ? rule.fixedDeltaMm().divide(BigDecimal.TEN, 2, java.math.RoundingMode.HALF_UP)
                        : rule.fixedDeltaMm());
            }
            req.setRatioPercent(rule.ratioPercent());
            if (!rule.segments().isEmpty()) {
                List<com.garment.factory.dto.GradeSegmentReq> segs = new ArrayList<>();
                for (NormSegment seg : rule.segments()) {
                    com.garment.factory.dto.GradeSegmentReq s = new com.garment.factory.dto.GradeSegmentReq();
                    s.setFromLevel(seg.fromLevel());
                    s.setToLevel(seg.toLevel());
                    s.setDelta(cm
                            ? seg.deltaMm().divide(BigDecimal.TEN, 2, java.math.RoundingMode.HALF_UP)
                            : seg.deltaMm());
                    segs.add(s);
                }
                req.setSegments(segs);
            }
            list.add(req);
        });
        return list;
    }
}
