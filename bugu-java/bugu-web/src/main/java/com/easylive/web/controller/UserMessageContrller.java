package com.easylive.web.controller;

import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.dto.UserMessageCountDto;
import com.easylive.entity.enums.MessageReadTypeEnum;
import com.easylive.entity.po.UserMessage;
import com.easylive.entity.query.UserMessageQuery;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.service.UserMessageService;
import com.easylive.web.annotation.GlobalInterceptor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.List;


/**
 * 用户消息控制器类，用于处理与用户消息相关的HTTP请求。
 */
@RestController
@Validated
@RequestMapping("/message")
public class UserMessageContrller extends ABaseController {

    @Resource
    private UserMessageService userMessageService;

    /**
     * 处理请求路径为/message/getNoReadCount的HTTP GET请求，用于获取未读消息数量。
     *
     * @return 包含未读消息数量的成功响应对象，如果用户未登录则返回未读消息数量为0
     */
    @RequestMapping("/getNoReadCount")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO getNoReadCount() {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        if (tokenUserInfoDto == null) {
            return getSuccessResponseVO(0);
        }
        UserMessageQuery messageQuery = new UserMessageQuery();
        messageQuery.setUserId(tokenUserInfoDto.getUserId());
        messageQuery.setReadType(MessageReadTypeEnum.NO_READ.getType());
        Integer count = userMessageService.findCountByParam(messageQuery);
        return getSuccessResponseVO(count);
    }

    /**
     * 处理请求路径为/message/getNoReadCountGroup的HTTP GET请求，用于获取按消息类型分组的未读消息数量。
     *
     * @return 包含按消息类型分组的未读消息数量列表的成功响应对象
     */
    @RequestMapping("/getNoReadCountGroup")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO getNoReadCountGroup() {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        List<UserMessageCountDto> dataList = userMessageService.getMessageTypeNoReadCount(tokenUserInfoDto.getUserId());
        return getSuccessResponseVO(dataList);
    }

    /**
     * 处理请求路径为/message/readAll的HTTP POST请求，用于将指定类型的消息全部标记为已读。
     *
     * @param messageType 消息类型
     * @return 操作成功的响应对象
     */
    @RequestMapping("/readAll")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO readAll(Integer messageType) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();

        UserMessageQuery userMessageQuery = new UserMessageQuery();
        userMessageQuery.setUserId(tokenUserInfoDto.getUserId());
        userMessageQuery.setMessageType(messageType);

        UserMessage userMessage = new UserMessage();
        userMessage.setReadType(MessageReadTypeEnum.READ.getType());
        userMessageService.updateByParam(userMessage, userMessageQuery);
        return getSuccessResponseVO(null);
    }

    /**
     * 处理请求路径为/message/loadMessage的HTTP GET请求，用于加载指定类型的消息列表。
     *
     * @param messageType 消息类型，不能为空
     * @param pageNo      页码
     * @return 包含指定类型消息列表的分页结果的成功响应对象
     */
    @RequestMapping("/loadMessage")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO loadMessage(@NotNull Integer messageType, Integer pageNo) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        UserMessageQuery messageQuery = new UserMessageQuery();
        messageQuery.setMessageType(messageType);
        messageQuery.setPageNo(pageNo);
        messageQuery.setUserId(tokenUserInfoDto.getUserId());
        messageQuery.setOrderBy("message_id desc");
        PaginationResultVO resultVO = userMessageService.findListByPage(messageQuery);
        return getSuccessResponseVO(resultVO);
    }

    /**
     * 处理请求路径为/message/delMessage的HTTP POST请求，用于删除指定ID的消息。
     *
     * @param messageId 消息ID，不能为空
     * @return 操作成功的响应对象
     */
    @RequestMapping("/delMessage")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO delMessage(@NotNull Integer messageId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        UserMessageQuery messageQuery = new UserMessageQuery();
        messageQuery.setUserId(tokenUserInfoDto.getUserId());
        messageQuery.setMessageId(messageId);
        userMessageService.deleteByParam(messageQuery);
        return getSuccessResponseVO(null);
    }
}