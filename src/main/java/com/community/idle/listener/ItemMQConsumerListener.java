package com.community.idle.listener;

import com.community.idle.dto.ItemChangeMessage;
import com.community.idle.service.ItemElasticsearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(
        topic = "${rocketmq.topic.item-change}",
        consumerGroup = "${rocketmq.consumer.group}"
)
public class ItemMQConsumerListener implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(ItemMQConsumerListener.class);

    private final ItemElasticsearchService itemElasticsearchService;
    private final ObjectMapper objectMapper;

    public ItemMQConsumerListener(ItemElasticsearchService itemElasticsearchService, ObjectMapper objectMapper) {
        this.itemElasticsearchService = itemElasticsearchService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(String message) {
        try {
            ItemChangeMessage itemChangeMessage = objectMapper.readValue(message, ItemChangeMessage.class);
            log.info("收到物品变更消息, itemId: {}, operationType: {}",
                    itemChangeMessage.getItemId(), itemChangeMessage.getOperationType());

            if ("DELETE".equals(itemChangeMessage.getOperationType())) {
                itemElasticsearchService.deleteItem(itemChangeMessage.getItemId());
            } else {
                itemElasticsearchService.syncItemById(itemChangeMessage.getItemId());
            }

            log.info("物品ES索引同步成功, itemId: {}", itemChangeMessage.getItemId());
        } catch (Exception e) {
            log.error("物品ES索引同步失败, message: {}", message, e);
        }
    }
}
