package com.easylive.web.controller;

import com.easylive.entity.constants.Constants;
import com.easylive.entity.po.VideoDanmu;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.query.VideoDanmuQuery;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.service.VideoDanmuService;
import com.easylive.service.impl.VideoInfoServiceImpl;
import com.easylive.web.annotation.GlobalInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Date;


/**
 * 视频弹幕控制器类，用于处理与视频弹幕相关的HTTP请求。
 */
@RestController
@Validated
@RequestMapping("/danmu")
@Slf4j
public class VideoDanmuController extends ABaseController {

    private static final Logger logger = LoggerFactory.getLogger(VideoDanmuController.class);

    @Resource
    private VideoDanmuService videoDanmuService;

    @Resource
    private VideoInfoServiceImpl videoInfoService;

    /**
     * 处理请求路径为/danmu/loadDanmu的HTTP GET请求，用于加载视频弹幕。
     *
     * @param fileId  视频文件ID，不能为空
     * @param videoId 视频ID，不能为空
     * @return 包含视频弹幕列表的成功响应对象，如果视频互动设置不允许弹幕则返回空列表
     */
    @RequestMapping("/loadDanmu")
    @GlobalInterceptor
    public ResponseVO loadDanmu(@NotEmpty String fileId, @NotEmpty String videoId) {

        VideoInfo videoInfo = videoInfoService.getVideoInfoByVideoId(videoId);
        if (videoInfo.getInteraction() != null && videoInfo.getInteraction().contains(Constants.ZERO.toString())) {
            return getSuccessResponseVO(new ArrayList<>());
        }

        VideoDanmuQuery videoDanmuQuery = new VideoDanmuQuery();
        videoDanmuQuery.setFileId(fileId);
        videoDanmuQuery.setOrderBy("danmu_id asc");
        return getSuccessResponseVO(videoDanmuService.findListByParam(videoDanmuQuery));
    }

    /**
     * 处理请求路径为/danmu/postDanmu的HTTP POST请求，用于发布弹幕。
     *
     * @param videoId 视频ID，不能为空
     * @param fileId  视频文件ID，不能为空
     * @param text    弹幕文本，不能为空，最大长度为200
     * @param mode    弹幕模式，不能为空
     * @param color   弹幕颜色，不能为空
     * @param time    弹幕显示时间，不能为空
     * @return 发布成功的响应对象
     */
    @RequestMapping("/postDanmu")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO postDanmu(@NotEmpty String videoId,
                                @NotEmpty String fileId,
                                @NotEmpty @Size(max = 200) String text,
                                @NotNull Integer mode,
                                @NotEmpty String color,
                                @NotNull Integer time) {
        VideoDanmu videoDanmu = new VideoDanmu();
        videoDanmu.setVideoId(videoId);
        videoDanmu.setFileId(fileId);
        videoDanmu.setText(text);
        videoDanmu.setMode(mode);
        videoDanmu.setColor(color);
        videoDanmu.setTime(time);
        videoDanmu.setUserId(getTokenUserInfoDto().getUserId());
        videoDanmu.setPostTime(new Date());
        videoDanmuService.saveVideoDanmu(videoDanmu);
        return getSuccessResponseVO(null);
    }
}