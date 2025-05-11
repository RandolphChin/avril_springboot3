package com.randy.chin.service.impl;

import com.alibaba.excel.EasyExcel;
import com.aspose.cells.Cell;
import com.aspose.cells.CellValueType;
import com.aspose.cells.Cells;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
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
import com.randy.chin.util.CellsUtil;

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
                .headRowNumber(5)  // 指定表头行号
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
    @Transactional
    public boolean importDataWithAspose(MultipartFile file) throws Exception {
        try {
            // 清空数据
            wineStockMapper.delete(null);

            // 授权Aspose Cells
            CellsUtil.authrolizeLicense();

            // 加载Excel文件
            Workbook workbook = new Workbook(file.getInputStream());
            Worksheet worksheet = workbook.getWorksheets().get(0);
            Cells cells = worksheet.getCells();

            // 获取数据范围
            int rowCount = cells.getMaxDataRow() + 1;
            int colCount = cells.getMaxDataColumn() + 1;

            // 表头行号（从0开始计数，所以第5行是索引4）
            int headerRowIndex = 0;

            // 存储表头信息
            Map<Integer, String> headerMap = new HashMap<>();
            for (int col = 0; col < colCount; col++) {
                Cell cell = cells.get(headerRowIndex, col);
                if (cell != null && cell.getType() == CellValueType.IS_STRING) {
                    headerMap.put(col, cell.getStringValue().trim());
                }
            }
            log.info("解析到Excel表头信息: {}", headerMap);

            // 存储待保存的数据
            List<WineStock> list = new ArrayList<>();

            // 添加全局变量，用于存储当前的一级分类和二级分类
            String currentFirstLevelCategory = null;
            String currentSecondLevelCategory = null;

            // 从表头下一行开始读取数据
            for (int row = headerRowIndex + 1; row < rowCount; row++) {
                // 创建Excel模型对象
                WineStockExcelModel excelModel = new WineStockExcelModel();

                // 读取每一列的数据
                for (int col = 0; col < colCount; col++) {
                    Cell cell = cells.get(row, col);
                    String headerName = headerMap.get(col);

                    if (headerName == null || cell == null || cell.getType() == CellValueType.IS_NULL) {
                        continue;
                    }

                    String cellValue = "";
                    if (cell.getType() == CellValueType.IS_STRING) {
                        cellValue = cell.getStringValue().trim();
                    } else if (cell.getType() == CellValueType.IS_NUMERIC) {
                        cellValue = String.valueOf(cell.getDoubleValue());
                    } else {
                        cellValue = cell.getDisplayStringValue().trim();
                    }

                    // 根据表头设置对应的属性
                    switch (headerName) {
                        case "一级分类":
                            excelModel.setFirstLevelCategory(cellValue);
                            break;
                        case "二级分类":
                            excelModel.setSecondLevelCategory(cellValue);
                            break;
                        case "商品编号":
                            excelModel.setProductCode(cellValue);
                            break;
                        case "商品名称":
                            excelModel.setProductName(cellValue);
                            break;
                        case "英文名":
                            excelModel.setEnglishName(cellValue);
                            break;
                        case "规格":
                            excelModel.setSpecification(cellValue);
                            break;
                        case "可销售库存":
                            excelModel.setSellableInventory(cellValue);
                            break;
                        case "餐饮售价":
                            excelModel.setCateringPrice(cellValue);
                            break;
                        case "账面库存":
                            excelModel.setBookInventory(cellValue);
                            break;
                        default:
                            log.debug("未知表头: {}, 值: {}", headerName, cellValue);
                    }
                }

                // 如果一级分类为空，跳过该行
                if (!StringUtils.hasText(excelModel.getFirstLevelCategory())) {
                    log.warn("跳过空行数据，行号: {}", row);
                    continue;
                }

                // 当商品名称为空时，更新全局变量
                if (!StringUtils.hasText(excelModel.getProductName())) {
                    currentFirstLevelCategory = excelModel.getFirstLevelCategory();
                    currentSecondLevelCategory = excelModel.getSecondLevelCategory();
                    log.info("更新分类信息 - 一级分类: {}, 二级分类: {}", currentFirstLevelCategory, currentSecondLevelCategory);
                    continue;
                }

                // 将Excel模型转换为实体类
                WineStock wineStock = new WineStock();
                BeanUtils.copyProperties(excelModel, wineStock);

                // 设置一级分类和二级分类
                if (!StringUtils.hasText(wineStock.getFirstLevelCategory())) {
                    wineStock.setFirstLevelCategory(currentFirstLevelCategory);
                }
                if (!StringUtils.hasText(wineStock.getSecondLevelCategory())) {
                    wineStock.setSecondLevelCategory(currentSecondLevelCategory);
                }

                wineStock.setLineIndex(row);
                wineStock.setStatus("0");
                wineStock.setCreatedAt(LocalDateTime.now());
                wineStock.setUpdatedAt(LocalDateTime.now());
                list.add(wineStock);
            }

            // 保存数据
            if (!list.isEmpty()) {
                log.info("{}条商品库存数据，开始存储数据库！", list.size());
                this.saveBatch(list);
                log.info("存储数据库成功！");
            } else {
                log.warn("没有数据需要保存");
            }

            return true;
        } catch (Exception e) {
            log.error("使用Aspose Cells导入商品库存数据失败", e);
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
                    String productNameRemoveBlank = ws.getProductName().replaceAll(" ", "");
                    if(productNameRemoveBlank.equals(productName)){
                        wineInventoryService.lambdaUpdate().eq(WineInventory::getId, wi.getId())
                        .set(WineInventory::getQuantity, ws.getBookInventory()).set(WineInventory::getIzChanged, "10")
                        .set(WineInventory::getUpdatedAt, LocalDateTime.now())
                        .update();
                    } else {
                        // 杜鲁安玫瑰酒庄热夫雷香贝丹拾园红葡萄酒  2021
                        // 杜鲁安玫瑰酒庄热夫雷香贝丹拾园干红2021   
                        // 杜鲁安玫瑰酒庄热夫雷香贝丹拾园红葡萄酒2022
                        // wi.getChineseName() 中 替换里面最后一个为“干”为空字符串 
                       
                        String newChineseName = productName.replaceAll("干(?!.*干)", "").replaceAll("葡萄酒(?!.*葡萄洒)", ""); 
                        String newProductName = productNameRemoveBlank.replaceAll("干(?!.*干)", "").replaceAll("葡萄酒(?!.*葡萄洒)", "");
                        if (newChineseName.equals(newProductName)) {
                            wineInventoryService.lambdaUpdate().eq(WineInventory::getId, wi.getId())
                        .set(WineInventory::getQuantity, ws.getBookInventory()).set(WineInventory::getIzChanged, "10")
                        .set(WineInventory::getUpdatedAt, LocalDateTime.now())
                        .update();
                        }

                    }
                }
        }
    }
    private boolean isLastFourCharsYear(String str) {
        Boolean flag = false;
        try{
            flag = str!= null && str.matches(".*\\d{4}$") &&
            Integer.parseInt(str.substring(str.length() - 4)) >= 1900 &&
            Integer.parseInt(str.substring(str.length() - 4)) <= 2100;
        }catch(Exception e){
            System.out.println("eee");
        }
        return flag;
    }
}
