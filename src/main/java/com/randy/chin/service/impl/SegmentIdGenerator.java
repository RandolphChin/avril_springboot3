package com.randy.chin.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.randy.chin.entity.IdSegment;
import com.randy.chin.mapper.IdSegmentMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SegmentIdGenerator {
    private final IdSegmentMapper idSegmentMapper;
    private final Map<String, SegmentBuffer> bufferMap = new ConcurrentHashMap<>();

    public SegmentIdGenerator(IdSegmentMapper idSegmentMapper) {
        this.idSegmentMapper = idSegmentMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public synchronized long nextId(String bizTag) {
        SegmentBuffer buffer = bufferMap.computeIfAbsent(bizTag, k -> new SegmentBuffer());
        if (buffer.remaining() <= 0) {
            loadNextSegment(bizTag, buffer);
        }
        return buffer.nextId();
    }

    private void loadNextSegment(String bizTag, SegmentBuffer buffer) {
        // 1. 查询当前号段信息
        IdSegment currentSegment = idSegmentMapper.selectByBizTag(bizTag);
        if (currentSegment == null) {
            throw new RuntimeException("业务标识未初始化: " + bizTag);
        }

        // 2. 构建更新后的对象（利用 MyBatis-Plus 乐观锁）
        IdSegment updateSegment = new IdSegment();
        updateSegment.setBizTag(bizTag);
        updateSegment.setMaxId(currentSegment.getMaxId() + currentSegment.getStep());
        // updateSegment.setVersion(currentSegment.getVersion()); // 设置当前版本号

        // 3. 执行乐观锁更新
        int affected = idSegmentMapper.update(updateSegment,
                new LambdaUpdateWrapper<IdSegment>()
                        .eq(IdSegment::getBizTag, bizTag)
                        .eq(IdSegment::getVersion, currentSegment.getVersion())
        );

        if (affected == 0) {
            throw new RuntimeException("并发更新冲突，重试获取号段");
        }

        // 4. 查询最新号段范围
        IdSegment newSegment = idSegmentMapper.selectByBizTag(bizTag);
        buffer.switchBuffer(
                newSegment.getMaxId() - newSegment.getStep() + 1,  // 修改为 maxId - step + 1
                newSegment.getMaxId(),
                newSegment.getVersion()
        );
    }

    /**
     * 号段缓存（双Buffer）
     */
    private static class SegmentBuffer {
        private volatile long currentStart;
        private volatile long currentEnd;
        private volatile int currentVersion;
        private long nextStart;
        private long nextEnd;
        private int nextVersion;

        public long nextId() {
            return currentStart++;
        }

        public int remaining() {
            return (int) (currentEnd - currentStart);
        }

        public void switchBuffer(long newStart, long newEnd, int newVersion) {
            // 首次加载时直接使用新获取的号段范围
            if (this.currentEnd == 0) {
                this.currentStart = newStart;
                this.currentEnd = newEnd;
                this.currentVersion = newVersion;
            } else {
                // 非首次加载时，切换到预加载的下一Buffer
                this.currentStart = this.nextStart;
                this.currentEnd = this.nextEnd;
                this.currentVersion = this.nextVersion;
            }

            // 异步预加载下一个号段
            this.nextStart = newStart;
            this.nextEnd = newEnd;
            this.nextVersion = newVersion;
        }

        public int getCurrentVersion() {
            return currentVersion;
        }
    }
}
