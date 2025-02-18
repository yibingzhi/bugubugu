package com.easylive.web.aspect;

import com.easylive.entity.constants.Constants;
import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.enums.ResponseCodeEnum;
import com.easylive.exception.BusinessException;
import com.easylive.redis.RedisUtils;
import com.easylive.utils.StringTools;
import com.easylive.web.annotation.GlobalInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

@Component("operationAspect")
@Aspect
@Slf4j
public class GlobalOperationAspect {

    @Resource
    private RedisUtils redisUtils;

    @Before("@annotation(com.easylive.web.annotation.GlobalInterceptor)")
    public void interceptorDo(JoinPoint point) {
        Method method = ((MethodSignature) point.getSignature()).getMethod();
        GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class);
        if (null == interceptor) {
            return;
        }
        /**
         * 校验登录
         */
        if (interceptor.checkLogin()) {
            checkLogin();
        }
    }

    //校验登录
    private void checkLogin() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String token = request.getHeader(Constants.TOKEN_WEB);
        if (StringTools.isEmpty(token)) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        TokenUserInfoDto tokenUserInfoDto = (TokenUserInfoDto) redisUtils.get(Constants.REDIS_KEY_TOKEN_WEB + token);
        if (tokenUserInfoDto == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
    }
}