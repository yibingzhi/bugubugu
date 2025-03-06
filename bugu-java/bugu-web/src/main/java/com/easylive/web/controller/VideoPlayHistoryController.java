package com.easylive.web.controller;


import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.query.VideoPlayHistoryQuery;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.service.VideoPlayHistoryService;
import com.easylive.web.annotation.GlobalInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;

/**
 * 视频播放历史控制器类，用于处理与视频播放历史相关的HTTP请求。
 */
@RestController
@Validated
@RequestMapping("/history")
@Slf4j
public class VideoPlayHistoryController extends ABaseController {

    private static final Logger logger = LoggerFactory.getLogger(VideoPlayHistoryController.class);

    @Resource
    private VideoPlayHistoryService videoPlayHistoryService;

    /**
     * 处理请求路径为/history/loadHistory的HTTP GET请求，用于加载视频播放历史记录。
     *
     * @param pageNo 页码
     * @return 包含视频播放历史记录分页结果的成功响应对象
     */
    @RequestMapping("/loadHistory")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO loadHistory(Integer pageNo) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        VideoPlayHistoryQuery historyQuery = new VideoPlayHistoryQuery();
        historyQuery.setUserId(tokenUserInfoDto.getUserId());
        historyQuery.setOrderBy("last_update_time desc");
        historyQuery.setPageNo(pageNo);
        historyQuery.setQueryVideoDetail(true);
        return getSuccessResponseVO(videoPlayHistoryService.findListByPage(historyQuery));
    }

    /**
     * 处理请求路径为/history/cleanHistory的HTTP POST请求，用于清除所有视频播放历史记录。
     *
     * @return 操作成功的响应对象
     */
    @RequestMapping("/cleanHistory")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO cleanHistory() {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        VideoPlayHistoryQuery historyQuery = new VideoPlayHistoryQuery();
        historyQuery.setUserId(tokenUserInfoDto.getUserId());
        videoPlayHistoryService.deleteByParam(historyQuery);
        return getSuccessResponseVO(null);
    }

    /**
     * 处理请求路径为/history/delHistory的HTTP POST请求，用于删除指定视频的播放历史记录。
     *
     * @param videoId 视频ID，不能为空
     * @return 操作成功的响应对象
     */
    @RequestMapping("/delHistory")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO delHistory(@NotEmpty String videoId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        videoPlayHistoryService.deleteVideoPlayHistoryByUserIdAndVideoId(tokenUserInfoDto.getUserId(), videoId);
        return getSuccessResponseVO(null);
    }
}