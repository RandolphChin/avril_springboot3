package com.randy.chin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("id_segment")
public class IdSegment {
    @TableId(value = "biz_tag", type = IdType.INPUT) // 手动指定主键
    private String bizTag;

    private Long maxId;
    private Integer step;

    @Version  // 乐观锁标记
    private Integer version;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
