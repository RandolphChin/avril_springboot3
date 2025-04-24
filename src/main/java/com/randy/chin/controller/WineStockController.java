package com.randy.chin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.randy.chin.entity.WineStock;
import com.randy.chin.service.WineStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品库存控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/wine-stocks")
@RequiredArgsConstructor
public class WineStockController {

    private final WineStockService wineStockService;

    /**
     * 分页查询商品库存列表
     */
    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "1") Integer current,
                                   @RequestParam(defaultValue = "10") Integer size,
                                   @RequestParam(required = false) String firstLevelCategory,
                                   @RequestParam(required = false) String secondLevelCategory,
                                   @RequestParam(required = false) String productName) {
        Page<WineStock> page = new Page<>(current, size);
        LambdaQueryWrapper<WineStock> queryWrapper = new LambdaQueryWrapper<>();

        if (firstLevelCategory != null && !firstLevelCategory.isEmpty()) {
            queryWrapper.like(WineStock::getFirstLevelCategory, firstLevelCategory);
        }

        if (secondLevelCategory != null && !secondLevelCategory.isEmpty()) {
            queryWrapper.like(WineStock::getSecondLevelCategory, secondLevelCategory);
        }

        if (productName != null && !productName.isEmpty()) {
            queryWrapper.like(WineStock::getProductName, productName);
        }

        Page<WineStock> wineStockPage = wineStockService.page(page, queryWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("records", wineStockPage.getRecords());
        result.put("total", wineStockPage.getTotal());
        result.put("size", wineStockPage.getSize());
        result.put("current", wineStockPage.getCurrent());

        return result;
    }

    /**
     * 根据ID获取商品库存
     */
    @GetMapping("/{id}")
    public WineStock getById(@PathVariable Integer id) {
        return wineStockService.getById(id);
    }

    /**
     * 创建商品库存
     */
    @PostMapping
    public WineStock create(@RequestBody WineStock wineStock) {
        wineStockService.save(wineStock);
        return wineStock;
    }

    /**
     * 更新商品库存
     */
    @PutMapping("/{id}")
    public WineStock update(@PathVariable Integer id, @RequestBody WineStock wineStock) {
        wineStock.setId(id);
        wineStockService.updateById(wineStock);
        return wineStock;
    }

    /**
     * 删除商品库存
     */
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Integer id) {
        return wineStockService.removeById(id);
    }

    /**
     * 导出商品库存数据
     */
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws Exception {
        wineStockService.exportData(response);
    }

    /**
     * 导入商品库存数据
     */
    @PostMapping("/import")
    public Map<String, Object> importData(@RequestParam("file") MultipartFile file) throws IOException {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = wineStockService.importData(file);
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
     * 查询所有商品库存（不分页）
     */
    @GetMapping("/all")
    public List<WineStock> listAll(@RequestParam(required = false) String firstLevelCategory,
                                   @RequestParam(required = false) String secondLevelCategory,
                                   @RequestParam(required = false) String productName) {
        return wineStockService.listAll(firstLevelCategory, secondLevelCategory, productName);
    }

    @PostMapping("/reNewWineInventory")
    public void reNewWineInventory() {
        wineStockService.reNewWineInventory();
    }
}
