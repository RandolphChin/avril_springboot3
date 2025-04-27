package com.randy.chin.service.impl;

import com.alibaba.excel.EasyExcel;
import com.aspose.cells.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.randy.chin.entity.WineInventory;
import com.randy.chin.excel.WineInventoryExcelListener;
import com.randy.chin.excel.WineInventoryExcelModel;
import com.randy.chin.mapper.WineInventoryMapper;
import com.randy.chin.service.WineInventoryService;
import com.randy.chin.util.CellsUtil;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.aspose.cells.CellValueType.IS_NULL;
import static com.aspose.cells.CellValueType.IS_STRING;

/**
 * 葡萄酒库存服务实现类
 */
@Slf4j
@Service
public class WineInventoryServiceImpl extends ServiceImpl<WineInventoryMapper, WineInventory> implements WineInventoryService {

    private final WineInventoryMapper wineInventoryMapper;

    @Autowired
    private Environment environment;

    public WineInventoryServiceImpl(WineInventoryMapper wineInventoryMapper) {
        this.wineInventoryMapper = wineInventoryMapper;
    }
  // 添加全局变量，用于存储当前的子葡萄酒类型和子酒庄
  private String currentSubWineType;
  private String currentSubWinery;

    @Override
    @Transactional
    public boolean importData(MultipartFile file) throws IOException {
        try {
            // 添加更多配置选项
            EasyExcel.read(file.getInputStream(),
                    WineInventoryExcelModel.class,
                    new WineInventoryExcelListener(this))
                .headRowNumber(2)  // 指定表头行号
                .ignoreEmptyRow(true)  // 忽略空行
                .autoTrim(true)  // 自动去除空格
                .sheet()
                .doRead();
            return true;
        } catch (Exception e) {
            log.error("导入葡萄酒库存数据失败", e);
            throw e;
        }
    }

    /**
     * 使用Aspose Cells导入Excel数据
     * 解决EasyExcel无法识别某些标题的问题
     */
    @Override
    @Transactional
    public boolean importDataWithAspose(MultipartFile file) throws Exception {
        try {
            // 授权Aspose Cells
            CellsUtil.authrolizeLicense();
            
            // 加载Excel文件
            Workbook workbook = new Workbook(file.getInputStream());
            Worksheet worksheet = workbook.getWorksheets().get(0);
            Cells cells = worksheet.getCells();
            
            // 获取数据范围
            int rowCount = cells.getMaxDataRow() + 1;
            int colCount = cells.getMaxDataColumn() + 1;
            
            // 表头行号（从0开始计数，所以第2行是索引1）
            int headerRowIndex = 1;
            
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
            List<WineInventory> list = new ArrayList<>();
            
            // 添加全局变量，用于存储当前的子葡萄酒类型和子酒庄
            String currentSubWineType = null;
            String currentSubWinery = null;
            
            // 从表头下一行开始读取数据
            for (int row = headerRowIndex + 1; row < rowCount; row++) {
                // 创建Excel模型对象
                WineInventoryExcelModel excelModel = new WineInventoryExcelModel();
                
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
                        case "Wine Type":
                            excelModel.setWineType(cellValue);
                            break;
                        case "Winery":
                            excelModel.setWinery(cellValue);
                            break;
                        case "Cuvée Name":
                            excelModel.setCuveeName(cellValue);
                            break;
                        case "Chinese Name":
                            excelModel.setChineseName(cellValue);
                            break;
                        case "Vintage":
                            excelModel.setVintage(cellValue);
                            break;
                        case "Region":
                            excelModel.setRegion(cellValue);
                            break;
                        case "Country":
                            excelModel.setCountry(cellValue);
                            break;
                        case "Format":
                            excelModel.setFormat(cellValue);
                            break;
                        case "Trade Price":
                            excelModel.setTradePrice(cellValue);
                            break;
                        case "Quantity":
                            excelModel.setQuantity(cellValue);
                            break;
                        case "Retail Price":
                            excelModel.setRetailPrice(cellValue);
                            break;
                        case "Distributor Price":
                        case "Distributor  Price":
                            excelModel.setDistributorPrice(cellValue);
                            break;
                        case "Cost Price":
                            excelModel.setCostPrice(cellValue);
                            break;
                        case "Remark":
                            excelModel.setRemark(cellValue);
                            break;
                        default:
                            log.debug("未知表头: {}, 值: {}", headerName, cellValue);
                    }
                }
                
                // 如果wineType为空，跳过该行
                if (!StringUtils.hasText(excelModel.getWineType())) {
                    log.warn("跳过空行数据，行号: {}", row);
                    continue;
                }
                
                // 当cuveeName为空时，更新全局变量
                if (!StringUtils.hasText(excelModel.getCuveeName())) {
                    currentSubWineType = excelModel.getWineType();
                    currentSubWinery = excelModel.getWinery();
                    log.info("更新子类型信息 - 子葡萄酒类型: {}, 子酒庄: {}", currentSubWineType, currentSubWinery);
                    continue;
                }
                
                // 将Excel模型转换为实体类
                WineInventory wineInventory = new WineInventory();
                BeanUtils.copyProperties(excelModel, wineInventory);
                
                // 设置子葡萄酒类型和子酒庄
                wineInventory.setSubWineType(currentSubWineType);
                wineInventory.setSubWinery(currentSubWinery);
                wineInventory.setLineIndex(row);
                log.info("设置子类型信息 - 子葡萄酒类型: {}, 子酒庄: {}", currentSubWineType, currentSubWinery);
                wineInventory.setStatus(1);
                wineInventory.setCreatedAt(LocalDateTime.now());
                wineInventory.setUpdatedAt(LocalDateTime.now());
                wineInventory.setVintage(removeTrailingZeros(wineInventory.getVintage()));
                list.add(wineInventory);
            }
            
            // 保存数据
            if (!list.isEmpty()) {
                // 把status为0的删除掉，把status为1的设置为0
                this.remove(Wrappers.<WineInventory>query().lambda().eq(WineInventory::getStatus, 0));
                this.lambdaUpdate().set(WineInventory::getStatus, 0).eq(WineInventory::getStatus, 1).update();
                
                log.info("{}条葡萄酒库存数据，开始存储数据库！", list.size());
                this.saveBatch(list);
                log.info("存储数据库成功！");
            } else {
                log.warn("没有数据需要保存");
            }
            
            return true;
        } catch (Exception e) {
            log.error("使用Aspose Cells导入葡萄酒库存数据失败", e);
            throw e;
        }
    }

    /**
     * DataList 数据库的数据集合
        RenderList 渲染的数据集合

        DataList循环，取每一行的 subWineType 和 subWinery

        判断DataList当前行的 subWineType 和 subWinery 和全局的是否一样

        如果不一致，则RenderList添加一条，wineType是subWineType，winery是subWinery，其他属性是null;

        如果一致，则RenderListi添加DataList中的当前循环的这一条；
     * @throws Exception
     */
    @Override
    public void exportData(HttpServletResponse response) throws Exception {
        List<WineInventory> dataList = wineInventoryMapper.selectList(Wrappers.<WineInventory>query().lambda().eq(WineInventory::getStatus, 1).orderByAsc(WineInventory::getLineIndex));
        List<WineInventoryExcelModel> renderList = this.buildRenderList(dataList);
        
        // 从classpath资源目录获取文件
        String templateFileName = "Montrachet_Trade_Price_List.xls";
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(templateFileName);
        if (inputStream == null) {
            throw new FileNotFoundException("模板文件未找到: " + templateFileName);
        }
        
        CellsUtil.authrolizeLicense();
        Workbook workbook = new Workbook(inputStream);
    
        WorkbookDesigner designer = new WorkbookDesigner();
        designer.setWorkbook(workbook);
        designer.setDataSource("Ch", renderList);
        designer.process();
    
        // 在数据绑定后设置蓝色底色
        setBlueBackgroundColor(workbook);
        
        // 设置响应头
        response.setContentType("application/vnd.ms-excel");
        // 取系统当前时间年月日的字符串
        LocalDate date = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedDate = date.format(formatter);
        String fileName = "Montrachet_Trade_Price_List_" + formattedDate + ".xls";
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
    
        // 将工作簿直接写入响应流
        workbook.save(response.getOutputStream(), SaveFormat.EXCEL_97_TO_2003);
    }

    /**
     * 设置蓝色底色
     * 规则：
     * 1. 每行第三列单元格如果不存在内容，且第一列和第二列都存在内容，则这一行的第一列和第二列底色是蓝色底色
     * 2. 每行第三列单元格如果不存在内容，且第二列不存在内容，第一列存在内容，则第一列是蓝色底色
     *
     * @param workbook Excel工作簿
     */
    private void setBlueBackgroundColor(Workbook workbook) throws Exception {
            // 处理样式
        Worksheet worksheet = workbook.getWorksheets().get(0);
        Cells cells = worksheet.getCells();

        // 查找数据起始行（假设模板中使用了&=Ch.WineType等标记）
        FindOptions opts = new FindOptions();
        opts.setLookInType(LookInType.VALUES);
        opts.setLookAtType(LookAtType.START_WITH);

        int dataStartRow = 2;
        int dataEndRow = cells.getMaxDataRow();

        // 遍历数据行设置背景色
        for (int rowIdx = dataStartRow; rowIdx <= dataEndRow; rowIdx++) {
            Row row = cells.getRow(rowIdx);
            if (row == null) continue;

            Cell cellC = row.get(2); // 第三列（索引2）
            if (isCellEmpty(cellC)) {
                Cell cellA = row.get(0);
                Cell cellB = row.get(1);

                boolean hasA = !isCellEmpty(cellA);
                boolean hasB = !isCellEmpty(cellB);

                if (hasA && hasB) {
                    applyBackgroundColor(cellA, Color.fromArgb(0,161,254));
                    applyBackgroundColor(cellB, Color.fromArgb(0,161,254));
                } else if (hasA) {
                    applyBackgroundColor(cellA, Color.fromArgb(0,161,254));
                } else if (hasB) {
                    applyBackgroundColor(cellB, Color.fromArgb(0,161,254));
                }
            }
        }
    }
    private List<WineInventoryExcelModel> buildRenderList(List<WineInventory> dataList){
        List<WineInventoryExcelModel> renderList = new ArrayList<>();
        currentSubWinery = null;
        currentSubWineType = null;
        for(WineInventory data:dataList){
            String subWineType = data.getSubWineType();
            String subWinery = data.getSubWinery();
            WineInventoryExcelModel excelModel = new WineInventoryExcelModel();
            if(!StringUtils.hasText(currentSubWineType)){
                WineInventoryExcelModel initModel = new WineInventoryExcelModel();
                currentSubWineType = subWineType;
                currentSubWinery = subWinery;
                initModel.setWineType(subWineType);
                initModel.setWinery(subWinery);
                renderList.add(initModel);
            }

            if(currentSubWineType.equals(data.getSubWineType())&&currentSubWinery.equals(data.getSubWinery())){
                if((StringUtils.hasText(data.getCuveeName()) && StringUtils.hasText(renderList.get(renderList.size()-1).getCuveeName())) && (!(data.getWineType().equals(renderList.get(renderList.size()-1).getWineType())) || !(data.getWinery().equals(renderList.get(renderList.size()-1).getWinery())))){
                    // subWineType相同、subWinery相同、(WineType不相同 or Winery不同)
                    WineInventoryExcelModel blankLineModel = new WineInventoryExcelModel();
                    renderList.add(blankLineModel);
                }
                BeanUtils.copyProperties(data, excelModel);
                renderList.add(excelModel);
            }else{
                currentSubWineType = data.getSubWineType();
                currentSubWinery = data.getSubWinery();
                WineInventoryExcelModel initModel = new WineInventoryExcelModel();
                initModel.setWineType(subWineType);
                initModel.setWinery(subWinery);
                renderList.add(initModel);
                BeanUtils.copyProperties(data, excelModel);
                renderList.add(excelModel);
            }
        }
        
        // 处理数值字段，将小数点后为0的值转换为整数形式
        for (WineInventoryExcelModel model : renderList) {
            // 处理tradePrice
            if (StringUtils.hasText(model.getTradePrice())) {
                model.setTradePrice(removeTrailingZeros(model.getTradePrice()));
            }
            
            // 处理quantity（如果是字符串类型）
            if (model.getQuantity() != null) {
                String quantityStr = String.valueOf(model.getQuantity());
                model.setQuantity(removeTrailingZeros(quantityStr));
            }
            
            // 处理retailPrice
            if (StringUtils.hasText(model.getRetailPrice())) {
                model.setRetailPrice(removeTrailingZeros(model.getRetailPrice()));
            }
            
            // 处理distributorPrice
            if (StringUtils.hasText(model.getDistributorPrice())) {
                model.setDistributorPrice(removeTrailingZeros(model.getDistributorPrice()));
            }
            
            // 处理costPrice
            if (StringUtils.hasText(model.getCostPrice())) {
                model.setCostPrice(removeTrailingZeros(model.getCostPrice()));
            }
        }
        
        return renderList;
    }
    
    /**
     * 移除数值字符串中的尾随零
     * 例如：将"5.0"转换为"5"，但保留"5.5"不变
     */
    private String removeTrailingZeros(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        
        try {
            // 尝试解析为数值
            if (value.contains(".")) {
                // 如果小数点后全是0，则移除小数点和后面的0
                if (value.matches(".*\\.0+$")) {
                    return value.replaceAll("\\.0+$", "");
                }
            }
        } catch (Exception e) {
            // 解析失败，返回原值
            log.debug("移除尾随零失败，值: {}", value);
        }
        
        return value;
    }





    // 判断单元格是否为空
    private boolean isCellEmpty(Cell cell) {
        if (cell == null) return true;
        switch (cell.getType()) {
            case IS_STRING:
                return cell.getStringValue().trim().isEmpty();
            case IS_NULL:
                return true;
            default:
                return false; // 其他类型（数字、布尔等）视为有内容
        }
    }

    // 应用背景色（保留原有样式）
    private void applyBackgroundColor(Cell cell, Color color) {
        if (cell == null) return;

        Style style = cell.getStyle();
        style.setForegroundColor(color);
        style.setPattern(BackgroundType.SOLID);

        StyleFlag flag = new StyleFlag();
        flag.setCellShading(true); // 仅修改背景色
        cell.setStyle(style, flag);
    }

    @Override
    public List<WineInventory> listAll(String wineType, String winery, Integer status,String vintage) {
        LambdaQueryWrapper<WineInventory> queryWrapper = new LambdaQueryWrapper<>();

        // 添加状态条件，只查询有效数据
        queryWrapper.eq(WineInventory::getStatus, status);

        // 添加排序条件
        queryWrapper.orderByAsc(WineInventory::getLineIndex);

        // 添加查询条件
        if (StringUtils.hasText(wineType)) {
            queryWrapper.like(WineInventory::getWineType, wineType);
        }

        if (StringUtils.hasText(winery)) {
            queryWrapper.like(WineInventory::getWinery, winery);
        }

        if (StringUtils.hasText(vintage)) {
            queryWrapper.like(WineInventory::getVintage, vintage);
        }

        return this.list(queryWrapper);
    }
}
