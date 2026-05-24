package com.community.idle.constants;

public class ItemConstants {

    public static final Integer CONDITION_NEW = 1;
    public static final Integer CONDITION_ALMOST_NEW = 2;
    public static final Integer CONDITION_GOOD = 3;
    public static final Integer CONDITION_FAIR = 4;
    public static final Integer CONDITION_POOR = 5;

    public static final Integer STATUS_OFFLINE = 0;
    public static final Integer STATUS_ON_SALE = 1;
    public static final Integer STATUS_LOCKED = 2;
    public static final Integer STATUS_SOLD = 3;
    public static final Integer STATUS_REVIEW = 4;

    public static final Integer TRADE_TYPE_FREE = 1;
    public static final Integer TRADE_TYPE_EXCHANGE = 2;
    public static final Integer TRADE_TYPE_SELL = 3;

    public static final String LIKE_KEY_PREFIX = "item:like:";
    public static final String FAVORITE_KEY_PREFIX = "item:favorite:";
    public static final String VIEW_COUNT_KEY_PREFIX = "item:view:";

    public static String getConditionName(Integer condition) {
        if (condition == null) return "未知";
        switch (condition) {
            case 1: return "全新";
            case 2: return "几乎全新";
            case 3: return "良好";
            case 4: return "一般";
            case 5: return "较差";
            default: return "未知";
        }
    }

    public static String getStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "已下架";
            case 1: return "在售";
            case 2: return "已锁定";
            case 3: return "已售出";
            case 4: return "审核中";
            default: return "未知";
        }
    }

    public static String getTradeTypeName(Integer tradeType) {
        if (tradeType == null) return "未知";
        switch (tradeType) {
            case 1: return "免费送";
            case 2: return "以物换物";
            case 3: return "出售";
            default: return "未知";
        }
    }
}
