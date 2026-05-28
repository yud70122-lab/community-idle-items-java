package com.community.idle.service;

import com.community.idle.dto.ItemChangeMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ItemMQProducerService {

    private static final Logger log = LoggerFactory.getLogger(ItemMQProducerService.class);

    private final DefaultMQProducer producer;
    private final ObjectMapper objectMapper;

    @Value("${rocketmq.topic.item-change}")
    private String itemChangeTopic;

    public ItemMQProducerService(DefaultMQProducer producer, ObjectMapper objectMapper) {
        this.producer = producer;
        this.objectMapper = objectMapper;
    }

    public void sendItemChangeMessage(Long itemId, String operationType) {
        try {
            ItemChangeMessage message = ItemChangeMessage.builder()
                    .itemId(itemId)
                    .operationType(operationType)
                    .timestamp(System.currentTimeMillis())
                    .build();

            String json = objectMapper.writeValueAsString(message);
            Message msg = new Message(itemChangeTopic, json.getBytes());
            msg.setKeys(String.valueOf(itemId));

            SendResult sendResult = producer.send(msg);
            log.info("物品变更消息发送成功, itemId: {}, operationType: {}, msgId: {}",
                    itemId, operationType, sendResult.getMsgId());
        } catch (Exception e) {
            log.error("物品变更消息发送失败, itemId: {}, operationType: {}",
                    itemId, operationType, e);
        }
    }
}
