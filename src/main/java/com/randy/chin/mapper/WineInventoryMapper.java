package com.randy.chin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.randy.chin.entity.WineInventory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 葡萄酒库存Mapper接口
 */
@Mapper
public interface WineInventoryMapper extends BaseMapper<WineInventory> {
}