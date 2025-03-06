package com.easylive.web.controller;

import com.easylive.entity.po.CategoryInfo;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.service.CategoryInfoService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 分类控制器类，用于处理与分类相关的HTTP请求。
 */
@RestController
@Validated
@RequestMapping("/category")
public class CategoryController extends ABaseController {

    @Resource
    private CategoryInfoService categoryInfoService;

    /**
     * 处理请求路径为/loadAllCategory的HTTP GET请求，用于加载所有分类信息。
     *
     * @return 包含所有分类信息列表的成功响应对象
     */
    @RequestMapping("/loadAllCategory")
    public ResponseVO loadAllCategory() {
        // 调用服务层的方法获取所有分类信息的列表
        List<CategoryInfo> categoryInfoList = categoryInfoService.getAllCategoryList();

        // 将获取到的分类信息列表封装到成功的响应对象中并返回
        return getSuccessResponseVO(categoryInfoList);
    }
}