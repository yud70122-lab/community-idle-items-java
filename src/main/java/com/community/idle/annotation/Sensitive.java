package com.community.idle.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.community.idle.serializer.SensitiveJsonSerializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = SensitiveJsonSerializer.class)
public @interface Sensitive {

    SensitiveType type() default SensitiveType.PHONE;

    enum SensitiveType {
        PHONE,
        EMAIL,
        ID_CARD,
        BANK_CARD,
        NAME,
        ADDRESS,
        CUSTOM
    }

    int keepFront() default 3;

    int keepBack() default 4;

    char maskChar() default '*';

    String mask() default "****";
}
