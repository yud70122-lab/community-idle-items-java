package com.community.idle.mq;

import com.community.idle.config.ItemSyncConfig;
import com.community.idle.dto.ItemChangeMessage;
import com.community.idle.service.ItemElasticsearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;

@Component
@ConditionalOnProperty(name = "item.sync.type", havingValue = "mq", matchIfMissing = true)
public class ItemMQConsumerService {

    private static final Logger log = LoggerFactory.getLogger(ItemMQConsumerService.class);

    private final ItemSyncConfig itemSyncConfig;
    private final ItemElasticsearchService itemElasticsearchService;
    private final ObjectMapper objectMapper;

    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Value("${rocketmq.consumer.group}")
    private String consumerGroup;

    @Value("${rocketmq.topic.item-change}")
    private String itemChangeTopic;

    private DefaultMQPushConsumer consumer;

    public ItemMQConsumerService(ItemSyncConfig itemSyncConfig,
                                  ItemElasticsearchService itemElasticsearchService,
                                  ObjectMapper objectMapper) {
        this.itemSyncConfig = itemSyncConfig;
        this.itemElasticsearchService = itemElasticsearchService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() throws Exception {
        if (!itemSyncConfig.isMqEnabled()) {
            log.info("MQ sync is not enabled, skipping consumer initialization");
            return;
        }

        log.info("Initializing Item MQ Consumer...");
        consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setNamesrvAddr(nameServer);
        consumer.subscribe(itemChangeTopic, "*");
        consumer.setConsumeMessageBatchMaxSize(10);
        consumer.setMaxReconsumeTimes(3);

        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            for (MessageExt msg : msgs) {
                try {
                    handleMessage(msg);
                } catch (Exception e) {
                    log.error("Error handling message, msgId: {}", msg.getMsgId(), e);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        consumer.start();
        log.info("Item MQ Consumer started successfully, topic: {}", itemChangeTopic);
    }

    private void handleMessage(MessageExt msg) throws Exception {
        String body = new String(msg.getBody());
        log.debug("Received item change message: {}", body);

        ItemChangeMessage message = objectMapper.readValue(body, ItemChangeMessage.class);

        if (message.getItemId() == null) {
            log.warn("Item id is null in message, skipping");
            return;
        }

        String operationType = message.getOperationType();
        Long itemId = message.getItemId();

        log.info("Processing MQ message - operation: {}, itemId: {}", operationType, itemId);

        if ("DELETE".equalsIgnoreCase(operationType)) {
            itemElasticsearchService.deleteItem(itemId);
        } else {
            itemElasticsearchService.syncItemById(itemId);
        }
    }

    @PreDestroy
    public void destroy() {
        if (consumer != null) {
            consumer.shutdown();
            log.info("Item MQ Consumer shutdown successfully");
        }
    }
}
