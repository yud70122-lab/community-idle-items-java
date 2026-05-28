package com.community.idle.listener;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.community.idle.service.ItemElasticsearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.List;

@Component
public class CanalItemChangeListener implements CommandLineRunner, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(CanalItemChangeListener.class);

    private final ItemElasticsearchService itemElasticsearchService;

    @Value("${canal.host}")
    private String canalHost;

    @Value("${canal.port}")
    private int canalPort;

    @Value("${canal.destination}")
    private String destination;

    @Value("${canal.username}")
    private String username;

    @Value("${canal.password}")
    private String password;

    @Value("${canal.enabled:false}")
    private boolean canalEnabled;

    private CanalConnector connector;
    private volatile boolean running = false;

    public CanalItemChangeListener(ItemElasticsearchService itemElasticsearchService) {
        this.itemElasticsearchService = itemElasticsearchService;
    }

    @Override
    public void run(String... args) {
        if (!canalEnabled) {
            log.info("Canal监听未启用");
            return;
        }

        new Thread(() -> {
            running = true;
            connector = CanalConnectors.newSingleConnector(
                    new InetSocketAddress(canalHost, canalPort),
                    destination,
                    username,
                    password
            );

            int batchSize = 1000;
            try {
                connector.connect();
                connector.subscribe("community_idle.item");
                connector.rollback();

                log.info("Canal监听启动成功, 监听表: community_idle.item");

                while (running) {
                    Message message = connector.getWithoutAck(batchSize, 1000L, java.util.concurrent.TimeUnit.MILLISECONDS);
                    long batchId = message.getId();
                    int size = message.getEntries().size();

                    if (batchId == -1 || size == 0) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        try {
                            handleEntry(message.getEntries());
                            connector.ack(batchId);
                        } catch (Exception e) {
                            log.error("处理Canal消息失败, batchId: {}", batchId, e);
                            connector.rollback(batchId);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Canal监听异常", e);
            } finally {
                connector.disconnect();
            }
        }).start();
    }

    private void handleEntry(List<CanalEntry.Entry> entries) {
        for (CanalEntry.Entry entry : entries) {
            if (entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONBEGIN
                    || entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONEND) {
                continue;
            }

            CanalEntry.RowChange rowChange;
            try {
                rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            } catch (Exception e) {
                throw new RuntimeException("解析Canal消息失败, data: " + entry.getStoreValue(), e);
            }

            CanalEntry.EventType eventType = rowChange.getEventType();
            log.info("Canal监听到变更, table: {}, eventType: {}", entry.getHeader().getTableName(), eventType);

            for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                Long itemId = getItemId(rowData, eventType);
                if (itemId == null) {
                    continue;
                }

                try {
                    if (eventType == CanalEntry.EventType.DELETE) {
                        itemElasticsearchService.deleteItem(itemId);
                        log.info("Canal同步删除ES索引成功, itemId: {}", itemId);
                    } else {
                        itemElasticsearchService.syncItemById(itemId);
                        log.info("Canal同步更新ES索引成功, itemId: {}", itemId);
                    }
                } catch (Exception e) {
                    log.error("Canal同步ES索引失败, itemId: {}, eventType: {}", itemId, eventType, e);
                }
            }
        }
    }

    private Long getItemId(CanalEntry.RowData rowData, CanalEntry.EventType eventType) {
        List<CanalEntry.Column> columns = eventType == CanalEntry.EventType.DELETE
                ? rowData.getBeforeColumnsList()
                : rowData.getAfterColumnsList();

        for (CanalEntry.Column column : columns) {
            if ("id".equals(column.getName())) {
                return Long.parseLong(column.getValue());
            }
        }
        return null;
    }

    @Override
    public void destroy() {
        running = false;
        if (connector != null) {
            connector.disconnect();
        }
        log.info("Canal监听已停止");
    }
}
