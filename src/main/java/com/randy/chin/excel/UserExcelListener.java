package com.randy.chin.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.randy.chin.entity.User;
import com.randy.chin.service.UserService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel导入监听器
 */
@Slf4j
public class UserExcelListener extends AnalysisEventListener<User> {

    /**
     * 批处理阈值
     */
    private static final int BATCH_COUNT = 100;
    
    private List<User> list = new ArrayList<>();
    
    private UserService userService;
    
    public UserExcelListener(UserService userService) {
        this.userService = userService;
    }
    
    @Override
    public void invoke(User user, AnalysisContext analysisContext) {
        log.info("解析到一条数据:{}", user);
        list.add(user);
        if (list.size() >= BATCH_COUNT) {
            saveData();
            list.clear();
        }
    }
    
    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        saveData();
        log.info("所有数据解析完成！");
    }
    
    /**
     * 保存数据
     */
    private void saveData() {
        log.info("{}条数据，开始存储数据库！", list.size());
        userService.saveBatch(list);
        log.info("存储数据库成功！");
    }
}