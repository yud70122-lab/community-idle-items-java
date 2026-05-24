package com.community.idle.constants;

public class CreditConstants {

    public static final Integer CREDIT_LEVEL_1 = 1;
    public static final Integer CREDIT_LEVEL_2 = 2;
    public static final Integer CREDIT_LEVEL_3 = 3;
    public static final Integer CREDIT_LEVEL_4 = 4;
    public static final Integer CREDIT_LEVEL_5 = 5;

    public static final String CREDIT_LEVEL_NAME_1 = "信用新星";
    public static final String CREDIT_LEVEL_NAME_2 = "信用达人";
    public static final String CREDIT_LEVEL_NAME_3 = "信用精英";
    public static final String CREDIT_LEVEL_NAME_4 = "信用专家";
    public static final String CREDIT_LEVEL_NAME_5 = "信用大师";

    public static final Integer NODE_STATUS_LOCKED = 0;
    public static final Integer NODE_STATUS_UNLOCKED = 1;
    public static final Integer NODE_STATUS_CURRENT = 2;

    public static final Integer VIOLATION_STATUS_PENDING = 1;
    public static final Integer VIOLATION_STATUS_PROCESSED = 2;
    public static final Integer VIOLATION_STATUS_REPAIRED = 3;

    public static final Integer REPAIR_STATUS_PENDING = 1;
    public static final Integer REPAIR_STATUS_APPROVED = 2;
    public static final Integer REPAIR_STATUS_REJECTED = 3;

    public static final Integer REPAIR_STATUS_CANCELED = 4;

    public static final Integer VISIT_TYPE_ITEM = 1;
    public static final Integer VISIT_TYPE_USER = 2;

    public static final String VISITOR_REDIS_KEY = "visitor:";

    public static final String CREDIT_LOCK_PREFIX = "lock:credit:";

    public static final long CREDIT_LOCK_EXPIRE = 300;

    public static final String CREDIT_CACHE_PREFIX = "cache:credit:";

    public static final long CREDIT_CACHE_EXPIRE = 3600;
}
