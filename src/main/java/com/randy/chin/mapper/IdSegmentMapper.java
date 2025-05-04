package com.randy.chin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.randy.chin.entity.IdSegment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface IdSegmentMapper extends BaseMapper<IdSegment> {
    /**
     * 自定义查询（非必须，仅为明确语义）
     */
    @Select("SELECT * FROM id_segment WHERE biz_tag = #{bizTag}")
    IdSegment selectByBizTag(@Param("bizTag") String bizTag);
}
