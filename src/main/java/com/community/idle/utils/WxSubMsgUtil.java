package com.community.idle.utils;

import com.community.idle.config.WxSubMsgProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class WxSubMsgUtil {

    private static final String TOKEN_CACHE_KEY = "wx:access_token";

    private final WxSubMsgProperties properties;
    private final RedisUtil redisUtil;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public WxSubMsgUtil(WxSubMsgProperties properties, RedisUtil redisUtil, ObjectMapper objectMapper) {
        this.properties = properties;
        this.redisUtil = redisUtil;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    public String getAccessToken() {
        String cachedToken = redisUtil.getString(TOKEN_CACHE_KEY);
        if (cachedToken != null && !cachedToken.isEmpty()) {
            return cachedToken;
        }

        String url = properties.getTokenUrl() + "?grant_type=client_credential&appid="
                + properties.getAppid() + "&secret=" + properties.getSecret();

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode node = objectMapper.readTree(response.getBody());
                String accessToken = node.get("access_token").asText();
                long expiresIn = node.get("expires_in").asLong(7200);
                redisUtil.setString(TOKEN_CACHE_KEY, accessToken, expiresIn - 300, TimeUnit.SECONDS);
                return accessToken;
            }
        } catch (Exception e) {
            log.error("获取微信access_token失败", e);
        }
        return null;
    }

    public boolean sendTradeSuccess(String openid, String itemName, String buyerName,
                                    String amount, String tradeTime) {
        Map<String, SubMsgData> data = new HashMap<>();
        data.put("thing1", new SubMsgData(itemName));
        data.put("name2", new SubMsgData(buyerName));
        data.put("amount3", new SubMsgData(amount));
        data.put("date4", new SubMsgData(tradeTime));

        return send(properties.getTemplates().getTradeSuccess(), openid, data);
    }

    public boolean sendOrderReminder(String openid, String itemName, String sellerName,
                                     String orderNo, String remark) {
        Map<String, SubMsgData> data = new HashMap<>();
        data.put("thing1", new SubMsgData(itemName));
        data.put("name2", new SubMsgData(sellerName));
        data.put("character_string3", new SubMsgData(orderNo));
        data.put("thing4", new SubMsgData(remark));

        return send(properties.getTemplates().getOrderReminder(), openid, data);
    }

    public boolean sendReviewResult(String openid, String itemName, String result,
                                    String reviewTime, String reason) {
        Map<String, SubMsgData> data = new HashMap<>();
        data.put("thing1", new SubMsgData(itemName));
        data.put("phrase2", new SubMsgData(result));
        data.put("date3", new SubMsgData(reviewTime));
        data.put("thing4", new SubMsgData(reason));

        return send(properties.getTemplates().getReviewResult(), openid, data);
    }

    public boolean sendCancelNotice(String openid, String cancelTime, String reason,
                                    String remark) {
        Map<String, SubMsgData> data = new HashMap<>();
        data.put("date1", new SubMsgData(cancelTime));
        data.put("thing2", new SubMsgData(reason));
        data.put("thing3", new SubMsgData(remark));

        return send(properties.getTemplates().getCancelNotice(), openid, data);
    }

    public boolean send(String templateId, String openid, Map<String, SubMsgData> data) {
        return send(templateId, openid, null, null, data);
    }

    public boolean send(String templateId, String openid, String page, String miniprogramState,
                        Map<String, SubMsgData> data) {
        if (openid == null || openid.isEmpty()) {
            log.warn("发送订阅消息失败：openid为空");
            return false;
        }
        if (templateId == null || templateId.isEmpty()) {
            log.warn("发送订阅消息失败：模板ID为空");
            return false;
        }

        String accessToken = getAccessToken();
        if (accessToken == null) {
            log.error("发送订阅消息失败：无法获取access_token");
            return false;
        }

        String url = properties.getSendUrl() + "?access_token=" + accessToken;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("touser", openid);
        requestBody.put("template_id", templateId);
        if (page != null && !page.isEmpty()) {
            requestBody.put("page", page);
        }
        if (miniprogramState != null && !miniprogramState.isEmpty()) {
            requestBody.put("miniprogram_state", miniprogramState);
        }
        requestBody.put("lang", "zh_CN");
        requestBody.put("data", data);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode node = objectMapper.readTree(response.getBody());
                int errcode = node.get("errcode").asInt(-1);
                if (errcode == 0) {
                    log.info("订阅消息发送成功，openid={}, template={}", openid, templateId);
                    return true;
                } else {
                    String errmsg = node.get("errmsg").asText("");
                    log.error("订阅消息发送失败，errcode={}, errmsg={}, openid={}", errcode, errmsg, openid);
                    if (errcode == 40001 || errcode == 42001) {
                        redisUtil.del(TOKEN_CACHE_KEY);
                    }
                }
            }
        } catch (Exception e) {
            log.error("发送订阅消息异常，openid={}, template={}", openid, templateId, e);
        }
        return false;
    }

    @Data
    public static class SubMsgData implements Serializable {
        private static final long serialVersionUID = 1L;
        private String value;

        public SubMsgData(String value) {
            this.value = value;
        }
    }
}
