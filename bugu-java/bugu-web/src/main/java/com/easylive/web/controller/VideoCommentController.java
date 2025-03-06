package com.easylive.web.controller;

import com.easylive.annotation.RecordUserMessage;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.enums.CommentTopTypeEnum;
import com.easylive.entity.enums.MessageTypeEnum;
import com.easylive.entity.enums.PageSize;
import com.easylive.entity.enums.UserActionTypeEnum;
import com.easylive.entity.po.UserAction;
import com.easylive.entity.po.VideoComment;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.query.UserActionQuery;
import com.easylive.entity.query.VideoCommentQuery;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.entity.vo.VideoCommentResultVO;
import com.easylive.service.UserActionService;
import com.easylive.service.VideoCommentService;
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
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Validated
@RequestMapping("/comment")
@Slf4j
public class VideoCommentController extends ABaseController {

    private static final Logger logger = LoggerFactory.getLogger(VideoCommentController.class);

    @Resource
    private VideoCommentService videoCommentService;

    @Resource
    private UserActionService userActionService;

    @Resource
    private VideoInfoServiceImpl videoInfoService;

    /**
     * 处理请求路径为/comment/loadComment的HTTP GET请求，用于加载视频评论。
     *
     * @param videoId   视频ID，不能为空
     * @param pageNo    页码
     * @param orderType 排序类型，0表示按点赞数和评论ID倒序，其他值表示按评论ID倒序
     * @return 包含视频评论数据和用户对评论的操作信息的成功响应对象
     */
    @RequestMapping("/loadComment")
    @GlobalInterceptor
    public ResponseVO loadComment(@NotEmpty String videoId, Integer pageNo, Integer orderType) {

        VideoInfo videoInfo = videoInfoService.getVideoInfoByVideoId(videoId);
        if (videoInfo.getInteraction() != null && videoInfo.getInteraction().contains(Constants.ONE.toString())) {
            return getSuccessResponseVO(new ArrayList<>());
        }

        VideoCommentQuery commentQuery = new VideoCommentQuery();
        commentQuery.setVideoId(videoId);
        commentQuery.setLoadChildren(true);
        commentQuery.setPageNo(pageNo);
        commentQuery.setPageSize(PageSize.SIZE15.getSize());
        commentQuery.setpCommentId(0);
        String orderBy = orderType == null || orderType == 0 ? "like_count desc,comment_id desc" : "comment_id desc";
        commentQuery.setOrderBy(orderBy);
        PaginationResultVO<VideoComment> commentData = videoCommentService.findListByPage(commentQuery);

        //置顶评论
        if (pageNo == null || pageNo == 1) {
            List<VideoComment> topCommentList = topComment(videoId);
            if (!topCommentList.isEmpty()) {
                List<VideoComment> commentList =
                        commentData.getList().stream().filter(item -> !item.getCommentId().equals(topCommentList.get(0).getCommentId())).collect(Collectors.toList());
                commentList.addAll(0, topCommentList);
                commentData.setList(commentList);
            }
        }

        VideoCommentResultVO resultVO = new VideoCommentResultVO();
        resultVO.setCommentData(commentData);
        List<UserAction> userActionList = new ArrayList<>();
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        if (tokenUserInfoDto != null) {
            UserActionQuery userActionQuery = new UserActionQuery();
            userActionQuery.setUserId(tokenUserInfoDto.getUserId());
            userActionQuery.setVideoId(videoId);
            userActionQuery.setActionTypeArray(new Integer[]{UserActionTypeEnum.COMMENT_LIKE.getType(), UserActionTypeEnum.COMMENT_HATE.getType()});
            userActionList = userActionService.findListByParam(userActionQuery);
        }
        resultVO.setUserActionList(userActionList);
        return getSuccessResponseVO(resultVO);
    }

    /**
     * 获取置顶评论列表
     *
     * @param videoId 视频ID
     * @return 置顶评论列表
     */
    private List<VideoComment> topComment(String videoId) {
        VideoCommentQuery commentQuery = new VideoCommentQuery();
        commentQuery.setVideoId(videoId);
        commentQuery.setTopType(CommentTopTypeEnum.TOP.getType());
        commentQuery.setLoadChildren(true);
        List<VideoComment> videoCommentList = videoCommentService.findListByParam(commentQuery);
        return videoCommentList;
    }

    /**
     * 处理请求路径为/comment/postComment的HTTP POST请求，用于发布评论。
     *
     * @param videoId        视频ID，不能为空
     * @param replyCommentId 回复的评论ID
     * @param content        评论内容，不能为空，最大长度为500
     * @param imgPath        评论图片路径，最大长度为50
     * @return 发布成功的评论对象
     */
    @RequestMapping("/postComment")
    @GlobalInterceptor(checkLogin = true)
    @RecordUserMessage(messageType = MessageTypeEnum.COMMENT)
    public ResponseVO postComment(@NotEmpty String videoId,
                                  Integer replyCommentId,
                                  @NotEmpty @Size(max = 500) String content,
                                  @Size(max = 50) String imgPath) {

        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        VideoComment comment = new VideoComment();
        comment.setUserId(tokenUserInfoDto.getUserId());
        comment.setAvatar(tokenUserInfoDto.getAvatar());
        comment.setNickName(tokenUserInfoDto.getNickName());
        comment.setVideoId(videoId);
        comment.setContent(content);
        comment.setImgPath(imgPath);
        videoCommentService.postComment(comment, replyCommentId);
        return getSuccessResponseVO(comment);
    }


    /**
     * 处理请求路径为/comment/userDelComment的HTTP POST请求，用于用户删除评论。
     *
     * @param commentId 评论ID，不能为空
     * @return 操作结果
     */
    @RequestMapping("/userDelComment")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO userDelComment(@NotNull Integer commentId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        VideoComment comment = new VideoComment();
        videoCommentService.deleteComment(commentId, tokenUserInfoDto.getUserId());
        return getSuccessResponseVO(comment);
    }

    /**
     * 处理请求路径为/comment/topComment的HTTP POST请求，用于置顶评论。
     *
     * @param commentId 评论ID，不能为空
     * @return 操作结果
     */
    @RequestMapping("/topComment")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO topComment(@NotNull Integer commentId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        videoCommentService.topComment(commentId, tokenUserInfoDto.getUserId());
        return getSuccessResponseVO(null);
    }

    /**
     * 处理请求路径为/comment/cancelTopComment的HTTP POST请求，用于取消置顶评论。
     *
     * @param commentId 评论ID，不能为空
     * @return 操作结果
     */
    @RequestMapping("/cancelTopComment")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO cancelTopComment(@NotNull Integer commentId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        videoCommentService.cancelTopComment(commentId, tokenUserInfoDto.getUserId());
        return getSuccessResponseVO(null);
    }
}