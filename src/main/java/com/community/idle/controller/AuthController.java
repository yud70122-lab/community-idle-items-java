package com.community.idle.controller;

import com.community.idle.annotation.RateLimit;
import com.community.idle.common.Result;
import com.community.idle.dto.LoginDTO;
import com.community.idle.service.AuthService;
import com.community.idle.vo.LoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "认证管理")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @ApiOperation("用户登录")
    @RateLimit(type = RateLimit.RateLimitType.IP, limit = 5, period = 60, message = "登录请求过于频繁，请稍后再试")
    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO dto) {
        LoginVO loginVO = authService.login(dto);
        return Result.success("登录成功", loginVO);
    }
}
