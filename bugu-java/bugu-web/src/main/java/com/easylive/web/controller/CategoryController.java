package com.easylive.web.controller;

import com.easylive.entity.po.CategoryInfo;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.service.CategoryInfoService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@Validated
@RequestMapping("/category")
public class CategoryController extends ABaseController {

    @Resource
    private CategoryInfoService categoryInfoService;

    // 定义一个控制器方法，用于处理请求路径为/loadAllCategory的HTTP请求
    @RequestMapping("/loadAllCategory")
    public ResponseVO loadAllCategory() {
        // 调用服务层的方法获取所有分类信息的列表
        List<CategoryInfo> categoryInfoList = categoryInfoService.getAllCategoryList();

        // 将获取到的分类信息列表封装到成功的响应对象中并返回
        return getSuccessResponseVO(categoryInfoList);
    }

}
