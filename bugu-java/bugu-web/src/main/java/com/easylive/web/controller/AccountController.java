package com.easylive.web.controller;

import com.easylive.component.RedisComponent;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.dto.UserCountInfoDto;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.exception.BusinessException;
import com.easylive.service.UserInfoService;
import com.easylive.utils.StringTools;
import com.easylive.web.annotation.GlobalInterceptor;
import com.wf.captcha.ArithmeticCaptcha;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;

@RestController("accountController")
@RequestMapping("/account")
@Validated
public class AccountController extends ABaseController {

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private RedisComponent redisComponent;

    /**
     * 验证码相关操作
     */
    @RequestMapping(value = "/checkCode")
    @GlobalInterceptor
    public ResponseVO checkCode() {
        // 创建一个算术验证码，宽度100，高度42
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(100, 42);

        // 获取生成的验证码文本
        String code = captcha.text();

        // 将验证码存储在Redis中，并获取对应的key
        String checkCodeKey = redisComponent.saveCheckCode(code);

        // 将验证码转换为Base64字符串以便于前端显示
        String checkCodeBase64 = captcha.toBase64();

        // 准备返回结果
        Map<String, String> result = new HashMap<>();
        result.put("checkCode", checkCodeBase64);
        result.put("checkCodeKey", checkCodeKey);

        // 返回成功响应
        return getSuccessResponseVO(result);
    }

    /**
     * 用户注册
     */
    @RequestMapping(value = "/register")
    @GlobalInterceptor
    public ResponseVO register(
            @NotEmpty @Email @Size(max = 150) String email,
            @NotEmpty @Size(max = 20) String nickName,
            @NotEmpty @Pattern(regexp = Constants.REGEX_PASSWORD) String registerPassword,
            @NotEmpty String checkCodeKey,
            @NotEmpty String checkCode) {

        try {
            // 验证用户输入的验证码是否正确
            if (!checkCode.equalsIgnoreCase(redisComponent.getCheckCode(checkCodeKey))) {
                throw new BusinessException("图片验证码不正确");
            }

            // 调用服务进行用户注册
            userInfoService.register(email, nickName, registerPassword);

            // 返回成功响应
            return getSuccessResponseVO(null);
        } finally {
            // 清除验证码
            redisComponent.cleanCheckCode(checkCodeKey);
        }
    }

    /**
     * 用户登录
     */
    @RequestMapping(value = "/login")
    @GlobalInterceptor
    public ResponseVO login(
            HttpServletRequest request,
            HttpServletResponse response,
            @NotEmpty @Email String email,
            @NotEmpty String password,
            @NotEmpty String checkCodeKey,
            @NotEmpty String checkCode) {

        try {
            // 验证用户输入的验证码是否正确
            if (!checkCode.equalsIgnoreCase(redisComponent.getCheckCode(checkCodeKey))) {
                throw new BusinessException("图片验证码不正确");
            }

            // 获取用户IP地址
            String ip = getIpAddr();

            // 调用服务进行用户登录
            TokenUserInfoDto tokenUserInfoDto = userInfoService.login(email, password, ip);

            // 将token保存到cookie中
            saveToken2Cookie(response, tokenUserInfoDto.getToken());

            // 返回成功响应
            return getSuccessResponseVO(tokenUserInfoDto);
        } finally {
            // 清除验证码
            redisComponent.cleanCheckCode(checkCodeKey);

            // 清除旧的token
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                String token = null;
                for (Cookie cookie : cookies) {
                    if (Constants.TOKEN_WEB.equals(cookie.getName())) {
                        token = cookie.getValue();
                    }
                }
                if (!StringTools.isEmpty(token)) {
                    redisComponent.cleanToken(token);
                }
            }
        }
    }

    /**
     * 自动登录
     */
    @RequestMapping(value = "/autoLogin")
    @GlobalInterceptor
    public ResponseVO autoLogin(HttpServletResponse response) {
        // 获取当前用户的token信息
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();

        // 如果用户未登录，返回null
        if (tokenUserInfoDto == null) {
            return getSuccessResponseVO(null);
        }

        // 检查token是否快过期，如果快过期则更新token
        if (tokenUserInfoDto.getExpireAt() - System.currentTimeMillis() < Constants.REDIS_KEY_EXPIRES_DAY) {
            redisComponent.saveTokenInfo(tokenUserInfoDto);
            saveToken2Cookie(response, tokenUserInfoDto.getToken());
        }

        // 返回成功响应
        return getSuccessResponseVO(tokenUserInfoDto);
    }

    /**
     * 用户登出
     */
    @RequestMapping(value = "/logout")
    @GlobalInterceptor
    public ResponseVO logout(HttpServletResponse response) {
        // 清除cookie
        cleanCookie(response);

        // 返回成功响应
        return getSuccessResponseVO(null);
    }

    /**
     * 获取用户统计信息
     */
    @RequestMapping(value = "/getUserCountInfo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO getUserCountInfo() {
        // 获取当前用户的token信息
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();

        // 获取用户统计信息
        UserCountInfoDto userCountInfoDto = userInfoService.getUserCountInfo(tokenUserInfoDto.getUserId());

        // 返回成功响应
        return getSuccessResponseVO(userCountInfoDto);
    }

}