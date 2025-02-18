package com.easylive.web.controller;

import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.query.VideoCommentQuery;
import com.easylive.entity.query.VideoDanmuQuery;
import com.easylive.entity.query.VideoInfoQuery;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.service.VideoCommentService;
import com.easylive.service.VideoDanmuService;
import com.easylive.service.VideoInfoService;
import com.easylive.web.annotation.GlobalInterceptor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@Validated
@RequestMapping("/ucenter")
public class UCenterInteractController extends ABaseController {

    @Resource
    private VideoCommentService videoCommentService;

    @Resource
    private VideoDanmuService videoDanmuService;

    @Resource
    private VideoInfoService videoInfoService;

    /**
     * @Description: 加载用户所有视频
     * @return: com.easylive.entity.vo.ResponseVO - 返回包含视频信息列表的响应对象
     */
    @RequestMapping("/loadAllVideo")
    @GlobalInterceptor(checkLogin = true) // 检查用户登录状态
    public ResponseVO loadAllVideo() {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto(); // 获取当前用户信息
        VideoInfoQuery videoInfoQuery = new VideoInfoQuery(); // 创建视频信息查询对象
        videoInfoQuery.setUserId(tokenUserInfoDto.getUserId()); // 设置用户ID
        videoInfoQuery.setOrderBy("create_time desc"); // 设置排序方式为创建时间降序
        List<VideoInfo> videoInfoList = videoInfoService.findListByParam(videoInfoQuery); // 查询视频信息列表
        return getSuccessResponseVO(videoInfoList); // 返回成功响应
    }

    /**
     * @Description: 加载视频评论
     * @param: [pageNo] - 当前页面编号
     * @param: [videoId] - 视频ID
     * @return: com.easylive.entity.vo.ResponseVO - 返回包含评论分页结果的响应对象
     */
    @RequestMapping("/loadComment")
    @GlobalInterceptor(checkLogin = true) // 检查用户登录状态
    public ResponseVO loadComment(Integer pageNo, String videoId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto(); // 获取当前用户信息
        VideoCommentQuery commentQuery = new VideoCommentQuery(); // 创建评论查询对象
        commentQuery.setVideoUserId(tokenUserInfoDto.getUserId()); // 设置视频用户ID
        commentQuery.setVideoId(videoId); // 设置视频ID
        commentQuery.setOrderBy("comment_id desc"); // 设置评论排序方式为评论ID降序
        commentQuery.setPageNo(pageNo); // 设置当前页面编号
        commentQuery.setQueryVideoInfo(true); // 查询视频信息
        PaginationResultVO resultVO = videoCommentService.findListByPage(commentQuery); // 获取评论分页结果
        return getSuccessResponseVO(resultVO); // 返回成功响应
    }

    /**
     * @Description: 删除评论
     * @param: [commentId] - 评论ID，不能为空
     * @return: com.easylive.entity.vo.ResponseVO - 返回操作结果的响应对象
     */
    @RequestMapping("/delComment")
    @GlobalInterceptor(checkLogin = true) // 检查用户登录状态
    public ResponseVO delComment(@NotNull Integer commentId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto(); // 获取当前用户信息
        videoCommentService.deleteComment(commentId, tokenUserInfoDto.getUserId()); // 删除指定评论
        return getSuccessResponseVO(null); // 返回成功响应
    }

    /**
     * @Description: 加载视频弹幕
     * @param: [pageNo] - 当前页面编号
     * @param: [videoId] - 视频ID
     * @return: com.easylive.entity.vo.ResponseVO - 返回包含弹幕分页结果的响应对象
     */
    @RequestMapping("/loadDanmu")
    @GlobalInterceptor(checkLogin = true) // 检查用户登录状态
    public ResponseVO loadDanmu(Integer pageNo, String videoId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto(); // 获取当前用户信息
        VideoDanmuQuery danmuQuery = new VideoDanmuQuery(); // 创建弹幕查询对象
        danmuQuery.setVideoUserId(tokenUserInfoDto.getUserId()); // 设置视频用户ID
        danmuQuery.setVideoId(videoId); // 设置视频ID
        danmuQuery.setOrderBy("danmu_id desc"); // 设置弹幕排序方式为弹幕ID降序
        danmuQuery.setPageNo(pageNo); // 设置当前页面编号
        danmuQuery.setQueryVideoInfo(true); // 查询视频信息
        PaginationResultVO resultVO = videoDanmuService.findListByPage(danmuQuery); // 获取弹幕分页结果
        return getSuccessResponseVO(resultVO); // 返回成功响应
    }

    /**
     * @Description: 删除弹幕
     * @param: [danmuId] - 弹幕ID，不能为空
     * @return: com.easylive.entity.vo.ResponseVO - 返回操作结果的响应对象
     */
    @RequestMapping("/delDanmu")
    @GlobalInterceptor(checkLogin = true) // 检查用户登录状态
    public ResponseVO delDanmu(@NotNull Integer danmuId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto(); // 获取当前用户信息
        videoDanmuService.deleteDanmu(tokenUserInfoDto.getUserId(), danmuId); // 删除指定弹幕
        return getSuccessResponseVO(null); // 返回成功响应
    }

}
