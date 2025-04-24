package com.randy.chin.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.randy.chin.entity.WineStock;
import com.randy.chin.service.WineStockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 商品库存Excel导入监听器
 */
@Slf4j
public class WineStockExcelListener extends AnalysisEventListener<WineStockExcelModel> {

    /**
     * 批处理阈值
     */
    private static final int BATCH_COUNT = 80000;

    private List<WineStock> list = new ArrayList<>();

    private WineStockService wineStockService;



    public WineStockExcelListener(WineStockService wineStockService) {
        this.wineStockService = wineStockService;
    }

    // 添加表头处理方法，用于调试表头信息
    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        log.info("解析到Excel表头信息: {}", headMap);
    }

    // 添加异常处理方法
    @Override
    public void onException(Exception exception, AnalysisContext context) {
        log.error("解析Excel异常", exception);
        if (exception instanceof ExcelDataConvertException) {
            ExcelDataConvertException excelException = (ExcelDataConvertException) exception;
            log.error("第{}行，第{}列解析异常，数据为:{}",
                    excelException.getRowIndex(),
                    excelException.getColumnIndex(),
                    excelException.getCellData());
        }
    }

    @Override
    public void invoke(WineStockExcelModel excelModel, AnalysisContext analysisContext) {
        // 打印原始数据行信息，用于调试
        log.info("当前行号: {}", analysisContext.readRowHolder().getRowIndex());
        log.info("解析到一条商品库存数据:{}", excelModel);

        // 如果firstLevelCategory为空，跳过该行
        if(!StringUtils.hasText(excelModel.getFirstLevelCategory())){
            log.warn("跳过空行数据");
            return;
        }
         // 如果productName为空，跳过该行
         if(!StringUtils.hasText(excelModel.getProductName())){
            log.warn("跳过空行数据");
            return;
         }


        // 将Excel模型转换为实体类
        WineStock wineStock = new WineStock();
        BeanUtils.copyProperties(excelModel, wineStock);
        wineStock.setLineIndex(analysisContext.readRowHolder().getRowIndex());
        wineStock.setStatus("0");
        wineStock.setCreatedAt(LocalDateTime.now());
        wineStock.setUpdatedAt(LocalDateTime.now());
        list.add(wineStock);
        if (list.size() >= BATCH_COUNT) {
            saveData();
            list.clear();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        saveData();
        log.info("所有商品库存数据解析完成！");
    }

    /**
     * 保存数据
     */
    private void saveData() {
        if (list.isEmpty()) {
            log.warn("没有数据需要保存");
            return;
        }
        log.info("{}条商品库存数据，开始存储数据库！", list.size());
        wineStockService.saveBatch(list);
        log.info("存储数据库成功！");
    }
}
