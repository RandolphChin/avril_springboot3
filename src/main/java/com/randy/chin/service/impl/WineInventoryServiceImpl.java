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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
        return renderList;
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
