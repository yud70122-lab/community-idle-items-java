package com.community.idle.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    String key() default "";

    int limit() default 10;

    int period() default 1;

    TimeUnit timeUnit() default TimeUnit.SECONDS;

    RateLimitType type() default RateLimitType.IP;

    String message() default "请求过于频繁，请稍后再试";

    enum RateLimitType {
        IP,
        USER_ID,
        IP_AND_USER_ID
    }
}
