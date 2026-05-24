package com.community.idle.controller;

import com.community.idle.common.Result;
import com.community.idle.service.VisitorService;
import com.community.idle.vo.VisitorVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = "访客管理")
@RestController
@RequestMapping("/user")
public class VisitorController {

    private final VisitorService visitorService;

    public VisitorController(VisitorService visitorService) {
        this.visitorService = visitorService;
    }

    @ApiOperation("获取最近访客列表")
    @GetMapping("/visitors")
    public Result<List<VisitorVO>> getRecentVisitors(@AuthenticationPrincipal Long userId) {
        List<VisitorVO> visitors = visitorService.getRecentVisitors(userId);
        return Result.success("获取成功", visitors);
    }
}
