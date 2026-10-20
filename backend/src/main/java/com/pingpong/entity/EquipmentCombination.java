package com.pingpong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("equipment_combination")
public class EquipmentCombination implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    private Long bladeId;

    private Long forehandRubberId;

    private Long backhandRubberId;

    private BigDecimal compatibilityScore;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
