package com.community.idle.controller;

import com.community.idle.common.Result;
import com.community.idle.dto.PhoneChangeDTO;
import com.community.idle.dto.PhoneChangeSendCodeDTO;
import com.community.idle.service.PhoneChangeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "手机号管理")
@RestController
@RequestMapping("/user/phone")
public class PhoneChangeController {

    private final PhoneChangeService phoneChangeService;

    public PhoneChangeController(PhoneChangeService phoneChangeService) {
        this.phoneChangeService = phoneChangeService;
    }

    @ApiOperation("发送换绑手机号验证码")
    @PostMapping("/send-code")
    public Result<Void> sendSmsCode(
            @AuthenticationPrincipal Long userId,
            @Validated @RequestBody PhoneChangeSendCodeDTO dto
    ) {
        phoneChangeService.sendSmsCode(userId, dto);
        return Result.success("验证码发送成功", null);
    }

    @ApiOperation("换绑手机号")
    @PostMapping("/change")
    public Result<Void> changePhone(
            @AuthenticationPrincipal Long userId,
            @Validated @RequestBody PhoneChangeDTO dto
    ) {
        phoneChangeService.changePhone(userId, dto);
        return Result.success("手机号换绑成功", null);
    }
}
