package com.easylive.admin.controller;

import com.easylive.entity.config.AppConfig;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.enums.DateTimePatternEnum;
import com.easylive.entity.enums.ResponseCodeEnum;
import com.easylive.entity.po.VideoInfoFilePost;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.exception.BusinessException;
import com.easylive.service.VideoInfoFilePostService;
import com.easylive.utils.DateUtil;
import com.easylive.utils.FFmpegUtils;
import com.easylive.utils.StringTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

@Validated // 用于启用 JSR 380（Hibernate Validator）验证，对方法参数和请求体进行校验
@Slf4j // 引入日志记录功能，方便在代码中输出日志信息
@RestController // 标识这是一个 RESTful Web 服务的控制器，用于处理 HTTP 请求并返回响应体
@RequestMapping("/file") // 指定该控制器处理的请求路径前缀为 /file
public class FileController extends ABaseController {

    @Resource // 依赖注入，将 AppConfig 实例注入到当前类的 appConfig 字段中
    private AppConfig appConfig;

    @Resource // 依赖注入，将 FFmpegUtils 实例注入到当前类的 fFmpegUtils 字段中
    private FFmpegUtils fFmpegUtils;

    @Resource // 依赖注入，将 VideoInfoFilePostService 实例注入到当前类的 videoInfoFilePostService 字段中
    private VideoInfoFilePostService videoInfoFilePostService;


    /**
     * 上传封面图片
     *
     * @param file            上传的文件
     * @param createThumbnail 是否创建缩略图
     * @return 包含封面文件路径的成功响应
     * @throws IOException 文件保存过程中发生异常时抛出
     */
    @RequestMapping("/uploadImage") // 处理 POST 请求，路径为 /file/uploadImage
    public ResponseVO uploadCover(@NotNull MultipartFile file, @NotNull Boolean createThumbnail) throws IOException {
        // 获取当前日期并格式化为 YYYYMM 格式的字符串
        String month = DateUtil.format(new Date(), DateTimePatternEnum.YYYYMM.getPattern());
        // 构建封面文件的存储路径，包括项目文件夹、文件文件夹、封面文件夹和月份
        String folder = appConfig.getProjectFolder() + Constants.FILE_FOLDER + Constants.FILE_COVER + month;
        // 创建存储封面文件的文件夹对象，如果文件夹不存在则创建
        File folderFile = new File(folder);
        if (!folderFile.exists()) {
            folderFile.mkdirs();
        }
        // 获取上传文件的原始文件名
        String fileName = file.getOriginalFilename();
        // 获取文件的后缀名
        String fileSuffix = fileName.substring(fileName.lastIndexOf("."));
        // 生成随机的文件名，长度为 30 并加上原文件后缀
        String realFileName = StringTools.getRandomString(Constants.LENGTH_30) + fileSuffix;
        // 构建完整的封面文件存储路径
        String filePath = folder + "/" + realFileName;
        // 将上传的文件保存到指定路径
        file.transferTo(new File(filePath));
        if (createThumbnail) {
            // 如果需要创建缩略图，则调用 FFmpegUtils 的 createImageThumbnail 方法生成缩略图
            fFmpegUtils.createImageThumbnail(filePath);
        }
        // 返回包含封面文件路径的成功响应
        return getSuccessResponseVO(Constants.FILE_COVER + month + "/" + realFileName);
    }

    /**
     * 获取资源文件
     *
     * @param response   HTTP 响应对象
     * @param sourceName 资源文件名
     */
    @RequestMapping("/getResource") // 处理 GET 请求，路径为 /file/getResource
    public void getResource(HttpServletResponse response, @NotEmpty String sourceName) {
        if (!StringTools.pathIsOk(sourceName)) {
            // 如果源文件名路径不合法，则抛出业务异常
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        // 获取文件的后缀名
        String suffix = StringTools.getFileSuffix(sourceName);
        // 设置响应的 Content-Type 为 image/加上后缀名（去掉开头的点）
        response.setContentType("image/" + suffix.replace(".", ""));
        // 设置缓存控制头，max-age=2592000 表示缓存 30 天
        response.setHeader("Cache-Control", "max-age=2592000");
        // 调用 readFile 方法读取并输出文件内容到响应中
        readFile(response, sourceName);
    }

    /**
     * 读取文件内容并输出到响应中
     *
     * @param response HTTP 响应对象
     * @param filePath 文件路径
     */

    protected void readFile(HttpServletResponse response, String filePath) {
        File file = new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER + filePath);
        if (!file.exists()) {
            // 如果文件不存在，则直接返回
            return;
        }
        try (OutputStream out = response.getOutputStream(); FileInputStream in = new FileInputStream(file)) {
            byte[] byteData = new byte[1024];
            int len = 0;
            while ((len = in.read(byteData)) != -1) {
                // 从输入流中读取数据到字节数组，并将数据写入输出流
                out.write(byteData, 0, len);
            }
            out.flush();
        } catch (Exception e) {
            // 捕获异常并记录日志
            log.error("读取文件异常", e);
        }
    }


    /**
     * 获取视频资源文件
     *
     * @param response HTTP 响应对象
     * @param fileId   文件 ID
     */
    @RequestMapping("/videoResource/{fileId}") // 处理 GET 请求，路径为 /file/videoResource/{fileId}
    public void getVideoResource(HttpServletResponse response, @PathVariable @NotEmpty String fileId) {
        // 根据 fileId 获取 VideoInfoFilePost 对象
        VideoInfoFilePost videoInfoFilePost = videoInfoFilePostService.getVideoInfoFilePostByFileId(fileId);
        // 获取视频文件的路径
        String filePath = videoInfoFilePost.getFilePath();
        // 调用 readFile 方法读取并输出视频资源文件的内容到响应中
        readFile(response, filePath + "/" + Constants.M3U8_NAME);
    }

    /**
     * 获取视频资源文件的 TS 文件
     *
     * @param response HTTP 响应对象
     * @param fileId   文件 ID
     * @param ts       TS 文件名
     */

    @RequestMapping("/videoResource/{fileId}/{ts}") // 处理 GET 请求，路径为 /file/videoResource/{fileId}/{ts}
    public void getVideoResourceTs(HttpServletResponse response, @PathVariable @NotEmpty String fileId, @PathVariable @NotNull String ts) {
        // 根据 fileId 获取 VideoInfoFilePost 对象
        VideoInfoFilePost videoInfoFilePost = videoInfoFilePostService.getVideoInfoFilePostByFileId(fileId);
        // 获取视频文件的路径
        String filePath = videoInfoFilePost.getFilePath() + "";
        // 调用 readFile 方法读取并输出视频资源文件的内容到响应中
        readFile(response, filePath + "/" + ts);
    }
}