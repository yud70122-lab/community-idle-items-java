package com.community.idle.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.community.idle.constants.ItemConstants;
import com.community.idle.entity.Item;
import com.community.idle.entity.User;
import com.community.idle.mapper.ItemMapper;
import com.community.idle.mapper.UserMapper;
import com.community.idle.utils.SensitiveWordUtil;
import com.community.idle.utils.WxSubMsgUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;

@Slf4j
@Service
public class AsyncAuditService {

    private final ItemMapper itemMapper;
    private final UserMapper userMapper;
    private final WxSubMsgUtil wxSubMsgUtil;

    public AsyncAuditService(ItemMapper itemMapper, UserMapper userMapper, WxSubMsgUtil wxSubMsgUtil) {
        this.itemMapper = itemMapper;
        this.userMapper = userMapper;
        this.wxSubMsgUtil = wxSubMsgUtil;
    }

    @Async
    public void auditItemContent(Long itemId) {
        log.info("开始异步审核物品内容，itemId={}", itemId);

        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            log.warn("物品不存在，itemId={}", itemId);
            return;
        }

        SensitiveWordUtil.SensitiveWordCheckResult checkResult =
                SensitiveWordUtil.checkItemContent(item.getTitle(), item.getDescription());

        if (checkResult.isSafe()) {
            log.info("物品内容审核通过，itemId={}", itemId);
            return;
        }

        StringJoiner sj = new StringJoiner("、");
        checkResult.getFoundWords().forEach(sj::add);
        String sensitiveWords = sj.toString();
        log.warn("物品包含敏感词：{}，itemId={}", sensitiveWords, itemId);

        if (checkResult.isSevere()) {
            log.error("发现严重违规内容，自动下架物品，itemId={}", itemId);

            itemMapper.update(null, new LambdaUpdateWrapper<Item>()
                    .eq(Item::getId, itemId)
                    .set(Item::getStatus, ItemConstants.STATUS_OFFLINE)
                    .set(Item::getUpdateTime, LocalDateTime.now()));

            notifyUserItemOffShelf(item, sensitiveWords);

            User user = userMapper.selectById(item.getUserId());
            if (user != null && user.getOpenid() != null) {
                String reviewTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                wxSubMsgUtil.sendReviewResult(
                        user.getOpenid(),
                        item.getTitle(),
                        "审核未通过",
                        reviewTime,
                        "内容包含违规词：" + sensitiveWords
                );
            }
        } else {
            log.info("存在一般敏感词，标记为人工审核，itemId={}", itemId);
            itemMapper.update(null, new LambdaUpdateWrapper<Item>()
                    .eq(Item::getId, itemId)
                    .set(Item::getStatus, ItemConstants.STATUS_REVIEW)
                    .set(Item::getUpdateTime, LocalDateTime.now()));
        }
    }

    private void notifyUserItemOffShelf(Item item, String sensitiveWords) {
        User user = userMapper.selectById(item.getUserId());
        if (user == null) {
            log.warn("用户不存在，userId={}", item.getUserId());
            return;
        }

        log.info("发送物品下架通知给用户，userId={}, itemId={}", item.getUserId(), item.getId());

        if (user.getOpenid() != null) {
            String offShelfTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            String reason = "内容包含违规词：" + sensitiveWords;
            String remark = "如有疑问请联系客服申诉";

            wxSubMsgUtil.send(
                    "item_off_shelf",
                    user.getOpenid(),
                    null,
                    null,
                    java.util.Map.of(
                            "thing1", new WxSubMsgUtil.SubMsgData(item.getTitle()),
                            "date2", new WxSubMsgUtil.SubMsgData(offShelfTime),
                            "thing3", new WxSubMsgUtil.SubMsgData(reason),
                            "thing4", new WxSubMsgUtil.SubMsgData(remark)
                    )
            );
        }
    }
}
