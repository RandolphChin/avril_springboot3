package com.randy.chin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.randy.chin.entity.WineInventory;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 葡萄酒库存服务接口
 */
public interface WineInventoryService extends IService<WineInventory> {

    /**
     * 导入葡萄酒库存数据
     * @param file Excel文件
     * @return 导入结果
     * @throws IOException IO异常
     */
    boolean importData(MultipartFile file) throws IOException;

    /**
     * 导出葡萄酒库存数据
     * @return 葡萄酒库存列表
     * @throws Exception
     */
    void exportData(HttpServletResponse response) throws Exception;

    /**
     * 查询所有葡萄酒库存（不分页）
     * @param wineType 葡萄酒类型
     * @param winery 酒庄
     * @param vintage 年份
     * @return 葡萄酒库存列表
     */
    List<WineInventory> listAll(String wineType, String winery,Integer status, String vintage);
}
