package com.easylive.web.controller;

import com.easylive.component.RedisComponent;
import com.easylive.entity.config.AppConfig;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.dto.SysSettingDto;
import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.dto.UploadingFileDto;
import com.easylive.entity.dto.VideoPlayInfoDto;
import com.easylive.entity.enums.DateTimePatternEnum;
import com.easylive.entity.enums.FileTypeEnum;
import com.easylive.entity.enums.ResponseCodeEnum;
import com.easylive.entity.po.VideoInfoFile;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.exception.BusinessException;
import com.easylive.service.VideoInfoFileService;
import com.easylive.utils.DateUtil;
import com.easylive.utils.FFmpegUtils;
import com.easylive.utils.StringTools;
import com.easylive.web.annotation.GlobalInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
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


@Validated
@Slf4j
@RestController
@RequestMapping("/file")
public class FileController extends ABaseController {

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private AppConfig appConfig;

    @Resource
    private VideoInfoFileService videoInfoFileService;

    @Resource
    private FFmpegUtils fFmpegUtils;


    /**
     * @Description: 上传资源文件
     * @param: [response] - HttpServletResponse对象，用于写入响应数据
     * @param: [sourceName] - 资源文件名，不能为空
     * @return: void
     */
    @RequestMapping("/getResource")
    @GlobalInterceptor
    public void getResource(HttpServletResponse response, @NotEmpty String sourceName) {
        // 检查sourceName是否有效
        if (!StringTools.pathIsOk(sourceName)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        // 获取文件后缀
        String suffix = StringTools.getFileSuffix(sourceName);
        FileTypeEnum fileTypeEnum = FileTypeEnum.getBySuffix(suffix);

        // 检查文件类型
        if (null == fileTypeEnum) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        // 根据文件类型设置响应头
        switch (fileTypeEnum) {
            case IMAGE:
                // 设置缓存时间为30天
                response.setHeader("Cache-Control", "max-age=" + 30 * 24 * 60 * 60);
                response.setContentType("image/" + suffix.replace(".", ""));
                break;
        }

        // 读取文件并写入响应
        readFile(response, sourceName);
    }

    /**
     * @Description: 读取文件并将其写入HTTP响应
     * @param: [response] - HttpServletResponse对象，用于写入文件内容
     * @param: [filePath] - 文件路径
     * @return: void
     */
    protected void readFile(HttpServletResponse response, String filePath) {
        File file = new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER + filePath);

        // 检查文件是否存在
        if (!file.exists()) {
            return; // 如果文件不存在，直接返回
        }

        try (OutputStream out = response.getOutputStream(); FileInputStream in = new FileInputStream(file)) {
            byte[] byteData = new byte[1024];
            int len;
            // 读取文件并写入输出流
            while ((len = in.read(byteData)) != -1) {
                out.write(byteData, 0, len);
            }
            out.flush(); // 刷新输出流
        } catch (Exception e) {
            log.error("读取文件异常", e); // 记录异常日志
        }
    }

    /**
     * @Description: 预上传视频，保存必要的上传信息
     * @param: [fileName] - 要上传的视频文件名，不能为空
     * @param: [chunks] - 视频文件的分片数
     * @return: com.easylive.entity.vo.ResponseVO - 返回上传ID的响应对象
     */
    @RequestMapping("/preUploadVideo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO preUploadVideo(@NotEmpty String fileName, @NotNull Integer chunks) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto(); // 获取用户信息
        // 保存视频文件的预上传信息
        String uploadId = redisComponent.savePreVideoFileInfo(tokenUserInfoDto.getUserId(), fileName, chunks);
        return getSuccessResponseVO(uploadId); // 返回成功响应
    }

    /**
     * @throws IOException - 文件操作异常
     * @Description: 上传视频的某一分片
     * @param: [chunkFile] - 视频分片文件
     * @param: [chunkIndex] - 当前分片的索引
     * @param: [uploadId] - 上传ID
     * @return: com.easylive.entity.vo.ResponseVO - 返回操作结果的响应对象
     */
    @RequestMapping("/uploadVideo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO uploadVideo(@NotNull MultipartFile chunkFile, @NotNull Integer chunkIndex, @NotEmpty String uploadId) throws IOException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto(); // 获取用户信息
        UploadingFileDto fileDto = redisComponent.getUploadingVideoFile(tokenUserInfoDto.getUserId(), uploadId);

        // 检查文件是否存在
        if (fileDto == null) {
            throw new BusinessException("文件不存在请重新上传");
        }

        SysSettingDto sysSettingDto = redisComponent.getSysSettingDto(); // 获取系统设置
        // 检查文件大小是否超过限制
        if (fileDto.getFileSize() > sysSettingDto.getVideoSize() * Constants.MB_SIZE) {
            throw new BusinessException("文件超过最大文件限制");
        }

        // 检查分片索引
        if ((chunkIndex - 1) > fileDto.getChunkIndex() || chunkIndex > fileDto.getChunks() - 1) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        // 保存分片到指定文件夹
        String folder = appConfig.getProjectFolder() + Constants.FILE_FOLDER + Constants.FILE_FOLDER_TEMP + fileDto.getFilePath();
        File targetFile = new File(folder + "/" + chunkIndex);
        chunkFile.transferTo(targetFile); // 将分片文件转移到目标位置

        // 更新上传信息
        fileDto.setChunkIndex(chunkIndex);
        fileDto.setFileSize(fileDto.getFileSize() + chunkFile.getSize());
        redisComponent.updateVideoFileInfo(tokenUserInfoDto.getUserId(), fileDto); // 更新Redis中的视频文件信息
        return getSuccessResponseVO(null); // 返回成功响应
    }

    /**
     * @throws IOException - 文件操作异常
     * @Description: 删除上传的视频信息
     * @param: [uploadId] - 上传ID
     * @return: com.easylive.entity.vo.ResponseVO - 返回操作结果的响应对象
     */
    @RequestMapping("/delUploadVideo")
    public ResponseVO delUploadVideo(@NotEmpty String uploadId) throws IOException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto(); // 获取用户信息
        UploadingFileDto fileDto = redisComponent.getUploadingVideoFile(tokenUserInfoDto.getUserId(), uploadId);

        // 检查文件是否存在
        if (fileDto == null) {
            throw new BusinessException("文件不存在请重新上传");
        }

        // 删除Redis中的视频文件信息
        redisComponent.delVideoFileInfo(tokenUserInfoDto.getUserId(), uploadId);
        // 删除临时文件夹
        FileUtils.deleteDirectory(new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER + Constants.FILE_FOLDER_TEMP + fileDto.getFilePath()));
        return getSuccessResponseVO(uploadId); // 返回成功响应
    }

    /**
     * @throws IOException - 文件操作异常
     * @Description: 上传图片
     * @param: [file] - 上传的图片文件，不能为空
     * @param: [createThumbnail] - 是否生成缩略图
     * @return: com.easylive.entity.vo.ResponseVO - 返回上传结果的响应对象
     */
    @RequestMapping("/uploadImage")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO uploadCover(@NotNull MultipartFile file, @NotNull Boolean createThumbnail) throws IOException {
        String day = DateUtil.format(new Date(), DateTimePatternEnum.YYYYMMDD.getPattern()); // 获取当前日期
        String folder = appConfig.getProjectFolder() + Constants.FILE_FOLDER + Constants.FILE_COVER + day; // 图片存储路径
        File folderFile = new File(folder);

        // 创建文件夹
        if (!folderFile.exists()) {
            folderFile.mkdirs();
        }

        String fileName = file.getOriginalFilename(); // 获取原始文件名
        String fileSuffix = fileName.substring(fileName.lastIndexOf(".")); // 获取文件后缀
        String realFileName = StringTools.getRandomString(Constants.LENGTH_30) + fileSuffix; // 生成随机文件名
        String filePath = folder + "/" + realFileName; // 生成完整文件路径
        file.transferTo(new File(filePath)); // 保存文件

        // 如果需要，生成缩略图
        if (createThumbnail) {
            fFmpegUtils.createImageThumbnail(filePath);
        }
        return getSuccessResponseVO(Constants.FILE_COVER + day + "/" + realFileName); // 返回成功响应
    }

    /**
     * @Description: 获取视频资源
     * @param: [response] - HttpServletResponse对象，用于写入响应数据
     * @param: [fileId] - 视频文件ID，不能为空
     * @return: void
     */
    @RequestMapping("/videoResource/{fileId}")
    @GlobalInterceptor
    public void getVideoResource(HttpServletResponse response, @PathVariable @NotEmpty String fileId) {
        VideoInfoFile videoInfoFile = videoInfoFileService.getVideoInfoFileByFileId(fileId); // 获取视频信息
        String filePath = videoInfoFile.getFilePath(); // 获取文件路径
        readFile(response, filePath + "/" + Constants.M3U8_NAME); // 读取视频文件

        // 记录视频播放信息
        VideoPlayInfoDto videoPlayInfoDto = new VideoPlayInfoDto();
        videoPlayInfoDto.setVideoId(videoInfoFile.getVideoId());
        videoPlayInfoDto.setFileIndex(videoInfoFile.getFileIndex());

        TokenUserInfoDto tokenUserInfoDto = getTokenInfoFromCookie();
        if (tokenUserInfoDto != null) {
            videoPlayInfoDto.setUserId(tokenUserInfoDto.getUserId());
        }
        redisComponent.addVideoPlay(videoPlayInfoDto); // 将播放信息存储到Redis
    }

    /**
     * @Description: 获取视频分片资源
     * @param: [response] - HttpServletResponse对象，用于写入响应数据
     * @param: [fileId] - 视频文件ID，不能为空
     * @param: [ts] - 分片文件名
     * @return: void
     */
    @RequestMapping("/videoResource/{fileId}/{ts}")
    @GlobalInterceptor
    public void getVideoResourceTs(HttpServletResponse response, @PathVariable @NotEmpty String fileId, @PathVariable @NotNull String ts) {
        VideoInfoFile videoInfoFile = videoInfoFileService.getVideoInfoFileByFileId(fileId); // 获取视频信息
        String filePath = videoInfoFile.getFilePath(); // 获取文件路径
        readFile(response, filePath + "/" + ts); // 读取分片文件
    }

}
