package com.community.idle.controller;

import com.community.idle.common.Result;
import com.community.idle.dto.ReportSubmitDTO;
import com.community.idle.entity.Report;
import com.community.idle.service.ReportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "举报管理")
@RestController
@RequestMapping("/report")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @ApiOperation("获取举报原因列表")
    @GetMapping("/reasons")
    public Result<List<String>> getReportReasons() {
        return Result.success("获取成功", reportService.getReportReasons());
    }

    @ApiOperation("提交举报")
    @PostMapping("/submit")
    public Result<Map<String, Object>> submitReport(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @Validated @RequestBody ReportSubmitDTO dto
    ) {
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        Report report = reportService.submitReport(userId, dto);
        Map<String, Object> data = new HashMap<>();
        data.put("reportId", report.getId());
        data.put("status", report.getStatus());
        data.put("message", "举报提交成功，我们将尽快处理");
        return Result.success("提交成功", data);
    }
}
