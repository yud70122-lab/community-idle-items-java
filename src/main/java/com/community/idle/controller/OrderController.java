package com.community.idle.controller;

import com.community.idle.common.Result;
import com.community.idle.service.TradeService;
import com.community.idle.vo.TradeCheckVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "订单交易管理")
@RestController
@RequestMapping("/order")
public class OrderController {

    private final TradeService tradeService;

    public OrderController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @ApiOperation("一键交换前置校验")
    @GetMapping("/check/exchange/{itemId}")
    public Result<TradeCheckVO> checkExchange(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiParam(value = "物品ID", required = true) @PathVariable("itemId") Long itemId
    ) {
        TradeCheckVO result = tradeService.checkExchangeEligibility(userId, itemId);
        return Result.success(result.getCanTrade() ? "校验通过" : "校验不通过", result);
    }

    @ApiOperation("立即购买前置校验")
    @GetMapping("/check/buy/{itemId}")
    public Result<TradeCheckVO> checkBuy(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiParam(value = "物品ID", required = true) @PathVariable("itemId") Long itemId
    ) {
        TradeCheckVO result = tradeService.checkBuyEligibility(userId, itemId);
        return Result.success(result.getCanTrade() ? "校验通过" : "校验不通过", result);
    }
}
