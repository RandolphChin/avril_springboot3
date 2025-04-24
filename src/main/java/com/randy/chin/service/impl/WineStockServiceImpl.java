package com.randy.chin.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.randy.chin.entity.WineInventory;
import com.randy.chin.entity.WineStock;
import com.randy.chin.excel.WineStockExcelListener;
import com.randy.chin.excel.WineStockExcelModel;
import com.randy.chin.mapper.WineStockMapper;
import com.randy.chin.service.WineInventoryService;
import com.randy.chin.service.WineStockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品库存服务实现类
 */
@Slf4j
@Service
public class WineStockServiceImpl extends ServiceImpl<WineStockMapper, WineStock> implements WineStockService {

    private final WineStockMapper wineStockMapper;
    @Autowired
    private WineInventoryService wineInventoryService;

    // 添加全局变量，用于存储当前的一级分类和二级分类
    private String currentFirstLevelCategory;
    private String currentSecondLevelCategory;

    public WineStockServiceImpl(WineStockMapper wineStockMapper) {
        this.wineStockMapper = wineStockMapper;
    }

    @Override
    public boolean importData(MultipartFile file) throws IOException {
        try {
            // 清空数据
            wineStockMapper.delete(null);
            // 添加更多配置选项
            EasyExcel.read(file.getInputStream(),
                    WineStockExcelModel.class,
                    new WineStockExcelListener(this))
                .headRowNumber(2)  // 指定表头行号
                .ignoreEmptyRow(true)  // 忽略空行
                .autoTrim(true)  // 自动去除空格
                .sheet()
                .doRead();
            return true;
        } catch (Exception e) {
            log.error("导入商品库存数据失败", e);
            throw e;
        }
    }

    @Override
    public void exportData(HttpServletResponse response) throws Exception {
        List<WineStock> dataList = wineStockMapper.selectList(Wrappers.<WineStock>query().lambda().eq(WineStock::getStatus, "1").orderByAsc(WineStock::getLineIndex));
        List<WineStockExcelModel> renderList = this.buildRenderList(dataList);

        // 设置响应头
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=WineStocks.xls");

        // 使用EasyExcel直接写入响应流
        EasyExcel.write(response.getOutputStream(), WineStockExcelModel.class)
            .sheet("商品库存")
            .doWrite(renderList);
    }

    private List<WineStockExcelModel> buildRenderList(List<WineStock> dataList) {
        List<WineStockExcelModel> renderList = new ArrayList<>();
        currentFirstLevelCategory = null;
        currentSecondLevelCategory = null;

        for (WineStock data : dataList) {
            String firstLevelCategory = data.getFirstLevelCategory();
            String secondLevelCategory = data.getSecondLevelCategory();
            WineStockExcelModel excelModel = new WineStockExcelModel();

            if (!StringUtils.hasText(currentFirstLevelCategory)) {
                WineStockExcelModel initModel = new WineStockExcelModel();
                currentFirstLevelCategory = firstLevelCategory;
                currentSecondLevelCategory = secondLevelCategory;
                initModel.setFirstLevelCategory(firstLevelCategory);
                initModel.setSecondLevelCategory(secondLevelCategory);
                renderList.add(initModel);
            }

            if (currentFirstLevelCategory.equals(data.getFirstLevelCategory()) && currentSecondLevelCategory.equals(data.getSecondLevelCategory())) {
                if ((StringUtils.hasText(data.getProductName()) && renderList.size() > 0 && StringUtils.hasText(renderList.get(renderList.size() - 1).getProductName()))
                    && (!(data.getFirstLevelCategory().equals(renderList.get(renderList.size() - 1).getFirstLevelCategory()))
                    || !(data.getSecondLevelCategory().equals(renderList.get(renderList.size() - 1).getSecondLevelCategory())))) {
                    // 一级分类相同、二级分类相同、(一级分类不相同 or 二级分类不同)
                    WineStockExcelModel blankLineModel = new WineStockExcelModel();
                    renderList.add(blankLineModel);
                }
                BeanUtils.copyProperties(data, excelModel);
                renderList.add(excelModel);
            } else {
                currentFirstLevelCategory = data.getFirstLevelCategory();
                currentSecondLevelCategory = data.getSecondLevelCategory();
                WineStockExcelModel initModel = new WineStockExcelModel();
                initModel.setFirstLevelCategory(firstLevelCategory);
                initModel.setSecondLevelCategory(secondLevelCategory);
                renderList.add(initModel);
                BeanUtils.copyProperties(data, excelModel);
                renderList.add(excelModel);
            }
        }

        return renderList;
    }

    @Override
    public List<WineStock> listAll(String firstLevelCategory, String secondLevelCategory, String productName) {
        LambdaQueryWrapper<WineStock> queryWrapper = new LambdaQueryWrapper<>();

        // 添加状态条件，只查询有效数据
        queryWrapper.eq(WineStock::getStatus, "0");

        // 添加排序条件
        queryWrapper.orderByAsc(WineStock::getLineIndex);

        // 添加查询条件
        if (StringUtils.hasText(firstLevelCategory)) {
            queryWrapper.like(WineStock::getFirstLevelCategory, firstLevelCategory);
        }

        if (StringUtils.hasText(secondLevelCategory)) {
            queryWrapper.like(WineStock::getSecondLevelCategory, secondLevelCategory);
        }

        if (StringUtils.hasText(productName)) {
            queryWrapper.like(WineStock::getProductName, productName);
        }

        return this.list(queryWrapper);
    }
    // 根据库存更新报表
    @Transactional
    public void reNewWineInventory(){
        List<WineStock> stocklist = wineStockMapper.selectList(Wrappers.<WineStock>query().lambda().eq(WineStock::getStatus, "0"));
        List<WineInventory> wineInventories = wineInventoryService.lambdaQuery().eq(WineInventory::getStatus, "1").list();

        for(WineInventory wi : wineInventories){
            // wineInventory的chineseName 拼接上Vintage，得到的结果和stocklist的productName进行匹配，productName需要去除所有的空格
            String productName = wi.getChineseName() + " " + wi.getVintage();
            productName = productName.replaceAll(" ", "");
                for(WineStock ws: stocklist){
                    if(ws.getProductName().replaceAll(" ", "").equals(productName)){
                        wineInventoryService.lambdaUpdate().eq(WineInventory::getId, wi.getId())
                        .set(WineInventory::getQuantity, ws.getSellableInventory()).set(WineInventory::getIzChanged, "10")
                        .set(WineInventory::getUpdatedAt, LocalDateTime.now())
                        .update();
                    }
                }
        }
    }
}
