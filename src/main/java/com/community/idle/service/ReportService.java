package com.community.idle.service;

import com.community.idle.constants.ReportConstants;
import com.community.idle.dto.ReportSubmitDTO;
import com.community.idle.entity.Item;
import com.community.idle.entity.Report;
import com.community.idle.entity.User;
import com.community.idle.exception.BusinessException;
import com.community.idle.mapper.ItemMapper;
import com.community.idle.mapper.ReportMapper;
import com.community.idle.mapper.UserMapper;
import com.community.idle.utils.WxSubMsgUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.StringJoiner;

@Slf4j
@Service
public class ReportService {

    private final ReportMapper reportMapper;
    private final ItemMapper itemMapper;
    private final UserMapper userMapper;
    private final WxSubMsgUtil wxSubMsgUtil;
    private final CreditService creditService;

    public ReportService(ReportMapper reportMapper, ItemMapper itemMapper, UserMapper userMapper,
                         WxSubMsgUtil wxSubMsgUtil, CreditService creditService) {
        this.reportMapper = reportMapper;
        this.itemMapper = itemMapper;
        this.userMapper = userMapper;
        this.wxSubMsgUtil = wxSubMsgUtil;
        this.creditService = creditService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Report submitReport(Long reporterId, ReportSubmitDTO dto) {
        User reporter = userMapper.selectById(reporterId);
        if (reporter == null) {
            throw new BusinessException("用户不存在");
        }

        if (dto.getReportType() == ReportConstants.TYPE_ITEM) {
            Item item = itemMapper.selectById(dto.getTargetId());
            if (item == null) {
                throw new BusinessException("举报的物品不存在");
            }
        }

        if (dto.getReportType() == ReportConstants.TYPE_USER) {
            User targetUser = userMapper.selectById(dto.getTargetId());
            if (targetUser == null) {
                throw new BusinessException("举报的用户不存在");
            }
        }

        Report report = new Report();
        report.setReporterId(reporterId);
        report.setReportType(dto.getReportType());
        report.setTargetId(dto.getTargetId());
        report.setReason(dto.getReason());
        report.setDescription(dto.getDescription());
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            report.setImages(String.join(",", dto.getImages()));
        }
        report.setStatus(ReportConstants.STATUS_PENDING);
        report.setCreateTime(LocalDateTime.now());
        report.setUpdateTime(LocalDateTime.now());

        reportMapper.insert(report);

        log.info("用户 {} 提交举报成功，举报ID: {}, 类型: {}, 目标: {}",
                reporterId, report.getId(), dto.getReportType(), dto.getTargetId());

        return report;
    }

    @Transactional(rollbackFor = Exception.class)
    public void processReport(Long reportId, boolean approved, String remark, Long auditorId) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException("举报记录不存在");
        }
        if (report.getStatus() != ReportConstants.STATUS_PENDING) {
            throw new BusinessException("该举报已处理");
        }

        int newStatus = approved ? ReportConstants.STATUS_PROCESSED : ReportConstants.STATUS_REJECTED;
        report.setStatus(newStatus);
        report.setAuditorId(auditorId);
        report.setAuditRemark(remark);
        report.setAuditTime(LocalDateTime.now());
        report.setUpdateTime(LocalDateTime.now());
        reportMapper.updateById(report);

        if (approved) {
            handleReportViolation(report);
        }

        notifyReporter(report, approved, remark);

        log.info("举报处理完成，reportId: {}, approved: {}, auditorId: {}", reportId, approved, auditorId);
    }

    private void handleReportViolation(Report report) {
        if (report.getReportType() == ReportConstants.TYPE_ITEM) {
            itemMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Item>()
                    .eq(Item::getId, report.getTargetId())
                    .set(Item::getStatus, com.community.idle.constants.ItemConstants.STATUS_OFFLINE)
                    .set(Item::getUpdateTime, LocalDateTime.now()));

            Item item = itemMapper.selectById(report.getTargetId());
            if (item != null) {
                creditService.recordViolation(item.getUserId(),
                        com.community.idle.constants.CreditConstants.VIOLATION_TYPE_REPORT,
                        "物品被举报违规：" + ReportConstants.getReasonName(report.getReason()));
            }
        } else if (report.getReportType() == ReportConstants.TYPE_USER) {
            creditService.recordViolation(report.getTargetId(),
                    com.community.idle.constants.CreditConstants.VIOLATION_TYPE_REPORT,
                    "用户被举报违规：" + ReportConstants.getReasonName(report.getReason()));
        }
    }

    private void notifyReporter(Report report, boolean approved, String remark) {
        User reporter = userMapper.selectById(report.getReporterId());
        if (reporter != null && reporter.getOpenid() != null) {
            String result = approved ? "已处理" : "已驳回";
            String time = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            String reason = ReportConstants.getReasonName(report.getReason());
            String details = remark != null ? remark : (approved ? "已对违规内容进行处理" : "经核实内容无违规");

            wxSubMsgUtil.sendReviewResult(reporter.getOpenid(), reason, result, time, details);
        }
    }

    public List<String> getReportReasons() {
        return List.of(
                "1-虚假信息",
                "2-违禁品",
                "3-诈骗行为",
                "4-不当内容",
                "5-侵权问题",
                "6-其他问题"
        );
    }
}
