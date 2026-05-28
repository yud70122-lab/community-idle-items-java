package com.community.idle.canal;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.community.idle.config.CanalConfig;
import com.community.idle.service.ItemElasticsearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.List;

@Component
@ConditionalOnProperty(name = "item.sync.type", havingValue = "canal")
public class CanalItemSyncListener implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CanalItemSyncListener.class);

    private final CanalConfig canalConfig;
    private final ItemElasticsearchService itemElasticsearchService;

    public CanalItemSyncListener(CanalConfig canalConfig, ItemElasticsearchService itemElasticsearchService) {
        this.canalConfig = canalConfig;
        this.itemElasticsearchService = itemElasticsearchService;
    }

    @Override
    public void run(String... args) {
        new Thread(this::startListening, "canal-item-sync-listener").start();
    }

    private void startListening() {
        log.info("Starting Canal item sync listener...");
        CanalConnector connector = CanalConnectors.newSingleConnector(
                new InetSocketAddress(canalConfig.getHostname(), canalConfig.getPort()),
                canalConfig.getDestination(),
                canalConfig.getUsername(),
                canalConfig.getPassword()
        );

        try {
            connector.connect();
            connector.subscribe(canalConfig.getDatabase() + "\\." + canalConfig.getTable());
            connector.rollback();
            log.info("Canal connected successfully, listening to {}.{}",
                    canalConfig.getDatabase(), canalConfig.getTable());

            while (true) {
                Message message = connector.getWithoutAck(canalConfig.getBatchSize());
                long batchId = message.getId();
                int size = message.getEntries().size();

                if (batchId == -1 || size == 0) {
                    try {
                        Thread.sleep(canalConfig.getSleepMillis());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    try {
                        handleEntries(message.getEntries());
                        connector.ack(batchId);
                    } catch (Exception e) {
                        log.error("Error processing canal entries, batchId: {}", batchId, e);
                        connector.rollback(batchId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Canal listener error", e);
        } finally {
            connector.disconnect();
            log.info("Canal disconnected");
        }
    }

    private void handleEntries(List<CanalEntry.Entry> entries) {
        for (CanalEntry.Entry entry : entries) {
            if (entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONBEGIN
                    || entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONEND) {
                continue;
            }

            CanalEntry.RowChange rowChange;
            try {
                rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            } catch (Exception e) {
                log.error("Error parsing row change", e);
                continue;
            }

            CanalEntry.EventType eventType = rowChange.getEventType();

            for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                handleRowData(eventType, rowData);
            }
        }
    }

    private void handleRowData(CanalEntry.EventType eventType, CanalEntry.RowData rowData) {
        Long itemId = null;

        if (eventType == CanalEntry.EventType.DELETE) {
            itemId = getItemId(rowData.getBeforeColumnsList());
            if (itemId != null) {
                log.info("Canal received DELETE event for item id: {}", itemId);
                itemElasticsearchService.deleteItem(itemId);
            }
        } else if (eventType == CanalEntry.EventType.INSERT || eventType == CanalEntry.EventType.UPDATE) {
            itemId = getItemId(rowData.getAfterColumnsList());
            if (itemId != null) {
                log.info("Canal received {} event for item id: {}", eventType, itemId);
                itemElasticsearchService.syncItemById(itemId);
            }
        }
    }

    private Long getItemId(List<CanalEntry.Column> columns) {
        for (CanalEntry.Column column : columns) {
            if ("id".equals(column.getName())) {
                return Long.parseLong(column.getValue());
            }
        }
        return null;
    }
}
