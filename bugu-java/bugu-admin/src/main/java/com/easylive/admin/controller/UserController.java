package com.easylive.admin.controller;

import com.easylive.entity.po.UserInfo;
import com.easylive.entity.query.UserInfoQuery;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.service.UserInfoService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/user")
@Validated
public class UserController extends ABaseController {
    @Resource
    private UserInfoService userInfoService;


    /**
     * 加载用户列表
     *
     * @param userInfoQuery
     * @return
     */
    @RequestMapping("/loadUser")
    public ResponseVO loadUser(UserInfoQuery userInfoQuery) {
        userInfoQuery.setOrderBy("join_time desc");
        PaginationResultVO resultVO = userInfoService.findListByPage(userInfoQuery);
        return getSuccessResponseVO(resultVO);
    }


    /**
     * 修改用户状态
     *
     * @param userId
     * @param status
     * @return
     */
    @RequestMapping("/changeStatus")
    public ResponseVO changeStatus(String userId, Integer status) {
        UserInfo userInfo = new UserInfo();
        userInfo.setStatus(status);
        userInfoService.updateUserInfoByUserId(userInfo, userId);
        return getSuccessResponseVO(null);
    }
}
