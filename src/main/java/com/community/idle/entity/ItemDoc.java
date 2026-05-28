package com.community.idle.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Document(indexName = "item_index", shards = 3, replicas = 1)
public class ItemDoc {

    @Id
    private Long id;

    @Field(type = FieldType.Long)
    private Long userId;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    @Field(type = FieldType.Long)
    private Long categoryId;

    @Field(type = FieldType.Keyword)
    private String categoryName;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Double)
    private BigDecimal originalPrice;

    @Field(type = FieldType.Keyword)
    private String coverImage;

    @Field(type = FieldType.Keyword)
    private String images;

    @Field(type = FieldType.Integer)
    private Integer condition;

    @Field(type = FieldType.Keyword)
    private String conditionName;

    @Field(type = FieldType.Integer)
    private Integer tradeType;

    @Field(type = FieldType.Keyword)
    private String tradeTypeName;

    @Field(type = FieldType.Integer)
    private Integer status;

    @Field(type = FieldType.Keyword)
    private String statusName;

    @Field(type = FieldType.Integer)
    private Integer viewCount;

    @Field(type = FieldType.Integer)
    private Integer likeCount;

    @Field(type = FieldType.Integer)
    private Integer favoriteCount;

    @GeoPointField
    private GeoPoint location;

    @Field(type = FieldType.Keyword)
    private String locationName;

    @Field(type = FieldType.Double)
    private BigDecimal latitude;

    @Field(type = FieldType.Double)
    private BigDecimal longitude;

    @Field(type = FieldType.Keyword)
    private String nickname;

    @Field(type = FieldType.Keyword)
    private String avatar;

    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Field(type = FieldType.Integer)
    private Integer deleted;

    @Field(type = FieldType.Double)
    private BigDecimal matchScore;
}
