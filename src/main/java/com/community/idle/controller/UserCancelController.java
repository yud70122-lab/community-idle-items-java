package com.community.idle.controller;

import com.community.idle.common.Result;
import com.community.idle.dto.UserCancelDTO;
import com.community.idle.service.UserCancelService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "账号注销管理")
@RestController
@RequestMapping("/user/cancel")
public class UserCancelController {

    private final UserCancelService userCancelService;

    public UserCancelController(UserCancelService userCancelService) {
        this.userCancelService = userCancelService;
    }

    @ApiOperation("提交注销申请")
    @PostMapping("/apply")
    public Result<Void> applyCancel(
            @AuthenticationPrincipal Long userId,
            @RequestBody(required = false) UserCancelDTO dto
    ) {
        userCancelService.applyCancel(userId, dto);
        return Result.success("注销申请已提交，7天内可取消", null);
    }

    @ApiOperation("取消注销申请")
    @PostMapping("/cancel")
    public Result<Void> cancelApply(
            @AuthenticationPrincipal Long userId
    ) {
        userCancelService.cancelApply(userId);
        return Result.success("注销申请已取消", null);
    }
}
