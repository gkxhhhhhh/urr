package com.urr.domain.market;

/**
 * 市场订单类型。
 */
public enum MarketOrderTypeEnum {

    /**
     * 卖单。
     */
    SELL("SELL", 1),

    /**
     * 买单。
     */
    BUY("BUY", 2);

    private final String code;

    private final int dbValue;

    MarketOrderTypeEnum(String code, int dbValue) {
        this.code = code;
        this.dbValue = dbValue;
    }

    /**
     * 按接口编码解析类型。
     *
     * @param code 接口编码
     * @return 枚举
     */
    public static MarketOrderTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (MarketOrderTypeEnum item : values()) {
            if (item.code.equalsIgnoreCase(code)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 按数据库值解析类型。
     *
     * @param dbValue 数据库值
     * @return 枚举
     */
    public static MarketOrderTypeEnum fromDbValue(Integer dbValue) {
        if (dbValue == null) {
            return null;
        }
        for (MarketOrderTypeEnum item : values()) {
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