package com.community.idle.constants;

public interface StoryConstants {

    String LIKE_KEY_PREFIX = "story:like:";

    Integer STATUS_NORMAL = 1;

    Integer STATUS_DELETED = 0;

    Integer CONDITION_NEW = 1;
    Integer CONDITION_ALMOST_NEW = 2;
    Integer CONDITION_GOOD = 3;
    Integer CONDITION_FAIR = 4;
    Integer CONDITION_POOR = 5;

    Integer ITEM_STATUS_OFFLINE = 0;
    Integer ITEM_STATUS_ON_SALE = 1;
    Integer ITEM_STATUS_LOCKED = 2;
    Integer ITEM_STATUS_SOLD = 3;
    Integer ITEM_STATUS_REVIEW = 4;
}
