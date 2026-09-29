package com.garment.factory.grading;

import java.util.ArrayList;
import java.util.List;

import com.garment.factory.grading.doc.SchemeDoc;

/**
 * 测试用方案构造工厂。
 */
final class TestSchemes {

    private TestSchemes() {
    }

    static SchemeDoc standard(String unit, String roundingUnit, String roundingStep) {
        SchemeDoc doc = new SchemeDoc();
        doc.setSchemaVersion(2);
        doc.setBaseGrade(0);
        doc.setInputUnit(unit);
        SchemeDoc.RoundingDoc r = new SchemeDoc.RoundingDoc();
        r.setUnit(roundingUnit);
        r.setStep(roundingStep);
        doc.setRounding(r);

        List<SchemeDoc.SizeDoc> sizes = new ArrayList<>();
        sizes.add(size(-2, "S"));
        sizes.add(size(-1, "M-"));
        sizes.add(size(0, "M"));
        sizes.add(size(1, "L"));
        sizes.add(size(2, "XL"));
        doc.setSizes(sizes);

        List<SchemeDoc.PartDoc> parts = new ArrayList<>();
        parts.add(part("bust", "胸围", null, unit.equals("mm") ? "900" : "90",
                unit.equals("mm") ? "40" : "4", "UP", unit));
        parts.add(part("waist", "腰围", null, unit.equals("mm") ? "720" : "72",
                unit.equals("mm") ? "30" : "3", "UP", unit));
        parts.add(part("bodyLen", "衣长", null, unit.equals("mm") ? "620" : "62",
                unit.equals("mm") ? "15" : "1.5", "UP", unit));
        doc.setParts(parts);
        return doc;
    }

    static SchemeDoc.SizeDoc size(int grade, String name) {
        SchemeDoc.SizeDoc s = new SchemeDoc.SizeDoc();
        s.setGrade(grade);
        s.setName(name);
        return s;
    }

    static SchemeDoc.PartDoc part(String id, String name, String side, String base,
                                  String delta, String direction, String unit) {
        SchemeDoc.PartDoc p = new SchemeDoc.PartDoc();
        p.setPartId(id);
        p.setPartName(name);
        p.setSide(side);
        p.setBase(base);
        p.setUnit(unit);
        p.setDirection(direction);
        SchemeDoc.RuleDoc rule = new SchemeDoc.RuleDoc();
        rule.setType("FIXED");
        rule.setDelta(delta);
        rule.setUnit(unit);
        p.setRule(rule);
        return p;
    }
}
