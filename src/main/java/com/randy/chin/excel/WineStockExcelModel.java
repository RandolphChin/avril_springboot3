package com.randy.chin.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品库存Excel模型
 */
@Data
public class WineStockExcelModel {

    @ExcelProperty("一级分类")
    private String firstLevelCategory;

    @ExcelProperty("二级分类")
    private String secondLevelCategory;

    @ExcelProperty("商品编号")
    private String productCode;

    @ExcelProperty("商品名称")
    private String productName;

    @ExcelProperty("英文名")
    private String englishName;

    @ExcelProperty("规格")
    private String specification;

    @ExcelProperty("可销售库存")
    private String sellableInventory;

    @ExcelProperty("餐饮售价")
    private String cateringPrice;

    private String status;

    private Integer lineIndex;

    @ExcelProperty("账面库存")
    private String bookInventory;
}