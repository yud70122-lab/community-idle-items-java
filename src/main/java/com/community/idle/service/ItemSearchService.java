package com.community.idle.service;

import com.community.idle.constants.ItemConstants;
import com.community.idle.dto.ItemQueryDTO;
import com.community.idle.entity.ItemDoc;
import com.community.idle.vo.ItemListVO;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.GeoDistanceSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ItemSearchService {

    private static final Logger log = LoggerFactory.getLogger(ItemSearchService.class);

    private final ElasticsearchRestTemplate elasticsearchRestTemplate;

    private static final String HIGHLIGHT_PRE_TAG = "<em>";
    private static final String HIGHLIGHT_POST_TAG = "</em>";

    public ItemSearchService(ElasticsearchRestTemplate elasticsearchRestTemplate) {
        this.elasticsearchRestTemplate = elasticsearchRestTemplate;
    }

    public Page<ItemListVO> searchItems(ItemQueryDTO query) {
        NativeSearchQuery searchQuery = buildSearchQuery(query);
        SearchHits<ItemDoc> searchHits = elasticsearchRestTemplate.search(searchQuery, ItemDoc.class);

        List<ItemListVO> voList = new ArrayList<>();
        for (SearchHit<ItemDoc> searchHit : searchHits) {
            ItemDoc doc = searchHit.getContent();
            ItemListVO vo = convertToVO(doc, searchHit.getHighlightFields());
            vo.setMatchScore(BigDecimal.valueOf(searchHit.getScore()));
            voList.add(vo);
        }

        long total = searchHits.getTotalHits();
        Pageable pageable = PageRequest.of(
                query.getPageNum() != null ? query.getPageNum().intValue() - 1 : 0,
                query.getPageSize() != null ? query.getPageSize().intValue() : 20
        );

        return new org.springframework.data.domain.PageImpl<>(voList, pageable, total);
    }

    private NativeSearchQuery buildSearchQuery(ItemQueryDTO query) {
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        boolQuery.must(QueryBuilders.termQuery("status", ItemConstants.STATUS_ON_SALE));
        boolQuery.must(QueryBuilders.termQuery("deleted", 0));

        if (query.getCategoryId() != null) {
            boolQuery.must(QueryBuilders.termQuery("categoryId", query.getCategoryId()));
        }

        if (StringUtils.hasText(query.getKeyword())) {
            BoolQueryBuilder keywordQuery = QueryBuilders.boolQuery();
            keywordQuery.should(QueryBuilders.multiMatchQuery(query.getKeyword())
                    .field("title", 5.0f)
                    .field("title.pinyin", 3.0f)
                    .field("description", 2.0f)
                    .field("description.pinyin", 1.0f)
                    .field("all_text", 1.5f)
                    .type(org.elasticsearch.index.query.MultiMatchQuery.Type.MOST_FIELDS)
                    .tieBreaker(0.3f));
            keywordQuery.minimumShouldMatch(1);
            boolQuery.must(keywordQuery);
        }

        if (!CollectionUtils.isEmpty(query.getConditions())) {
            boolQuery.must(QueryBuilders.termsQuery("condition", query.getConditions()));
        }

        if (query.getTradeType() != null) {
            boolQuery.must(QueryBuilders.termQuery("tradeType", query.getTradeType()));
        }

        if (query.getMinPrice() != null) {
            boolQuery.must(QueryBuilders.rangeQuery("price").gte(query.getMinPrice().doubleValue()));
        }

        if (query.getMaxPrice() != null) {
            boolQuery.must(QueryBuilders.rangeQuery("price").lte(query.getMaxPrice().doubleValue()));
        }

        if (query.getUserLatitude() != null && query.getUserLongitude() != null && query.getDistance() != null) {
            boolQuery.must(QueryBuilders.geoDistanceQuery("location")
                    .point(query.getUserLatitude().doubleValue(), query.getUserLongitude().doubleValue())
                    .distance(query.getDistance().doubleValue(), DistanceUnit.KILOMETERS));
        }

        FunctionScoreQueryBuilder functionScoreQuery = QueryBuilders.functionScoreQuery(boolQuery);
        functionScoreQuery.scoreMode(org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders.ScoreFunctionMode.SUM);
        functionScoreQuery.setMinScore(0.01f);

        if (StringUtils.hasText(query.getKeyword())) {
            FunctionScoreQueryBuilder.FilterFunctionBuilder[] functions = new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                    new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                            ScoreFunctionBuilders.scriptFunction(
                                    "doc['viewCount'].value * 0.001 + doc['likeCount'].value * 0.01 + doc['favoriteCount'].value * 0.02"
                            )
                    )
            };
            functionScoreQuery.add(functions);
        }

        functionScoreQuery.boostMode(CombineFunction.SUM);
        queryBuilder.withQuery(functionScoreQuery);

        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title").fragmentSize(50).numberOfFragments(1);
        highlightBuilder.field("description").fragmentSize(150).numberOfFragments(1);
        highlightBuilder.field("all_text").fragmentSize(200).numberOfFragments(1);
        highlightBuilder.preTags(HIGHLIGHT_PRE_TAG);
        highlightBuilder.postTags(HIGHLIGHT_POST_TAG);
        highlightBuilder.requireFieldMatch(false);
        highlightBuilder.highlightQuery(QueryBuilders.multiMatchQuery(query.getKeyword() != null ? query.getKeyword() : "")
                .field("title").field("description").field("all_text"));
        queryBuilder.withHighlightBuilder(highlightBuilder);

        String sortBy = query.getSortBy() != null ? query.getSortBy() : "match";
        switch (sortBy) {
            case "distance":
                if (query.getUserLatitude() != null && query.getUserLongitude() != null) {
                    GeoDistanceSortBuilder distanceSort = SortBuilders.geoDistanceSort("location",
                            query.getUserLatitude().doubleValue(), query.getUserLongitude().doubleValue());
                    distanceSort.unit(DistanceUnit.KILOMETERS);
                    distanceSort.order(SortOrder.ASC);
                    queryBuilder.withSort(distanceSort);
                }
                break;
            case "time":
                queryBuilder.withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC));
                break;
            case "popularity":
                queryBuilder.withSort(SortBuilders.fieldSort("viewCount").order(SortOrder.DESC));
                queryBuilder.withSort(SortBuilders.fieldSort("likeCount").order(SortOrder.DESC));
                break;
            case "match":
            default:
                queryBuilder.withSort(SortBuilders.scoreSort().order(SortOrder.DESC));
                queryBuilder.withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC));
                break;
        }

        int pageNum = query.getPageNum() != null ? query.getPageNum().intValue() - 1 : 0;
        int pageSize = query.getPageSize() != null ? query.getPageSize().intValue() : 20;
        queryBuilder.withPageable(PageRequest.of(pageNum, pageSize));

        return queryBuilder.build();
    }

    private ItemListVO convertToVO(ItemDoc doc, Map<String, List<String>> highlightFields) {
        ItemListVO.ItemListVOBuilder builder = ItemListVO.builder()
                .id(doc.getId())
                .userId(doc.getUserId())
                .title(doc.getTitle())
                .coverImage(doc.getCoverImage())
                .price(doc.getPrice())
                .originalPrice(doc.getOriginalPrice())
                .status(doc.getStatus())
                .statusName(ItemConstants.getStatusName(doc.getStatus()))
                .condition(doc.getCondition())
                .conditionName(ItemConstants.getConditionName(doc.getCondition()))
                .viewCount(doc.getViewCount())
                .likeCount(doc.getLikeCount())
                .favoriteCount(doc.getFavoriteCount())
                .location(doc.getLocationName())
                .nickname(doc.getNickname())
                .avatar(doc.getAvatar())
                .createTime(doc.getCreateTime());

        if (doc.getLocation() != null) {
            builder.distance(BigDecimal.valueOf(calculateDistance(doc.getLocation())));
        }

        if (highlightFields != null && !highlightFields.isEmpty()) {
            if (highlightFields.containsKey("title")) {
                String highlightTitle = String.join("", highlightFields.get("title"));
                builder.highlightTitle(highlightTitle);
            }
            if (highlightFields.containsKey("description")) {
                String highlightDescription = String.join("", highlightFields.get("description"));
                builder.highlightDescription(highlightDescription);
            }
        }

        return builder.build();
    }

    private double calculateDistance(GeoPoint location) {
        return 0.0;
    }

    public String getEsQueryDSL(ItemQueryDTO query) {
        NativeSearchQuery searchQuery = buildSearchQuery(query);
        return searchQuery.getQuery().toString();
    }
}
