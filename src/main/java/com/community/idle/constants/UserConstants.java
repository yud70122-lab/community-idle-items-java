package com.community.idle.constants;

public class UserConstants {

    public static final Integer STATUS_NORMAL = 1;

    public static final Integer STATUS_DISABLED = 0;

    public static final Integer STATUS_CANCELING = 2;

    public static final Integer STATUS_CANCELED = 3;

    public static final String SMS_SCENE_CHANGE_OLD = "change_phone_old";

    public static final String SMS_SCENE_CHANGE_NEW = "change_phone_new";

    public static final String PHONE_CHANGE_LOCK_PREFIX = "lock:phone_change:";

    public static final String USER_CANCEL_LOCK_PREFIX = "lock:user_cancel:";

    public static final long PHONE_CHANGE_LOCK_EXPIRE = 300;

    public static final long USER_CANCEL_LOCK_EXPIRE = 300;

    public static final long CANCEL_PROCESS_DAYS = 7;
}
