package com.easylive.admin.controller;

import com.easylive.entity.query.VideoCommentQuery;
import com.easylive.entity.query.VideoDanmuQuery;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.service.VideoCommentService;
import com.easylive.service.VideoDanmuService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

@RestController
@RequestMapping("/interact")
@Validated
public class InteractController extends ABaseController {
    @Resource
    private VideoCommentService videoCommentService;

    @Resource
    private VideoDanmuService videoDanmuService;


    /**
     * 弹幕列表
     *
     * @param pageNo
     * @param videoNameFuzzy
     * @return
     */
    @RequestMapping("/loadDanmu")
    public ResponseVO loadDanmu(Integer pageNo, String videoNameFuzzy) {
        VideoDanmuQuery danmuQuery = new VideoDanmuQuery();
        danmuQuery.setOrderBy("danmu_id desc");
        danmuQuery.setPageNo(pageNo);
        danmuQuery.setQueryVideoInfo(true);
        danmuQuery.setVideoNameFuzzy(videoNameFuzzy);
        PaginationResultVO resultVO = videoDanmuService.findListByPage(danmuQuery);
        return getSuccessResponseVO(resultVO);
    }


    /**
     * 删除弹幕
     *
     * @param danmuId
     * @return
     */

    @RequestMapping("/delDanmu")
    public ResponseVO delDanmu(@NotNull Integer danmuId) {
        videoDanmuService.deleteDanmu(null, danmuId);
        return getSuccessResponseVO(null);
    }


    /**
     * 评论列表
     *
     * @param pageNo
     * @param videoNameFuzzy
     * @return
     */
    @RequestMapping("/loadComment")
    public ResponseVO loadComment(Integer pageNo, String videoNameFuzzy) {
        VideoCommentQuery commentQuery = new VideoCommentQuery();
        commentQuery.setOrderBy("comment_id desc");
        commentQuery.setPageNo(pageNo);
        commentQuery.setQueryVideoInfo(true);
        commentQuery.setVideoNameFuzzy(videoNameFuzzy);
        PaginationResultVO resultVO = videoCommentService.findListByPage(commentQuery);
        return getSuccessResponseVO(resultVO);
    }


    /**
     * 删除评论
     *
     * @param commentId
     * @return
     */
    @RequestMapping("/delComment")
    public ResponseVO delComment(@NotNull Integer commentId) {
        videoCommentService.deleteComment(commentId, null);
        return getSuccessResponseVO(null);
    }
}
