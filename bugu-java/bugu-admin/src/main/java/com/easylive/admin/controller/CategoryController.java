package com.easylive.admin.controller;

import com.easylive.entity.po.CategoryInfo;
import com.easylive.entity.query.CategoryInfoQuery;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.service.CategoryInfoService;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/category")
@Validated
public class CategoryController extends ABaseController {

    private static final Logger log = LoggerFactory.getLogger(CategoryController.class);

    // 注入 CategoryInfoService 用于操作分类信息相关的业务逻辑
    @Resource
    private CategoryInfoService categoryInfoService;

    /**
     * 处理请求路径为 /category/loadCategory 的 HTTP GET 请求，用于加载所有分类
     *
     * @param categoryInfo 用于查询分类的查询条件
     */
    @RequestMapping("/loadCategory")
    public ResponseVO loadAllCategory(CategoryInfoQuery categoryInfo) {
        // 设置排序字段为 "sort asc"，表示按 sort 字段升序排列
        categoryInfo.setOrderBy("sort asc");
        // 设置转换为树形结构的标志为 true，用于将分类信息转换为树形结构以便展示
        categoryInfo.setConvert2Tree(true);
        // 调用 categoryInfoService 的 findListByParam 方法，根据传入的 categoryInfo 查询条件获取分类信息列表
        List<CategoryInfo> categoryInfoList = categoryInfoService.findListByParam(categoryInfo);
        // 返回包含分类信息列表的成功响应对象
        return getSuccessResponseVO(categoryInfoList);
    }

    /**
     * 处理请求路径为 /category/saveCategory 的 HTTP POST 请求，用于保存分类信息
     *
     * @param pCategoryId
     * @param pCategoryId
     * @param categoryId
     * @param categoryCode
     * @param categoryName
     * @param icon
     * @param background
     * @return
     */
    @RequestMapping("/saveCategory")
    public ResponseVO saveCategory(@NotNull Integer pCategoryId,
                                   Integer categoryId,
                                   @NotEmpty String categoryCode,
                                   @NotEmpty String categoryName,
                                   String icon,
                                   String background) {
        // 创建一个 CategoryInfo 对象，用于封装要保存的分类信息
        CategoryInfo categoryInfo = new CategoryInfo();
        categoryInfo.setpCategoryId(pCategoryId);
        categoryInfo.setCategoryId(categoryId);
        categoryInfo.setCategoryCode(categoryCode);
        categoryInfo.setCategoryName(categoryName);
        categoryInfo.setIcon(icon);
        categoryInfo.setBackground(background);
        // 调用 categoryInfoService 的 saveCategoryInfo 方法保存分类信息
        categoryInfoService.saveCategoryInfo(categoryInfo);
        // 返回包含空对象的成功响应对象，表示保存操作成功
        return getSuccessResponseVO(null);
    }


    /**
     * 处理请求路径为 /category/delCategory 的 HTTP POST 请求，用于删除指定分类
     *
     * @param categoryId
     * @return
     */
    @RequestMapping("/delCategory")
    public ResponseVO delCategory(@NotNull Integer categoryId) {
        // 调用 categoryInfoService 的 delCategory 方法删除指定分类
        categoryInfoService.delCategory(categoryId);
        // 返回包含空对象的成功响应对象，表示删除操作成功
        return getSuccessResponseVO(null);
    }

    /**
     * 处理请求路径为 /category/changeSort 的 HTTP POST 请求，用于更改分类排序
     *
     * @param pCategoryId
     * @param categoryIds
     * @return
     */
    @RequestMapping("/changeSort")
    public ResponseVO changeSort(@NotNull Integer pCategoryId,
                                 @NotEmpty String categoryIds) {
        // 调用 categoryInfoService 的 changeSort 方法更改分类排序
        categoryInfoService.changeSort(pCategoryId, categoryIds);
        // 返回包含空对象的成功响应对象，表示排序更改操作成功
        return getSuccessResponseVO(null);
    }
}