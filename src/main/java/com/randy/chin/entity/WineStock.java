package com.randy.chin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品库存实体类
 */
@Data
@TableName("wine_stocks")
public class WineStock implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 一级分类
     */
    private String firstLevelCategory;

    /**
     * 二级分类
     */
    private String secondLevelCategory;

    /**
     * 商品编号
     */
    private String productCode;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 英文名
     */
    private String englishName;

    /**
     * 规格
     */
    private String specification;

    /**
     * 可销售库存
     */
    private Integer sellableInventory;

    /**
     * 餐饮售价
     */
    private BigDecimal cateringPrice;

    /**
     * 状态
     */
    private String status;

    /**
     * 行排序
     */
    private Integer lineIndex;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}