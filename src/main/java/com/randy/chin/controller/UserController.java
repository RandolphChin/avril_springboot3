package com.randy.chin.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.randy.chin.entity.User;
import com.randy.chin.excel.UserExcelListener;
import com.randy.chin.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 分页查询用户列表
     */
    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "1") Integer current,
                                   @RequestParam(defaultValue = "10") Integer size,
                                   @RequestParam(required = false) String username) {
        Page<User> page = new Page<>(current, size);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isEmpty()) {
            queryWrapper.like(User::getUsername, username);
        }

        Page<User> userPage = userService.page(page, queryWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("records", userPage.getRecords());
        result.put("total", userPage.getTotal());
        result.put("size", userPage.getSize());
        result.put("current", userPage.getCurrent());

        return result;
    }

    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    public User getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    /**
     * 创建用户
     */
    @PostMapping
    public User create(@RequestBody User user) {
        userService.save(user);
        return user;
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public User update(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        userService.updateById(user);
        return user;
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return userService.removeById(id);
    }

    /**
     * 导出用户数据
     */
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("用户数据").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            List<User> list = userService.list();
            EasyExcel.write(response.getOutputStream(), User.class).sheet("用户数据").doWrite(list);
        } catch (Exception e) {
            log.error("导出Excel异常", e);
            response.reset();
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            Map<String, String> map = new HashMap<>();
            map.put("status", "failure");
            map.put("message", "导出Excel失败：" + e.getMessage());
            response.getWriter().println(map);
        }
    }

    /**
     * 导入用户数据
     */
    @PostMapping("/import")
    public Map<String, Object> importData(@RequestParam("file") MultipartFile file) throws IOException {
        Map<String, Object> result = new HashMap<>();
        try {
            EasyExcel.read(file.getInputStream(), User.class, new UserExcelListener(userService)).sheet().doRead();
            result.put("status", "success");
            result.put("message", "导入成功");
        } catch (Exception e) {
            log.error("导入Excel异常", e);
            result.put("status", "failure");
            result.put("message", "导入失败：" + e.getMessage());
        }
        return result;
    }
}
