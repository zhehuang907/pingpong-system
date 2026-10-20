package com.pingpong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("star_player")
public class StarPlayer implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;

    private String country;

    private Integer gender;

    private String level;

    private String playStyle;

    private Integer ranking;

    private String achievements;

    private String avatar;

    private String bannerImage;

    private String intro;

    private String highlights;

    private Integer featured;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
