package com.easylive.web.controller;

import com.easylive.component.RedisComponent;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.web.annotation.GlobalInterceptor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 系统设置控制器类，用于处理与系统设置相关的HTTP请求。
 */
@RestController("sysSettingController")
@RequestMapping("/sysSetting")
@Validated
public class SysSettingController extends ABaseController {

    @Resource
    private RedisComponent redisComponent;

    /**
     * 处理请求路径为/sysSetting/getSetting的HTTP GET请求，用于获取系统设置。
     *
     * @return 包含系统设置数据的成功响应对象
     */
    @RequestMapping(value = "/getSetting")
    @GlobalInterceptor
    public ResponseVO getSetting() {
        return getSuccessResponseVO(redisComponent.getSysSettingDto());
    }
}