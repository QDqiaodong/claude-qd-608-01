package com.garment.factory.grading.engine;

/**
 * 放码结果异常类型（定位到「尺码 × 部位槽位」，并带原因说明）。
 */
public enum AnomalyType {

    /** 最终尺寸为负，制版无意义。 */
    NEGATIVE("尺寸为负"),

    /** 最终尺寸为零（零长度），纸样不可用。 */
    ZERO_LENGTH("零长度"),

    /** 相邻尺码倒挂：该增大的反而减小（或反之）。 */
    INVERSION("相邻尺码倒挂"),

    /** 增量方向错误：规则配置的单级变化量与该部位声明的放码方向冲突。 */
    INCREMENT_DIRECTION("增量方向错误"),

    /** 跨部位比例明显异常，如肩宽相对胸围过大/过小。 */
    RATIO_ABNORMAL("部位比例异常");

    private final String label;

    AnomalyType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
