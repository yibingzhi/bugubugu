package com.easylive.web.controller;

import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.enums.ResponseCodeEnum;
import com.easylive.entity.enums.VideoStatusEnum;
import com.easylive.entity.po.VideoInfoFilePost;
import com.easylive.entity.po.VideoInfoPost;
import com.easylive.entity.query.VideoInfoFilePostQuery;
import com.easylive.entity.query.VideoInfoPostQuery;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.entity.vo.VideoPostEditInfoVo;
import com.easylive.entity.vo.VideoStatusCountInfoVO;
import com.easylive.exception.BusinessException;
import com.easylive.service.VideoInfoFilePostService;
import com.easylive.service.VideoInfoPostService;
import com.easylive.service.VideoInfoService;
import com.easylive.utils.JsonUtils;
import com.easylive.web.annotation.GlobalInterceptor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@RestController
@Validated
@RequestMapping("/ucenter")
public class UCenterVideoPostController extends ABaseController {

    @Resource
    private VideoInfoPostService videoInfoPostService;

    @Resource
    private VideoInfoFilePostService videoInfoFilePostService;

    @Resource
    private VideoInfoService videoInfoService;

    /**
     * @Description: 发布视频
     * @param: [videoId] - 视频ID
     * @param: [videoCover] - 视频封面，不能为空
     * @param: [videoName] - 视频名称，不能为空且最大长度为100
     * @param: [pCategoryId] - 主分类ID，不能为空
     * @param: [categoryId] - 子分类ID，可为空
     * @param: [postType] - 发布类型，不能为空
     * @param: [tags] - 标签，不能为空且最大长度为300
     * @param: [introduction] - 视频介绍，最大长度为2000
     * @param: [interaction] - 互动类型，最大长度为3
     * @param: [uploadFileList] - 上传文件列表，不能为空
     * @return: com.easylive.entity.vo.ResponseVO - 返回操作结果的响应对象
     */
    @RequestMapping("/postVideo")
    @GlobalInterceptor(checkLogin = true) // 检查用户登录状态
    public ResponseVO postVideo(String videoId, @NotEmpty String videoCover,
                                @NotEmpty @Size(max = 100) String videoName,
                                @NotNull Integer pCategoryId, Integer categoryId,
                                @NotNull Integer postType,
                                @NotEmpty @Size(max = 300) String tags,
                                @Size(max = 2000) String introduction,
                                @Size(max = 3) String interaction,
                                @NotEmpty String uploadFileList) {

        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto(); // 获取当前用户信息
        List<VideoInfoFilePost> fileInfoList = JsonUtils.convertJsonArray2List(uploadFileList, VideoInfoFilePost.class); // 将上传文件列表转换为对象列表

        // 创建视频信息对象并设置属性
        VideoInfoPost videoInfo = new VideoInfoPost();
        videoInfo.setVideoId(videoId);
        videoInfo.setVideoName(videoName);
        videoInfo.setVideoCover(videoCover);
        videoInfo.setpCategoryId(pCategoryId);
        videoInfo.setCategoryId(categoryId);
        videoInfo.setPostType(postType);
        videoInfo.setTags(tags);
        videoInfo.setIntroduction(introduction);
        videoInfo.setInteraction(interaction);
        videoInfo.setUserId(tokenUserInfoDto.getUserId()); // 设置用户ID

        // 保存视频信息及文件信息
        videoInfoPostService.saveVideoInfo(videoInfo, fileInfoList);
        return getSuccessResponseVO(null); // 返回成功响应
    }

    /**
     * @Description: 加载用户的视频列表
     * @param: [status] - 视频状态，可为空
     * @param: [pageNo] - 当前页面编号
     * @param: [videoNameFuzzy] - 视频名称的模糊查询字符串
     * @return: com.easylive.entity.vo.ResponseVO - 返回包含视频列表的分页结果
     */
    @RequestMapping("/loadVideoList")
    @GlobalInterceptor(checkLogin = true) // 检查用户登录状态
    public ResponseVO loadVideoList(Integer status, Integer pageNo, String videoNameFuzzy) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto(); // 获取当前用户信息
        VideoInfoPostQuery videoInfoQuery = new VideoInfoPostQuery(); // 创建视频信息查询对象
        videoInfoQuery.setUserId(tokenUserInfoDto.getUserId()); // 设置用户ID
        videoInfoQuery.setOrderBy("v.create_time desc"); // 设置排序方式为创建时间降序
        videoInfoQuery.setPageNo(pageNo); // 设置当前页面编号

        // 根据状态设置查询条件
        if (status != null) {
            if (status == -1) {
                videoInfoQuery.setExcludeStatusArray(new Integer[]{VideoStatusEnum.STATUS3.getStatus(), VideoStatusEnum.STATUS4.getStatus()});
            } else {
                videoInfoQuery.setStatus(status);
            }
        }
        videoInfoQuery.setVideoNameFuzzy(videoNameFuzzy); // 设置模糊查询视频名称
        videoInfoQuery.setQueryCountInfo(true); // 查询计数信息

        // 获取视频列表的分页结果
        PaginationResultVO resultVO = videoInfoPostService.findListByPage(videoInfoQuery);
        return getSuccessResponseVO(resultVO); // 返回成功响应
    }

    /**
     * @Description: 获取用户的视频统计信息
     * @return: com.easylive.entity.vo.ResponseVO - 返回包含视频状态统计信息的响应对象
     */
    @RequestMapping("/getVideoCountInfo")
    @GlobalInterceptor(checkLogin = true) // 检查用户登录状态
    public ResponseVO getVideoCountInfo() {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto(); // 获取当前用户信息
        VideoInfoPostQuery videoInfoQuery = new VideoInfoPostQuery(); // 创建视频信息查询对象
        videoInfoQuery.setUserId(tokenUserInfoDto.getUserId()); // 设置用户ID
        videoInfoQuery.setStatus(VideoStatusEnum.STATUS3.getStatus()); // 设置审核通过状态

        // 查询审核通过、审核不通过和进行中的视频数量
        Integer auditPassCount = videoInfoPostService.findCountByParam(videoInfoQuery);
        videoInfoQuery.setStatus(VideoStatusEnum.STATUS4.getStatus()); // 设置审核不通过状态
        Integer auditFailCount = videoInfoPostService.findCountByParam(videoInfoQuery);
        videoInfoQuery.setStatus(null); // 不设置状态
        videoInfoQuery.setExcludeStatusArray(new Integer[]{VideoStatusEnum.STATUS3.getStatus(), VideoStatusEnum.STATUS4.getStatus()}); // 排除审核通过和不通过状态
        Integer inProgress = videoInfoPostService.findCountByParam(videoInfoQuery); // 查询进行中的视频数量

        // 创建视频状态统计信息对象并设置属性
        VideoStatusCountInfoVO countInfo = new VideoStatusCountInfoVO();
        countInfo.setAuditPassCount(auditPassCount);
        countInfo.setAuditFailCount(auditFailCount);
        countInfo.setInProgress(inProgress);
        return getSuccessResponseVO(countInfo); // 返回成功响应
    }

    /**
     * @Description: 根据视频ID获取视频信息
     * @param: [videoId] - 视频ID，不能为空
     * @return: com.easylive.entity.vo.ResponseVO - 返回包含视频信息和文件信息的响应对象
     */
    @RequestMapping("/getVideoByVideoId")
    @GlobalInterceptor(checkLogin = true) // 检查用户登录状态
    public ResponseVO getVideoByVideoId(@NotEmpty String videoId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto(); // 获取当前用户信息
        VideoInfoPost videoInfoPost = this.videoInfoPostService.getVideoInfoPostByVideoId(videoId); // 获取视频信息

        // 检查视频是否存在以及用户是否有权限访问
        if (videoInfoPost == null || !videoInfoPost.getUserId().equals(tokenUserInfoDto.getUserId())) {
            throw new BusinessException(ResponseCodeEnum.CODE_404); // 抛出未找到异常
        }

        // 获取视频文件信息
        VideoInfoFilePostQuery videoInfoFilePostQuery = new VideoInfoFilePostQuery();
        videoInfoFilePostQuery.setVideoId(videoId);
        videoInfoFilePostQuery.setOrderBy("file_index asc"); // 按文件索引升序排列
        List<VideoInfoFilePost> videoInfoFilePostList = this.videoInfoFilePostService.findListByParam(videoInfoFilePostQuery);

        // 创建返回对象并设置视频信息和文件列表
        VideoPostEditInfoVo vo = new VideoPostEditInfoVo();
        vo.setVideoInfo(videoInfoPost);
        vo.setVideoInfoFileList(videoInfoFilePostList);
        return getSuccessResponseVO(vo); // 返回成功响应
    }

    /**
     * @Description: 保存视频互动信息
     * @param: [videoId] - 视频ID，不能为空
     * @param: [interaction] - 互动信息
     * @return: com.easylive.entity.vo.ResponseVO - 返回操作结果的响应对象
     */
    @RequestMapping("/saveVideoInteraction")
    @GlobalInterceptor(checkLogin = true) // 检查用户登录状态
    public ResponseVO saveVideoInteraction(@NotEmpty String videoId, String interaction) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto(); // 获取当前用户信息
        videoInfoService.changeInteraction(videoId, tokenUserInfoDto.getUserId(), interaction); // 保存互动信息
        return getSuccessResponseVO(null); // 返回成功响应
    }

    /**
     * @Description: 删除视频
     * @param: [videoId] - 视频ID，不能为空
     * @return: com.easylive.entity.vo.ResponseVO - 返回操作结果的响应对象
     */
    @RequestMapping("/deleteVideo")
    @GlobalInterceptor(checkLogin = true) // 检查用户登录状态
    public ResponseVO deleteVideo(@NotEmpty String videoId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto(); // 获取当前用户信息
        videoInfoService.deleteVideo(videoId, tokenUserInfoDto.getUserId()); // 删除指定视频
        return getSuccessResponseVO(null); // 返回成功响应
    }

}
