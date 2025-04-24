package com.randy.chin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.randy.chin.entity.WineStock;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品库存Mapper接口
 */
@Mapper
public interface WineStockMapper extends BaseMapper<WineStock> {
}