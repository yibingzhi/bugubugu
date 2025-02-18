package com.easylive.web.controller;

import com.easylive.entity.constants.Constants;
import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.enums.PageSize;
import com.easylive.entity.enums.UserActionTypeEnum;
import com.easylive.entity.enums.VideoOrderTypeEnum;
import com.easylive.entity.po.UserInfo;
import com.easylive.entity.query.UserActionQuery;
import com.easylive.entity.query.UserFocusQuery;
import com.easylive.entity.query.VideoInfoQuery;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.entity.vo.UserInfoVO;
import com.easylive.service.UserActionService;
import com.easylive.service.UserFocusService;
import com.easylive.service.UserInfoService;
import com.easylive.service.VideoInfoService;
import com.easylive.utils.CopyTools;
import com.easylive.web.annotation.GlobalInterceptor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@RestController
@Validated
@RequestMapping("/uhome")
public class UHomeController extends ABaseController {

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private VideoInfoService videoInfoService;

    @Resource
    private UserFocusService userFocusService;

    @Resource
    private UserActionService userActionService;

    /**
     * @Description: 获取用户信息
     * @param: [userId] - 用户ID，不能为空
     * @return: com.easylive.entity.vo.ResponseVO - 返回包含用户信息的响应对象
     */
    @RequestMapping("/getUserInfo")
    @GlobalInterceptor // 使用全局拦截器
    public ResponseVO getUserInfo(@NotEmpty String userId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto(); // 获取当前用户信息
        // 查询用户详细信息
        UserInfo userInfo = userInfoService.getUserDetailInfo(null == tokenUserInfoDto ? null : tokenUserInfoDto.getUserId(), userId);
        // 将用户信息复制到用户信息视图对象
        UserInfoVO userInfoVO = CopyTools.copy(userInfo, UserInfoVO.class);
        return getSuccessResponseVO(userInfoVO); // 返回成功响应
    }

    /**
     * @Description: 更新用户信息
     * @param: [nickName] - 昵称，不能为空且最大长度为20
     * @param: [avatar] - 头像URL，不能为空且最大长度为100
     * @param: [sex] - 性别，不能为空
     * @param: [birthday] - 生日，可为空
     * @param: [school] - 学校，最大长度为150
     * @param: [personIntroduction] - 个人介绍，最大长度为80
     * @param: [noticeInfo] - 通知信息，最大长度为300
     * @return: com.easylive.entity.vo.ResponseVO - 返回操作结果的响应对象
     */
    @RequestMapping("/updateUserInfo")
    @GlobalInterceptor(checkLogin = true) // 检查用户登录状态
    public ResponseVO updateUserInfo(@NotEmpty @Size(max = 20) String nickName,
                                     @NotEmpty @Size(max = 100) String avatar,
                                     @NotNull Integer sex,
                                     String birthday,
                                     @Size(max = 150) String school,
                                     @Size(max = 80) String personIntroduction,
                                     @Size(max = 300) String noticeInfo) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto(); // 获取当前用户信息
        UserInfo userInfo = new UserInfo(); // 创建用户信息对象
        userInfo.setUserId(tokenUserInfoDto.getUserId()); // 设置用户ID
        userInfo.setNickName(nickName); // 设置昵称
        userInfo.setAvatar(avatar); // 设置头像
        userInfo.setSex(sex); // 设置性别
        userInfo.setBirthday(birthday); // 设置生日
        userInfo.setSchool(school); // 设置学校
        userInfo.setPersonIntroduction(personIntroduction); // 设置个人介绍
        userInfo.setNoticeInfo(noticeInfo); // 设置通知信息
        userInfoService.updateUserInfo(userInfo, tokenUserInfoDto); // 更新用户信息

        return getSuccessResponseVO(null); // 返回成功响应
    }

    /**
     * @Description: 保存用户主题设置
     * @param: [theme] - 主题值
     * @return: com.easylive.entity.vo.ResponseVO - 返回操作结果的响应对象
     */
    @RequestMapping("/saveTheme")
    @GlobalInterceptor // 使用全局拦截器
    public ResponseVO saveTheme(Integer theme) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto(); // 获取当前用户信息
        UserInfo userInfo = new UserInfo(); // 创建用户信息对象
        userInfo.setTheme(theme); // 设置主题
        userInfoService.updateUserInfoByUserId(userInfo, tokenUserInfoDto.getUserId()); // 更新用户主题

        return getSuccessResponseVO(null); // 返回成功响应
    }

    /**
     * @Description: 关注用户
     * @param: [focusUserId] - 被关注用户ID，不能为空
     * @return: com.easylive.entity.vo.ResponseVO - 返回操作结果的响应对象
     */
    @RequestMapping("/focus")
    @GlobalInterceptor(checkLogin = true) // 检查用户登录状态
    public ResponseVO focus(@NotEmpty String focusUserId) {
        userFocusService.focusUser(getTokenUserInfoDto().getUserId(), focusUserId); // 关注用户
        return getSuccessResponseVO(null); // 返回成功响应
    }

    /**
     * @Description: 取消关注用户
     * @param: [focusUserId] - 被取消关注用户ID，不能为空
     * @return: com.easylive.entity.vo.ResponseVO - 返回操作结果的响应对象
     */
    @RequestMapping("/cancelFocus")
    @GlobalInterceptor(checkLogin = true) // 检查用户登录状态
    public ResponseVO cancelFocus(@NotEmpty String focusUserId) {
        userFocusService.cancelFocus(getTokenUserInfoDto().getUserId(), focusUserId); // 取消关注用户
        return getSuccessResponseVO(null); // 返回成功响应
    }

    /**
     * @Description: 加载用户关注列表
     * @param: [pageNo] - 当前页面编号
     * @return: com.easylive.entity.vo.ResponseVO - 返回包含关注列表的分页结果
     */
    @RequestMapping("/loadFocusList")
    @GlobalInterceptor(checkLogin = true) // 检查用户登录状态
    public ResponseVO loadFocusList(Integer pageNo) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto(); // 获取当前用户信息
        UserFocusQuery focusQuery = new UserFocusQuery(); // 创建用户关注查询对象
        focusQuery.setUserId(tokenUserInfoDto.getUserId()); // 设置用户ID
        focusQuery.setQueryType(Constants.ZERO); // 设置查询类型（关注）
        focusQuery.setPageNo(pageNo); // 设置当前页面编号
        focusQuery.setOrderBy("focus_time desc"); // 设置排序方式为关注时间降序
        PaginationResultVO resultVO = userFocusService.findListByPage(focusQuery); // 获取关注列表的分页结果
        return getSuccessResponseVO(resultVO); // 返回成功响应
    }

    /**
     * @Description: 加载用户粉丝列表
     * @param: [pageNo] - 当前页面编号
     * @return: com.easylive.entity.vo.ResponseVO - 返回包含粉丝列表的分页结果
     */
    @RequestMapping("/loadFansList")
    @GlobalInterceptor(checkLogin = true) // 检查用户登录状态
    public ResponseVO loadFansList(Integer pageNo) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto(); // 获取当前用户信息
        UserFocusQuery focusQuery = new UserFocusQuery(); // 创建用户关注查询对象
        focusQuery.setFocusUserId(tokenUserInfoDto.getUserId()); // 设置关注用户ID
        focusQuery.setQueryType(Constants.ONE); // 设置查询类型（粉丝）
        focusQuery.setPageNo(pageNo); // 设置当前页面编号
        focusQuery.setOrderBy("focus_time desc"); // 设置排序方式为关注时间降序
        PaginationResultVO resultVO = userFocusService.findListByPage(focusQuery); // 获取粉丝列表的分页结果
        return getSuccessResponseVO(resultVO); // 返回成功响应
    }

    /**
     * @Description: 加载用户视频列表
     * @param: [userId] - 用户ID，不能为空
     * @param: [type] - 视频类型，可为空
     * @param: [pageNo] - 当前页面编号
     * @param: [videoName] - 视频名称，可为空
     * @param: [orderType] - 排序类型，可为空
     * @return: com.easylive.entity.vo.ResponseVO - 返回包含视频列表的分页结果
     */
    @RequestMapping("/loadVideoList")
    @GlobalInterceptor // 使用全局拦截器
    public ResponseVO loadVideoList(@NotEmpty String userId, Integer type, Integer pageNo, String videoName, Integer orderType) {
        VideoInfoQuery infoQuery = new VideoInfoQuery(); // 创建视频信息查询对象
        if (type != null) {
            infoQuery.setPageSize(PageSize.SIZE10.getSize()); // 设置页面大小
        }
        VideoOrderTypeEnum videoOrderTypeEnum = VideoOrderTypeEnum.getByType(orderType); // 获取排序类型
        if (videoOrderTypeEnum == null) {
            videoOrderTypeEnum = VideoOrderTypeEnum.CREATE_TIME; // 默认排序方式为创建时间
        }
        infoQuery.setOrderBy(videoOrderTypeEnum.getField() + " desc"); // 设置排序字段
        infoQuery.setVideoNameFuzzy(videoName); // 设置模糊查询视频名称
        infoQuery.setPageNo(pageNo); // 设置当前页面编号
        infoQuery.setUserId(userId); // 设置用户ID
        PaginationResultVO resultVO = videoInfoService.findListByPage(infoQuery); // 获取视频列表的分页结果
        return getSuccessResponseVO(resultVO); // 返回成功响应
    }

    /**
     * @Description: 加载用户收藏的视频
     * @param: [userId] - 用户ID，不能为空
     * @param: [pageNo] - 当前页面编号
     * @return: com.easylive.entity.vo.ResponseVO - 返回包含用户收藏视频的分页结果
     */
    @RequestMapping("/loadUserCollection")
    @GlobalInterceptor // 使用全局拦截器
    public ResponseVO loadUserCollection(@NotEmpty String userId, Integer pageNo) {
        UserActionQuery actionQuery = new UserActionQuery(); // 创建用户行为查询对象
        actionQuery.setActionType(UserActionTypeEnum.VIDEO_COLLECT.getType()); // 设置行为类型为视频收藏
        actionQuery.setUserId(userId); // 设置用户ID
        actionQuery.setPageNo(pageNo); // 设置当前页面编号
        actionQuery.setQueryVideoInfo(true); // 查询视频信息
        actionQuery.setOrderBy("action_time desc"); // 设置排序方式为行为时间降序
        PaginationResultVO resultVO = userActionService.findListByPage(actionQuery); // 获取收藏视频的分页结果
        return getSuccessResponseVO(resultVO); // 返回成功响应
    }


}
