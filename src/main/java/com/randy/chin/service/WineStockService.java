package com.randy.chin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.randy.chin.entity.WineStock;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 商品库存服务接口
 */
public interface WineStockService extends IService<WineStock> {

    /**
     * 导入商品库存数据
     * @param file Excel文件
     * @return 导入结果
     * @throws IOException IO异常
     */
    boolean importData(MultipartFile file) throws IOException;

    /**
     * 导出商品库存数据
     * @param response HTTP响应对象
     * @throws Exception 异常
     */
    void exportData(HttpServletResponse response) throws Exception;

    /**
     * 查询所有商品库存（不分页）
     * @param firstLevelCategory 一级分类
     * @param secondLevelCategory 二级分类
     * @param productName 商品名称
     * @return 商品库存列表
     */
    List<WineStock> listAll(String firstLevelCategory, String secondLevelCategory, String productName);

    // 根据库存更新报表
    public void reNewWineInventory();

    /**
     * 使用Aspose Cells导入Excel数据
     * 解决EasyExcel无法识别某些标题的问题
     */
    boolean importDataWithAspose(MultipartFile file) throws Exception;
}