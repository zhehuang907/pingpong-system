package com.pingpong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("leaderboard_snapshot")
public class LeaderboardSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String boardType;

    private String boardName;

    private Long equipmentId;

    private Integer rankPosition;

    private BigDecimal totalScore;

    private BigDecimal hotScore;

    private BigDecimal valueScore;

    private BigDecimal timeDecay;

    private Integer favoriteCount;

    private Integer commentCount;

    private BigDecimal avgRating;

    private BigDecimal satisfactionRate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime latestInteractionTime;

    private String boardReason;

    private String refreshMode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime refreshedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private Equipment equipment;
}
