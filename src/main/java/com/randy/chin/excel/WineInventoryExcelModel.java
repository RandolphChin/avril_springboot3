package com.randy.chin.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 葡萄酒库存Excel模型
 */
@Data
public class WineInventoryExcelModel {

    @ExcelProperty("Wine Type") // 葡萄酒类型
    private String wineType;

    @ExcelProperty("Winery") // 酒庄
    private String winery;

    @ExcelProperty("Cuvée Name") // Cuvée名称
    private String cuveeName;

    @ExcelProperty("Chinese Name")
    private String chineseName;

    @ExcelProperty("Vintage") // 年份
    private String vintage;

    @ExcelProperty("Region") // 产区
    private String region;

    @ExcelProperty("Country") // 国家
    private String country;

    @ExcelProperty("Format") // 规格(瓶装尺寸)
    private String format;

    @ExcelProperty("Trade Price") // 贸易价格
    private String tradePrice;

    @ExcelProperty("Quantity") // 数量
    private String quantity;

    private Integer status;

    // 子葡萄酒类型
    private String subWineType;

    // 子酒庄
    private String subWinery;

    @ExcelProperty("Retail Price") // 零售价格
    private String retailPrice;

    // 经销商价格
    @ExcelProperty("Distributor  Price")
    private String distributorPrice;
    // 成本价格
    @ExcelProperty("Cost Price")
    private String costPrice;

    private String izChanged; // 无变化默认0， 变动10

    private String remark; // 备注
}
