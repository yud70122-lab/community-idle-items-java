package com.community.idle.controller;

import com.community.idle.common.Result;
import com.community.idle.service.FavoriteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Map;

@Api(tags = "收藏管理")
@RestController
@RequestMapping("/favorite")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @ApiOperation("添加/取消收藏")
    @PostMapping("/add")
    public Result<Map<String, Object>> addFavorite(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiParam("物品ID", required = true) @RequestParam Long itemId
    ) {
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        Map<String, Object> result = favoriteService.addFavorite(userId, itemId);
        return Result.success((String) result.get("message"), result);
    }
}
