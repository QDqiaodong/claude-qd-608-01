package com.garment.factory.service.grade;

/**
 * 部位 + 侧别的复合键。非对称部位（肩宽、袖长）的左右规则各自独立、互不合并。
 */
public record PartSide(String part, String side) {

    public String label() {
        return part + "#" + side;
    }
}
