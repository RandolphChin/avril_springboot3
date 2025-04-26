package com.randy.chin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 葡萄酒库存实体类
 */
@Data
@TableName("wine_inventory")
public class WineInventory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 葡萄酒类型
     */
    private String wineType;

    /**
     * 酒庄
     */
    private String winery;

    /**
     * Cuvée名称
     */
    private String cuveeName;

    /**
     * 中文名称
     */
    private String chineseName;

    /**
     * 年份
     */
    private String vintage;

    /**
     * 产区
     */
    private String region;

    /**
     * 国家
     */
    private String country;

    /**
     * 规格(瓶装尺寸)
     */
    private String format;

    /**
     * 贸易价格
     */
    private String tradePrice;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 子葡萄酒类型
     */
    private String subWineType;

    /**
     * 子酒庄
     */
    private String subWinery;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    private Integer lineIndex; // 行排序
    // 是否发生变化
    private String izChanged; // 无变化默认0， 变动10

    // 零售价格
    private String retailPrice;
    // 经销商价格
    private String distributorPrice;
    // 成本价格
    private String costPrice;
}
