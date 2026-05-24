package com.community.idle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.idle.dto.ItemQueryDTO;
import com.community.idle.entity.Item;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface ItemMapper extends BaseMapper<Item> {

    List<Item> selectMyItems(@Param("userId") Long userId, @Param("status") Integer status,
                             @Param("offset") Long offset, @Param("limit") Long limit);

    long countMyItems(@Param("userId") Long userId, @Param("status") Integer status);

    Map<String, BigDecimal> selectPriceStatsByItemName(@Param("itemName") String itemName);

    int countSoldItemsByName(@Param("itemName") String itemName);

    Page<Item> selectItemList(Page<Item> page, @Param("query") ItemQueryDTO query);

    List<Item> selectSimilarItems(@Param("itemId") Long itemId,
                                  @Param("categoryId") Long categoryId,
                                  @Param("minPrice") java.math.BigDecimal minPrice,
                                  @Param("maxPrice") java.math.BigDecimal maxPrice,
                                  @Param("keyword") String keyword,
                                  @Param("limit") Integer limit);
}
