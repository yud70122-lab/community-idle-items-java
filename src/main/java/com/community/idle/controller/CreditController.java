package com.community.idle.controller;

import com.community.idle.common.Result;
import com.community.idle.dto.CreditRepairDTO;
import com.community.idle.service.CreditService;
import com.community.idle.service.RepairService;
import com.community.idle.vo.CreditRepairResultVO;
import com.community.idle.vo.CreditTreeVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "信用管理")
@RestController
@RequestMapping("/credit")
public class CreditController {

    private final CreditService creditService;
    private final RepairService repairService;

    public CreditController(CreditService creditService, RepairService repairService) {
        this.creditService = creditService;
        this.repairService = repairService;
    }

    @ApiOperation("获取信用成长树")
    @GetMapping("/tree")
    public Result<CreditTreeVO> getCreditTree(@AuthenticationPrincipal Long userId) {
        CreditTreeVO tree = creditService.getCreditTree(userId);
        return Result.success("获取成功", tree);
    }

    @ApiOperation("提交信用修复申请")
    @PostMapping("/repair")
    public Result<CreditRepairResultVO> applyRepair(
            @AuthenticationPrincipal Long userId,
            @Validated @RequestBody CreditRepairDTO dto
    ) {
        CreditRepairResultVO result = repairService.applyRepair(userId, dto);
        return Result.success("修复申请已提交", result);
    }
}
