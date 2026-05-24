package com.community.idle.constants;

import java.util.Map;

public class ReportConstants {

    public static final Integer REASON_FAKE = 1;
    public static final Integer REASON_ILLEGAL = 2;
    public static final Integer REASON_FRAUD = 3;
    public static final Integer REASON_INAPPROPRIATE = 4;
    public static final Integer REASON_COPYRIGHT = 5;
    public static final Integer REASON_OTHER = 6;

    public static final Integer STATUS_PENDING = 0;
    public static final Integer STATUS_PROCESSED = 1;
    public static final Integer STATUS_REJECTED = 2;

    public static final Integer TYPE_ITEM = 1;
    public static final Integer TYPE_USER = 2;
    public static final Integer TYPE_COMMENT = 3;

    public static final Map<Integer, String> REASON_NAMES = Map.of(
            REASON_FAKE, "虚假信息",
            REASON_ILLEGAL, "违禁品",
            REASON_FRAUD, "诈骗行为",
            REASON_INAPPROPRIATE, "不当内容",
            REASON_COPYRIGHT, "侵权问题",
            REASON_OTHER, "其他问题"
    );

    public static String getReasonName(Integer reason) {
        if (reason == null) return "未知";
        return REASON_NAMES.getOrDefault(reason, "其他问题");
    }
}
