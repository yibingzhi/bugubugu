package com.easylive.web.controller;

import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.enums.ResponseCodeEnum;
import com.easylive.entity.po.UserVideoSeries;
import com.easylive.entity.po.UserVideoSeriesVideo;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.query.UserVideoSeriesQuery;
import com.easylive.entity.query.UserVideoSeriesVideoQuery;
import com.easylive.entity.query.VideoInfoQuery;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.entity.vo.UserVideoSeriesDetailVO;
import com.easylive.exception.BusinessException;
import com.easylive.service.UserVideoSeriesService;
import com.easylive.service.UserVideoSeriesVideoService;
import com.easylive.service.VideoInfoService;
import com.easylive.web.annotation.GlobalInterceptor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UHome视频系列控制器类，用于处理与UHome视频系列相关的HTTP请求。
 */
@RestController
@Validated
@RequestMapping("/uhome/series")
public class UHomeVideoSeriesController extends ABaseController {

    @Resource
    private VideoInfoService videoInfoService;

    @Resource
    private UserVideoSeriesService userVideoSeriesService;

    @Resource
    private UserVideoSeriesVideoService userVideoSeriesVideoService;

    /**
     * 处理请求路径为/uhome/series/loadVideoSeries的HTTP GET请求，用于加载用户的所有视频系列。
     *
     * @param userId 用户ID，不能为空
     * @return 包含用户所有视频系列的成功响应对象
     */
    @RequestMapping("/loadVideoSeries")
    @GlobalInterceptor
    public ResponseVO loadVideoSeries(@NotEmpty String userId) {
        List<UserVideoSeries> videoSeries = userVideoSeriesService.getUserAllSeries(userId);
        return getSuccessResponseVO(videoSeries);
    }

    /**
     * 处理请求路径为/uhome/series/saveVideoSeries的HTTP POST请求，用于保存视频系列。
     *
     * @param seriesId          系列ID
     * @param seriesName        系列名称，不能为空，最大长度为100
     * @param seriesDescription 系列描述，最大长度为200
     * @param videoIds          视频ID列表
     * @return 保存成功的响应对象
     */
    @RequestMapping("/saveVideoSeries")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO saveVideoSeries(Integer seriesId,
                                      @NotEmpty @Size(max = 100) String seriesName,
                                      @Size(max = 200) String seriesDescription,
                                      String videoIds) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        UserVideoSeries videoSeries = new UserVideoSeries();
        videoSeries.setUserId(tokenUserInfoDto.getUserId());
        videoSeries.setSeriesId(seriesId);
        videoSeries.setSeriesName(seriesName);
        videoSeries.setSeriesDescription(seriesDescription);
        userVideoSeriesService.saveUserVideoSeries(videoSeries, videoIds);
        return getSuccessResponseVO(null);
    }

    /**
     * 处理请求路径为/uhome/series/loadAllVideo的HTTP GET请求，用于加载所有视频（排除已在指定系列中的视频）。
     *
     * @param seriesId 系列ID
     * @return 包含所有符合条件视频信息的成功响应对象
     */
    @RequestMapping("/loadAllVideo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO loadAllVideo(Integer seriesId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        VideoInfoQuery infoQuery = new VideoInfoQuery();
        if (seriesId != null) {
            UserVideoSeriesVideoQuery videoSeriesVideoQuery = new UserVideoSeriesVideoQuery();
            videoSeriesVideoQuery.setSeriesId(seriesId);
            videoSeriesVideoQuery.setUserId(tokenUserInfoDto.getUserId());
            List<UserVideoSeriesVideo> seriesVideoList = userVideoSeriesVideoService.findListByParam(videoSeriesVideoQuery);
            List<String> videoList = seriesVideoList.stream().map(item -> item.getVideoId()).collect(Collectors.toList());
            infoQuery.setExcludeVideoIdArray(videoList.toArray(new String[videoList.size()]));
        }
        infoQuery.setUserId(tokenUserInfoDto.getUserId());
        List<VideoInfo> videoInfoList = videoInfoService.findListByParam(infoQuery);
        return getSuccessResponseVO(videoInfoList);
    }

    /**
     * 处理请求路径为/uhome/series/getVideoSeriesDetail的HTTP GET请求，用于获取视频系列详情。
     *
     * @param seriesId 系列ID，不能为空
     * @return 包含视频系列详情的成功响应对象，如果视频系列不存在则抛出业务异常
     */
    @RequestMapping("/getVideoSeriesDetail")
    @GlobalInterceptor
    public ResponseVO getVideoSeriesDetail(@NotNull Integer seriesId) {
        UserVideoSeries videoSeries = userVideoSeriesService.getUserVideoSeriesBySeriesId(seriesId);
        if (videoSeries == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }

        UserVideoSeriesVideoQuery videoSeriesVideoQuery = new UserVideoSeriesVideoQuery();
        videoSeriesVideoQuery.setOrderBy("sort asc");
        videoSeriesVideoQuery.setQueryVideoInfo(true);
        videoSeriesVideoQuery.setSeriesId(seriesId);
        List<UserVideoSeriesVideo> seriesVideoList = userVideoSeriesVideoService.findListByParam(videoSeriesVideoQuery);
        return getSuccessResponseVO(new UserVideoSeriesDetailVO(videoSeries, seriesVideoList));
    }

    /**
     * 处理请求路径为/uhome/series/saveSeriesVideo的HTTP POST请求，用于保存系列视频。
     *
     * @param seriesId 系列ID，不能为空
     * @param videoIds 视频ID列表，不能为空
     * @return 保存成功的响应对象
     */
    @RequestMapping("/saveSeriesVideo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO saveSeriesVideo(@NotNull Integer seriesId, @NotEmpty String videoIds) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        userVideoSeriesService.saveSeriesVideo(tokenUserInfoDto.getUserId(), seriesId, videoIds);
        return getSuccessResponseVO(null);
    }

    /**
     * 处理请求路径为/uhome/series/delSeriesVideo的HTTP POST请求，用于删除系列中的视频。
     *
     * @param seriesId 系列ID，不能为空
     * @param videoId  视频ID，不能为空
     * @return 删除成功的响应对象
     */
    @RequestMapping("/delSeriesVideo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO delSeriesVideo(@NotNull Integer seriesId, @NotEmpty String videoId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        userVideoSeriesService.delSeriesVideo(tokenUserInfoDto.getUserId(), seriesId, videoId);
        return getSuccessResponseVO(null);
    }

    /**
     * 处理请求路径为/uhome/series/delVideoSeries的HTTP POST请求，用于删除视频系列。
     *
     * @param seriesId 系列ID，不能为空
     * @return 删除成功的响应对象
     */
    @RequestMapping("/delVideoSeries")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO delVideoSeries(@NotNull Integer seriesId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        userVideoSeriesService.delVideoSeries(tokenUserInfoDto.getUserId(), seriesId);
        return getSuccessResponseVO(null);
    }

    /**
     * 处理请求路径为/uhome/series/changeVideoSeriesSort的HTTP POST请求，用于更改视频系列排序。
     *
     * @param seriesIds 系列ID列表，不能为空
     * @return 排序更改成功的响应对象
     */
    @RequestMapping("/changeVideoSeriesSort")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO changeVideoSeriesSort(@NotEmpty String seriesIds) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        userVideoSeriesService.changeVideoSeriesSort(tokenUserInfoDto.getUserId(), seriesIds);
        return getSuccessResponseVO(null);
    }

    /**
     * 处理请求路径为/uhome/series/loadVideoSeriesWithVideo的HTTP GET请求，用于加载包含视频的视频系列。
     *
     * @param userId 用户ID，不能为空
     * @return 包含视频的视频系列列表的成功响应对象
     */
    @RequestMapping("/loadVideoSeriesWithVideo")
    @GlobalInterceptor
    public ResponseVO loadVideoSeriesWithVideo(@NotEmpty String userId) {
        UserVideoSeriesQuery seriesQuery = new UserVideoSeriesQuery();
        seriesQuery.setUserId(userId);
        seriesQuery.setOrderBy("sort asc");
        List<UserVideoSeries> videoSeries = userVideoSeriesService.findListWithVideoList(seriesQuery);
        return getSuccessResponseVO(videoSeries);
    }
}