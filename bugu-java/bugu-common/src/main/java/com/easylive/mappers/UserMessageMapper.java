package com.easylive.mappers;

import com.easylive.entity.dto.UserMessageCountDto;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户消息表 数据库操作接口
 */
public interface UserMessageMapper<T, P> extends BaseMapper<T, P> {

    /**
     * 根据MessageId更新
     */
    Integer updateByMessageId(@Param("bean") T t, @Param("messageId") Integer messageId);


    /**
     * 根据MessageId删除
     */
    Integer deleteByMessageId(@Param("messageId") Integer messageId);


    /**
     * 根据MessageId获取对象
     */
    T selectByMessageId(@Param("messageId") Integer messageId);


    List<UserMessageCountDto> getMessageTypeNoReadCount(@Param("userId") String userId);
}
