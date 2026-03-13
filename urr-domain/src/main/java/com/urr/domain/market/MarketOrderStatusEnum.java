package com.urr.domain.market;

/**
 * 市场订单状态。
 */
public enum MarketOrderStatusEnum {

    /**
     * 挂单中。
     */
    LISTED("LISTED", 1),

    /**
     * 部分成交。
     */
    PARTIAL("PARTIAL", 2),

    /**
     * 已完成。
     */
    COMPLETED("COMPLETED", 3),

    /**
     * 已取消。
     */
    CANCELLED("CANCELLED", 4),

    /**
     * 已过期。
     */
    EXPIRED("EXPIRED", 5);

    private final String code;

    private final int dbValue;

    MarketOrderStatusEnum(String code, int dbValue) {
        this.code = code;
        this.dbValue = dbValue;
    }

    /**
     * 按数据库值解析。
     *
     * @param dbValue 数据库值
     * @return 枚举
     */
    public static MarketOrderStatusEnum fromDbValue(Integer dbValue) {
        if (dbValue == null) {
            return null;
        }
        for (MarketOrderStatusEnum item : values()) {
            if (item.dbValue == dbValue.intValue()) {
                return item;
            }
        }
        return null;
    }

    public String getCode() {
        return code;
    }

    public int getDbValue() {
        return dbValue;
    }
}