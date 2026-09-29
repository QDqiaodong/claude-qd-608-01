package com.garment.factory.dto;

/**
 * 异常关系定位：负数/零长度、相邻尺码倒挂、增量方向错误、肩宽胸围比例异常。
 * 每条都定位到具体尺码（level 给出级序，base 级为 0）、部位与侧别，并说明原因。
 *
 * code:
 *   NEGATIVE_OR_ZERO 负数或零长度
 *   INVERSION        相邻尺码倒挂
 *   DIRECTION        增量方向与尺码升降方向相反
 *   SHOULDER_RATIO   肩宽/胸围比例明显异常
 */
public class GradeIssue {

    private String code;
    private String sizeCode;
    private Integer level;
    private String part;
    private String side;
    private String message;

    public GradeIssue() {
    }

    public GradeIssue(String code, String sizeCode, Integer level, String part, String side, String message) {
        this.code = code;
        this.sizeCode = sizeCode;
        this.level = level;
        this.part = part;
        this.side = side;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSizeCode() {
        return sizeCode;
    }

    public void setSizeCode(String sizeCode) {
        this.sizeCode = sizeCode;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getPart() {
        return part;
    }

    public void setPart(String part) {
        this.part = part;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
