package com.community.idle.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.community.idle.annotation.Sensitive;
import com.community.idle.annotation.Sensitive.SensitiveType;

import java.io.IOException;

public class SensitiveJsonSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private SensitiveType sensitiveType;
    private int keepFront;
    private int keepBack;
    private char maskChar;

    public SensitiveJsonSerializer() {
    }

    public SensitiveJsonSerializer(SensitiveType sensitiveType, int keepFront, int keepBack, char maskChar) {
        this.sensitiveType = sensitiveType;
        this.keepFront = keepFront;
        this.keepBack = keepBack;
        this.maskChar = maskChar;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null || value.isEmpty()) {
            gen.writeString(value);
            return;
        }
        gen.writeString(desensitize(value));
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        if (property == null) {
            return prov.findNullValueSerializer(null);
        }

        Sensitive sensitive = property.getAnnotation(Sensitive.class);
        if (sensitive == null) {
            sensitive = property.getContextAnnotation(Sensitive.class);
        }

        if (sensitive != null) {
            return new SensitiveJsonSerializer(
                    sensitive.type(),
                    sensitive.keepFront(),
                    sensitive.keepBack(),
                    sensitive.maskChar()
            );
        }

        return prov.findValueSerializer(property.getType(), property);
    }

    private String desensitize(String value) {
        switch (sensitiveType) {
            case PHONE:
                return desensitizePhone(value);
            case EMAIL:
                return desensitizeEmail(value);
            case ID_CARD:
                return desensitizeIdCard(value);
            case BANK_CARD:
                return desensitizeBankCard(value);
            case NAME:
                return desensitizeName(value);
            case ADDRESS:
                return desensitizeAddress(value);
            case CUSTOM:
                return desensitizeCustom(value);
            default:
                return desensitizePhone(value);
        }
    }

    private String desensitizePhone(String phone) {
        if (phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String desensitizeEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }
        String prefix = email.substring(0, 1);
        String suffix = email.substring(atIndex);
        return prefix + "****" + suffix;
    }

    private String desensitizeIdCard(String idCard) {
        if (idCard.length() < 10) {
            return idCard;
        }
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }

    private String desensitizeBankCard(String bankCard) {
        if (bankCard.length() < 8) {
            return bankCard;
        }
        return bankCard.substring(0, 4) + " **** **** " + bankCard.substring(bankCard.length() - 4);
    }

    private String desensitizeName(String name) {
        if (name.length() <= 1) {
            return name;
        }
        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(name.charAt(0));
        for (int i = 1; i < name.length() - 1; i++) {
            sb.append('*');
        }
        sb.append(name.charAt(name.length() - 1));
        return sb.toString();
    }

    private String desensitizeAddress(String address) {
        if (address.length() < 6) {
            return address;
        }
        return address.substring(0, 3) + "****" + address.substring(address.length() - 3);
    }

    private String desensitizeCustom(String value) {
        int length = value.length();
        if (length <= keepFront + keepBack) {
            return value;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(value, 0, keepFront);
        int maskLength = length - keepFront - keepBack;
        for (int i = 0; i < maskLength; i++) {
            sb.append(maskChar);
        }
        sb.append(value.substring(length - keepBack));
        return sb.toString();
    }
}
