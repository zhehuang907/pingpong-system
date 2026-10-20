package com.pingpong.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("equipment")
public class Equipment implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;

    private Long categoryId;

    private String brand;

    private String model;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private Integer stock;

    private String description;

    private String parameters;

    private String images;

    private String mainImage;

    private String imageUrl;

    private String videoUrl;

    private String suitableFor;

    private String features;

    private BigDecimal rating;

    private Integer ratingCount;

    private Integer viewCount;

    private Integer favoriteCount;

    private Integer salesCount;

    private String tags;

    private Integer status;

    private Integer isRecommend;

    @TableField(exist = false)
    private String recommendChannel;

    @TableField(exist = false)
    private String recommendReason;

    @TableField(exist = false)
    private List<String> recommendTags;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
