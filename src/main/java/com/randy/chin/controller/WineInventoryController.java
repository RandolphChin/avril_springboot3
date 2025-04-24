package com.randy.chin.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.randy.chin.entity.WineInventory;
import com.randy.chin.excel.WineInventoryExcelModel;
import com.randy.chin.service.WineInventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 葡萄酒库存控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/wine-inventories")
@RequiredArgsConstructor
public class WineInventoryController {

    private final WineInventoryService wineInventoryService;

    /**
     * 分页查询葡萄酒库存列表
     */
    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "1") Integer current,
                                   @RequestParam(defaultValue = "10") Integer size,
                                   @RequestParam(required = false) String wineType,
                                   @RequestParam(required = false) String winery,
                                   @RequestParam(required = false) String vintage) {
        Page<WineInventory> page = new Page<>(current, size);
        LambdaQueryWrapper<WineInventory> queryWrapper = new LambdaQueryWrapper<>();

        if (wineType != null && !wineType.isEmpty()) {
            queryWrapper.like(WineInventory::getWineType, wineType);
        }

        if (winery != null && !winery.isEmpty()) {
            queryWrapper.like(WineInventory::getWinery, winery);
        }

        if (vintage != null && !vintage.isEmpty()) {
            queryWrapper.like(WineInventory::getVintage, vintage);
        }

        Page<WineInventory> wineInventoryPage = wineInventoryService.page(page, queryWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("records", wineInventoryPage.getRecords());
        result.put("total", wineInventoryPage.getTotal());
        result.put("size", wineInventoryPage.getSize());
        result.put("current", wineInventoryPage.getCurrent());

        return result;
    }

    /**
     * 根据ID获取葡萄酒库存
     */
    @GetMapping("/{id}")
    public WineInventory getById(@PathVariable Integer id) {
        return wineInventoryService.getById(id);
    }

    /**
     * 创建葡萄酒库存
     */
    @PostMapping
    public WineInventory create(@RequestBody WineInventory wineInventory) {
        wineInventoryService.save(wineInventory);
        return wineInventory;
    }

    /**
     * 更新葡萄酒库存
     */
    @PutMapping("/{id}")
    public WineInventory update(@PathVariable Integer id, @RequestBody WineInventory wineInventory) {
        wineInventory.setId(id);
        wineInventoryService.updateById(wineInventory);
        return wineInventory;
    }

    /**
     * 删除葡萄酒库存
     */
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Integer id) {
        return wineInventoryService.removeById(id);
    }

    /**
     * 导出葡萄酒库存数据
     */
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws Exception {
            wineInventoryService.exportData(response);
    }

    /**
     * 导入葡萄酒库存数据
     */
    @PostMapping("/import")
    public Map<String, Object> importData(@RequestParam("file") MultipartFile file) throws IOException {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = wineInventoryService.importData(file);
            result.put("status", success ? "success" : "failure");
            result.put("message", success ? "导入成功" : "导入失败");
        } catch (Exception e) {
            log.error("导入Excel异常", e);
            result.put("status", "failure");
            result.put("message", "导入失败：" + e.getMessage());
        }
        return result;
    }
    
    /**
     * 查询所有葡萄酒库存（不分页）
     */
    @GetMapping("/all")
    public List<WineInventory> listAll(@RequestParam(required = false) String wineType,
                                      @RequestParam(required = false) String winery,
                                      @RequestParam(required = false) Integer status,
                                      @RequestParam(required = false) String vintage) {
        return wineInventoryService.listAll(wineType, winery, status, vintage);
    }
}
