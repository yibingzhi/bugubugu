package com.easylive.web.controller;

import com.easylive.component.EsSearchComponent;
import com.easylive.component.RedisComponent;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.enums.*;
import com.easylive.entity.po.UserAction;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.po.VideoInfoFile;
import com.easylive.entity.query.UserActionQuery;
import com.easylive.entity.query.VideoInfoFileQuery;
import com.easylive.entity.query.VideoInfoQuery;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.entity.vo.VideoInfoResultVo;
import com.easylive.entity.vo.VideoInfoVo;
import com.easylive.exception.BusinessException;
import com.easylive.service.UserActionService;
import com.easylive.service.VideoInfoFileService;
import com.easylive.service.VideoInfoService;
import com.easylive.utils.CopyTools;
import com.easylive.web.annotation.GlobalInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 视频控制器类，用于处理与视频相关的HTTP请求。
 */
@RestController
@Validated
@RequestMapping("/video")
@Slf4j
public class VideoController extends ABaseController {

    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    @Resource
    private VideoInfoService videoInfoService;

    @Resource
    private VideoInfoFileService videoInfoFileService;

    @Resource
    private UserActionService userActionService;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private EsSearchComponent esSearchComponent;

    /**
     * 处理请求路径为/video/loadRecommendVideo的HTTP GET请求，用于加载推荐视频列表。
     *
     * @return 包含推荐视频列表的成功响应对象
     */
    @RequestMapping("/loadRecommendVideo")
    @GlobalInterceptor
    public ResponseVO loadRecommendVideo() {
        VideoInfoQuery videoInfoQuery = new VideoInfoQuery();
        videoInfoQuery.setQueryUserInfo(true);
        videoInfoQuery.setOrderBy("create_time desc");
        videoInfoQuery.setRecommendType(VideoRecommendTypeEnum.RECOMMEND.getType());
        List<VideoInfo> recommendVideoList = videoInfoService.findListByParam(videoInfoQuery);
        return getSuccessResponseVO(recommendVideoList);
    }

    /**
     * 处理请求路径为/video/loadVideo的HTTP GET请求，用于加载视频列表。
     *
     * @param pCategoryId 父分类ID
     * @param categoryId  分类ID
     * @param pageNo      页码
     * @return 包含视频列表分页结果的成功响应对象
     */
    @RequestMapping("/loadVideo")
    @GlobalInterceptor
    public ResponseVO postVideo(Integer pCategoryId, Integer categoryId, Integer pageNo) {
        VideoInfoQuery videoInfoQuery = new VideoInfoQuery();
        videoInfoQuery.setCategoryId(categoryId);
        videoInfoQuery.setpCategoryId(pCategoryId);
        videoInfoQuery.setPageNo(pageNo);
        videoInfoQuery.setQueryUserInfo(true);
        videoInfoQuery.setOrderBy("create_time desc");
        if (categoryId == null && pCategoryId == null) {
            videoInfoQuery.setRecommendType(VideoRecommendTypeEnum.NO_RECOMMEND.getType());
        }
        PaginationResultVO resultVO = videoInfoService.findListByPage(videoInfoQuery);
        return getSuccessResponseVO(resultVO);
    }

    /**
     * 处理请求路径为/video/loadVideoPList的HTTP GET请求，用于加载视频的播放列表。
     *
     * @param videoId 视频ID，不能为空
     * @return 包含视频播放列表的成功响应对象
     */
    @RequestMapping("/loadVideoPList")
    @GlobalInterceptor
    public ResponseVO loadVideoPList(@NotEmpty String videoId) {
        VideoInfoFileQuery videoInfoQuery = new VideoInfoFileQuery();
        videoInfoQuery.setVideoId(videoId);
        videoInfoQuery.setOrderBy("file_index asc");
        List<VideoInfoFile> fileList = videoInfoFileService.findListByParam(videoInfoQuery);
        return getSuccessResponseVO(fileList);
    }

    /**
     * 处理请求路径为/video/getVideoInfo的HTTP GET请求，用于获取视频信息。
     *
     * @param videoId 视频ID，不能为空
     * @return 包含视频信息和用户对视频操作信息的成功响应对象，如果视频不存在则抛出业务异常
     */
    @RequestMapping("/getVideoInfo")
    @GlobalInterceptor
    public ResponseVO getVideoInfo(@NotEmpty String videoId) {
        VideoInfo videoInfo = videoInfoService.getVideoInfoByVideoId(videoId);
        if (null == videoInfo) {
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
        TokenUserInfoDto userInfoDto = getTokenUserInfoDto();

        List<UserAction> userActionList = new ArrayList<>();
        if (userInfoDto != null) {
            UserActionQuery actionQuery = new UserActionQuery();
            actionQuery.setVideoId(videoId);
            actionQuery.setUserId(userInfoDto.getUserId());
            actionQuery.setActionTypeArray(new Integer[]{UserActionTypeEnum.VIDEO_LIKE.getType(), UserActionTypeEnum.VIDEO_COLLECT.getType(),
                    UserActionTypeEnum.VIDEO_COIN.getType(),});
            userActionList = userActionService.findListByParam(actionQuery);
        }
        VideoInfoResultVo resultVo = new VideoInfoResultVo();
        resultVo.setVideoInfo(CopyTools.copy(videoInfo, VideoInfoVo.class));
        resultVo.setUserActionList(userActionList);
        return getSuccessResponseVO(resultVo);
    }

    /**
     * 处理请求路径为/video/getVideoRecommend的HTTP GET请求，用于获取视频推荐列表。
     *
     * @param keyword 搜索关键词，不能为空
     * @param videoId 视频ID，不能为空
     * @return 包含视频推荐列表的成功响应对象，排除当前视频
     */
    @RequestMapping("/getVideoRecommend")
    @GlobalInterceptor
    public ResponseVO getVideoRecommend(@NotEmpty String keyword, @NotEmpty String videoId) {
        List<VideoInfo> videoInfoList = esSearchComponent.search(false, keyword, SearchOrderTypeEnum.VIDEO_PLAY.getType(), 1, PageSize.SIZE10.getSize()).getList();
        videoInfoList = videoInfoList.stream().filter(item -> !item.getVideoId().equals(videoId)).collect(Collectors.toList());
        return getSuccessResponseVO(videoInfoList);
    }

    /**
     * 处理请求路径为/video/reportVideoPlayOnline的HTTP POST请求，用于上报视频在线播放数据。
     *
     * @param fileId   文件ID，不能为空
     * @param deviceId 设备ID
     * @return 上报结果的成功响应对象
     */
    @RequestMapping("/reportVideoPlayOnline")
    @GlobalInterceptor
    public ResponseVO reportVideoPlayOnline(@NotEmpty String fileId, String deviceId) {
        Integer count = redisComponent.reportVideoPlayOnline(fileId, deviceId);
        return getSuccessResponseVO(count);
    }

    /**
     * 处理请求路径为/video/search的HTTP GET请求，用于搜索视频。
     *
     * @param keyword   搜索关键词，不能为空
     * @param orderType 排序类型
     * @param pageNo    页码
     * @return 包含搜索结果分页的成功响应对象
     */
    @RequestMapping("/search")
    @GlobalInterceptor
    public ResponseVO search(@NotEmpty String keyword, Integer orderType, Integer pageNo) {
        redisComponent.addKeywordCount(keyword);
        PaginationResultVO resultVO = esSearchComponent.search(true, keyword, orderType, pageNo, PageSize.SIZE30.getSize());
        return getSuccessResponseVO(resultVO);
    }

    /**
     * 处理请求路径为/video/getSearchKeywordTop的HTTP GET请求，用于获取搜索关键词排行榜。
     *
     * @return 包含搜索关键词排行榜的成功响应对象
     */
    @RequestMapping("/getSearchKeywordTop")
    @GlobalInterceptor
    public ResponseVO getSearchKeywordTop() {
        List<String> keywordList = redisComponent.getKeywordTop(Constants.LENGTH_10);
        return getSuccessResponseVO(keywordList);
    }

    /**
     * 处理请求路径为/video/loadHotVideoList的HTTP GET请求，用于加载热门视频列表。
     *
     * @param pageNo 页码
     * @return 包含热门视频列表分页结果的成功响应对象
     */
    @RequestMapping("/loadHotVideoList")
    @GlobalInterceptor
    public ResponseVO loadHotVideoList(Integer pageNo) {
        VideoInfoQuery videoInfoQuery = new VideoInfoQuery();
        videoInfoQuery.setPageNo(pageNo);
        videoInfoQuery.setQueryUserInfo(true);
        videoInfoQuery.setOrderBy("play_count desc");
        videoInfoQuery.setLastPlayHour(Constants.HOUR_24);
        PaginationResultVO resultVO = videoInfoService.findListByPage(videoInfoQuery);
        return getSuccessResponseVO(resultVO);
    }
}