package com.randy.chin.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.randy.chin.entity.WineInventory;
import com.randy.chin.service.WineInventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 葡萄酒库存Excel导入监听器
 */
@Slf4j
public class WineInventoryExcelListener extends AnalysisEventListener<WineInventoryExcelModel> {

    /**
     * 批处理阈值
     */
    private static final int BATCH_COUNT = 80000;

    private List<WineInventory> list = new ArrayList<>();

    private WineInventoryService wineInventoryService;

    // 添加全局变量，用于存储当前的子葡萄酒类型和子酒庄
    private String currentSubWineType;
    private String currentSubWinery;

    public WineInventoryExcelListener(WineInventoryService wineInventoryService) {
        this.wineInventoryService = wineInventoryService;
        this.currentSubWineType = null;
        this.currentSubWinery = null;
    }

    // 添加表头处理方法，用于调试表头信息
    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        log.info("解析到Excel表头信息（原始）: {}", headMap);
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
    public void invoke(WineInventoryExcelModel excelModel, AnalysisContext analysisContext) {
        // 打印原始数据行信息，用于调试
        log.info("当前行号: {}", analysisContext.readRowHolder().getRowIndex());
        log.info("解析到一条葡萄酒库存数据:{}", excelModel);

        // 如果wineType为空，跳过该行
        if(!StringUtils.hasText(excelModel.getWineType())){
            log.warn("跳过空行数据");
            return;
        }

        if(!StringUtils.hasText(excelModel.getCuveeName())){
            // 当cuveeName为空时，更新全局变量
            currentSubWineType = excelModel.getWineType();
            currentSubWinery = excelModel.getWinery();
            log.info("更新子类型信息 - 子葡萄酒类型: {}, 子酒庄: {}", currentSubWineType, currentSubWinery);
            return;
        }

        // 将Excel模型转换为实体类
        WineInventory wineInventory = new WineInventory();
        BeanUtils.copyProperties(excelModel, wineInventory);

        // 设置子葡萄酒类型和子酒庄
        wineInventory.setSubWineType(currentSubWineType);
        wineInventory.setSubWinery(currentSubWinery);
        wineInventory.setLineIndex(analysisContext.readRowHolder().getRowIndex());
        log.info("设置子类型信息 - 子葡萄酒类型: {}, 子酒庄: {}", currentSubWineType, currentSubWinery);
        wineInventory.setStatus(1);
        wineInventory.setCreatedAt(LocalDateTime.now());
        wineInventory.setUpdatedAt(LocalDateTime.now());
        list.add(wineInventory);
        if (list.size() >= BATCH_COUNT) {
            saveData();
            list.clear();
        }
    }

    @Override
    @Transactional
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        saveData();
        log.info("所有葡萄酒库存数据解析完成！");
    }

    /**
     * 保存数据
     */
    private void saveData() {
        if (list.isEmpty()) {
            log.warn("没有数据需要保存");
            return;
        }
        // 把status为0的删除掉，把status为1的设置为0
       wineInventoryService.remove(Wrappers.<WineInventory>query().lambda().eq(WineInventory::getStatus, 0));
       wineInventoryService.lambdaUpdate().set(WineInventory::getStatus, 0).eq(WineInventory::getStatus,1).update();

        log.info("{}条葡萄酒库存数据，开始存储数据库！", list.size());
        wineInventoryService.saveBatch(list);
        log.info("存储数据库成功！");
    }
}
