package com.easylive.service.impl;

import com.easylive.component.EsSearchComponent;
import com.easylive.component.RedisComponent;
import com.easylive.entity.config.AppConfig;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.dto.SysSettingDto;
import com.easylive.entity.dto.UploadingFileDto;
import com.easylive.entity.enums.*;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.po.VideoInfoFile;
import com.easylive.entity.po.VideoInfoFilePost;
import com.easylive.entity.po.VideoInfoPost;
import com.easylive.entity.query.*;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.exception.BusinessException;
import com.easylive.mappers.*;
import com.easylive.service.UserMessageService;
import com.easylive.service.VideoInfoPostService;
import com.easylive.utils.CopyTools;
import com.easylive.utils.FFmpegUtils;
import com.easylive.utils.StringTools;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 视频信息 业务接口实现
 */
@Service("videoInfoPostService")
@Slf4j
public class VideoInfoPostServiceImpl implements VideoInfoPostService {

    @Resource
    private VideoInfoPostMapper<VideoInfoPost, VideoInfoPostQuery> videoInfoPostMapper;

    @Resource
    private VideoInfoFilePostMapper<VideoInfoFilePost, VideoInfoFilePostQuery> videoInfoFilePostMapper;

    @Resource
    private VideoInfoMapper<VideoInfo, VideoInfoQuery> videoInfoMapper;

    @Resource
    private VideoInfoFileMapper<VideoInfoFile, VideoInfoFileQuery> videoInfoFileMapper;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private AppConfig appConfig;

    @Resource
    private FFmpegUtils fFmpegUtils;

    @Resource
    private UserMessageService userMessageService;

    @Resource
    private EsSearchComponent esSearchComponent;

    @Resource
    private UserInfoMapper userInfoMapper;

    /**
     * 根据条件查询列表
     */
    @Override
    public List<VideoInfoPost> findListByParam(VideoInfoPostQuery param) {
        List<VideoInfoPost> dataList = this.videoInfoPostMapper.selectList(param);
        return dataList;
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public Integer findCountByParam(VideoInfoPostQuery param) {
        return this.videoInfoPostMapper.selectCount(param);
    }

    /**
     * 分页查询方法
     */
    @Override
    public PaginationResultVO<VideoInfoPost> findListByPage(VideoInfoPostQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<VideoInfoPost> list = this.findListByParam(param);
        PaginationResultVO<VideoInfoPost> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    @Override
    public Integer add(VideoInfoPost bean) {
        return this.videoInfoPostMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<VideoInfoPost> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.videoInfoPostMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或者修改
     */
    @Override
    public Integer addOrUpdateBatch(List<VideoInfoPost> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.videoInfoPostMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 多条件更新
     */
    @Override
    public Integer updateByParam(VideoInfoPost bean, VideoInfoPostQuery param) {
        StringTools.checkParam(param);
        return this.videoInfoPostMapper.updateByParam(bean, param);
    }

    /**
     * 多条件删除
     */
    @Override
    public Integer deleteByParam(VideoInfoPostQuery param) {
        StringTools.checkParam(param);
        return this.videoInfoPostMapper.deleteByParam(param);
    }

    /**
     * 根据VideoId获取对象
     */
    @Override
    public VideoInfoPost getVideoInfoPostByVideoId(String videoId) {
        return this.videoInfoPostMapper.selectByVideoId(videoId);
    }

    /**
     * 根据VideoId修改
     */
    @Override
    public Integer updateVideoInfoPostByVideoId(VideoInfoPost bean, String videoId) {
        return this.videoInfoPostMapper.updateByVideoId(bean, videoId);
    }

    /**
     * 根据VideoId删除
     */
    @Override
    public Integer deleteVideoInfoPostByVideoId(String videoId) {
        return this.videoInfoPostMapper.deleteByVideoId(videoId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveVideoInfo(VideoInfoPost videoInfoPost, List<VideoInfoFilePost> uploadFileList) {
        if (uploadFileList.size() > redisComponent.getSysSettingDto().getVideoPCount()) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        if (!StringTools.isEmpty(videoInfoPost.getVideoId())) {
            VideoInfoPost videoInfoPostDb = this.videoInfoPostMapper.selectByVideoId(videoInfoPost.getVideoId());
            if (videoInfoPostDb == null) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
            if (ArrayUtils.contains(new Integer[]{VideoStatusEnum.STATUS0.getStatus(), VideoStatusEnum.STATUS2.getStatus()}, videoInfoPostDb.getStatus())) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
        }

        Date curDate = new Date();
        String videoId = videoInfoPost.getVideoId();
        List<VideoInfoFilePost> deleteFileList = new ArrayList();
        List<VideoInfoFilePost> addFileList = uploadFileList;

        if (StringTools.isEmpty(videoId)) {
            videoId = StringTools.getRandomString(Constants.LENGTH_10);
            videoInfoPost.setVideoId(videoId);
            videoInfoPost.setCreateTime(curDate);
            videoInfoPost.setLastUpdateTime(curDate);
            videoInfoPost.setStatus(VideoStatusEnum.STATUS0.getStatus());
            this.videoInfoPostMapper.insert(videoInfoPost);
        } else {
            //查询已经存在的视频
            VideoInfoFilePostQuery fileQuery = new VideoInfoFilePostQuery();
            fileQuery.setVideoId(videoId);
            fileQuery.setUserId(videoInfoPost.getUserId());
            List<VideoInfoFilePost> dbInfoFileList = this.videoInfoFilePostMapper.selectList(fileQuery);
            Map<String, VideoInfoFilePost> uploadFileMap = uploadFileList.stream().collect(Collectors.toMap(item -> item.getUploadId(), Function.identity(), (data1,
                                                                                                                                                              data2) -> data2));
            //删除的文件 -> 数据库中有，uploadFileList没有
            Boolean updateFileName = false;
            for (VideoInfoFilePost fileInfo : dbInfoFileList) {
                VideoInfoFilePost updateFile = uploadFileMap.get(fileInfo.getUploadId());
                if (updateFile == null) {
                    deleteFileList.add(fileInfo);
                } else if (!updateFile.getFileName().equals(fileInfo.getFileName())) {
                    updateFileName = true;
                }
            }
            //新增的文件  没有fileId就是新增的文件
            addFileList = uploadFileList.stream().filter(item -> item.getFileId() == null).collect(Collectors.toList());
            videoInfoPost.setLastUpdateTime(curDate);

            //判断视频信息是否有更改
            Boolean changeVideoInfo = this.changeVideoInfo(videoInfoPost);
            if (!addFileList.isEmpty()) {
                videoInfoPost.setStatus(VideoStatusEnum.STATUS0.getStatus());
            } else if (changeVideoInfo || updateFileName) {
                videoInfoPost.setStatus(VideoStatusEnum.STATUS2.getStatus());
            }
            this.videoInfoPostMapper.updateByVideoId(videoInfoPost, videoInfoPost.getVideoId());
        }

        //清除已经删除的数据
        if (!deleteFileList.isEmpty()) {
            List<String> delFileIdList = deleteFileList.stream().map(item -> item.getFileId()).collect(Collectors.toList());
            this.videoInfoFilePostMapper.deleteBatchByFileId(delFileIdList, videoInfoPost.getUserId());
            //将要删除的视频加入消息队列
            List<String> delFilePathList = deleteFileList.stream().map(item -> item.getFilePath()).collect(Collectors.toList());
            redisComponent.addFile2DelQueue(videoId, delFilePathList);
        }

        //更新视频信息
        Integer index = 1;
        for (VideoInfoFilePost videoInfoFile : uploadFileList) {
            videoInfoFile.setFileIndex(index++);
            videoInfoFile.setVideoId(videoId);
            videoInfoFile.setUserId(videoInfoPost.getUserId());
            if (videoInfoFile.getFileId() == null) {
                videoInfoFile.setFileId(StringTools.getRandomString(Constants.LENGTH_20));
                videoInfoFile.setUpdateType(VideoFileUpdateTypeEnum.UPDATE.getStatus());
                videoInfoFile.setTransferResult(VideoFileTransferResultEnum.TRANSFER.getStatus());
            }
        }
        this.videoInfoFilePostMapper.insertOrUpdateBatch(uploadFileList);


        //将需要转码的视频加入队列
        if (!addFileList.isEmpty()) {
            for (VideoInfoFilePost file : addFileList) {
                file.setUserId(videoInfoPost.getUserId());
                file.setVideoId(videoId);
            }
            redisComponent.addFile2TransferQueue(addFileList);
        }
    }

    private boolean changeVideoInfo(VideoInfoPost videoInfoPost) {
        VideoInfoPost dbInfo = this.videoInfoPostMapper.selectByVideoId(videoInfoPost.getVideoId());
        //标题，封面，标签，简介
        if (!videoInfoPost.getVideoCover().equals(dbInfo.getVideoCover()) || !videoInfoPost.getVideoName().equals(dbInfo.getVideoName()) || !videoInfoPost.getTags().equals(dbInfo.getTags()) || !videoInfoPost.getIntroduction().equals(
                dbInfo.getIntroduction())) {
            return true;
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferVideoFile(VideoInfoFilePost videoInfoFile) {
        VideoInfoFilePost updateFilePost = new VideoInfoFilePost();
        try {
            UploadingFileDto fileDto = redisComponent.getUploadingVideoFile(videoInfoFile.getUserId(), videoInfoFile.getUploadId());
            /**
             * 拷贝文件到正式目录
             */
            String tempFilePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER + Constants.FILE_FOLDER_TEMP + fileDto.getFilePath();

            File tempFile = new File(tempFilePath);

            String targetFilePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER + Constants.FILE_VIDEO + fileDto.getFilePath();
            File taregetFile = new File(targetFilePath);
            if (!taregetFile.exists()) {
                taregetFile.mkdirs();
            }
            FileUtils.copyDirectory(tempFile, taregetFile);

            /**
             * 删除临时目录
             */
            FileUtils.forceDelete(tempFile);
            redisComponent.delVideoFileInfo(videoInfoFile.getUserId(), videoInfoFile.getUploadId());

            /**
             * 合并文件
             */
            String completeVideo = targetFilePath + Constants.TEMP_VIDEO_NAME;
            this.union(targetFilePath, completeVideo, true);

            /**
             * 获取播放时长
             */
            Integer duration = fFmpegUtils.getVideoInfoDuration(completeVideo);
            updateFilePost.setDuration(duration);
            updateFilePost.setFileSize(new File(completeVideo).length());
            updateFilePost.setFilePath(Constants.FILE_VIDEO + fileDto.getFilePath());
            updateFilePost.setTransferResult(VideoFileTransferResultEnum.SUCCESS.getStatus());

            /**
             * ffmpeg切割文件
             */
            this.convertVideo2Ts(completeVideo);
        } catch (Exception e) {
            log.error("文件转码失败", e);
            updateFilePost.setTransferResult(VideoFileTransferResultEnum.FAIL.getStatus());
        } finally {
            //更新文件状态
            videoInfoFilePostMapper.updateByUploadIdAndUserId(updateFilePost, videoInfoFile.getUploadId(), videoInfoFile.getUserId());
            //更新视频信息
            VideoInfoFilePostQuery fileQuery = new VideoInfoFilePostQuery();
            fileQuery.setVideoId(videoInfoFile.getVideoId());
            fileQuery.setTransferResult(VideoFileTransferResultEnum.FAIL.getStatus());
            Integer failCount = videoInfoFilePostMapper.selectCount(fileQuery);
            if (failCount > 0) {
                VideoInfoPost videoUpdate = new VideoInfoPost();
                videoUpdate.setStatus(VideoStatusEnum.STATUS1.getStatus());
                videoInfoPostMapper.updateByVideoId(videoUpdate, videoInfoFile.getVideoId());
                return;
            }
            fileQuery.setTransferResult(VideoFileTransferResultEnum.TRANSFER.getStatus());
            Integer transferCount = videoInfoFilePostMapper.selectCount(fileQuery);
            if (transferCount == 0) {
                Integer duration = videoInfoFilePostMapper.sumDuration(videoInfoFile.getVideoId());
                VideoInfoPost videoUpdate = new VideoInfoPost();
                videoUpdate.setStatus(VideoStatusEnum.STATUS2.getStatus());
                videoUpdate.setDuration(duration);
                videoInfoPostMapper.updateByVideoId(videoUpdate, videoInfoFile.getVideoId());
            }
        }
    }

    public static void union(String dirPath, String toFilePath, boolean delSource) throws BusinessException {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            throw new BusinessException("目录不存在");
        }
        File fileList[] = dir.listFiles();
        File targetFile = new File(toFilePath);
        try (RandomAccessFile writeFile = new RandomAccessFile(targetFile, "rw")) {
            byte[] b = new byte[1024 * 10];
            for (int i = 0; i < fileList.length; i++) {
                int len = -1;
                //创建读块文件的对象
                File chunkFile = new File(dirPath + File.separator + i);
                RandomAccessFile readFile = null;
                try {
                    readFile = new RandomAccessFile(chunkFile, "r");
                    while ((len = readFile.read(b)) != -1) {
                        writeFile.write(b, 0, len);
                    }
                } catch (Exception e) {
                    log.error("合并分片失败", e);
                    throw new BusinessException("合并文件失败");
                } finally {
                    readFile.close();
                }
            }
        } catch (Exception e) {
            throw new BusinessException("合并文件" + dirPath + "出错了");
        } finally {
            if (delSource) {
                for (int i = 0; i < fileList.length; i++) {
                    fileList[i].delete();
                }
            }
        }
    }


    private void convertVideo2Ts(String videoFilePath) {
        File videoFile = new File(videoFilePath);
        //创建同名切片目录
        File tsFolder = videoFile.getParentFile();
        String codec = fFmpegUtils.getVideoCodec(videoFilePath);
        //转码
        if (Constants.VIDEO_CODE_HEVC.equals(codec)) {
            String tempFileName = videoFilePath + Constants.VIDEO_CODE_TEMP_FILE_SUFFIX;
            new File(videoFilePath).renameTo(new File(tempFileName));
            fFmpegUtils.convertHevc2Mp4(tempFileName, videoFilePath);
            new File(tempFileName).delete();
        }

        //视频转为ts
        fFmpegUtils.convertVideo2Ts(tsFolder, videoFilePath);

        //删除视频文件
        videoFile.delete();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditVideo(String videoId, Integer status, String reason) {
        VideoStatusEnum videoStatusEnum = VideoStatusEnum.getByStatus(status);
        if (videoStatusEnum == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        VideoInfoPost videoInfoPost = new VideoInfoPost();
        videoInfoPost.setStatus(status);

        VideoInfoPostQuery videoInfoPostQuery = new VideoInfoPostQuery();
        videoInfoPostQuery.setStatus(VideoStatusEnum.STATUS2.getStatus());
        videoInfoPostQuery.setVideoId(videoId);
        Integer audioCount = this.videoInfoPostMapper.updateByParam(videoInfoPost, videoInfoPostQuery);
        if (audioCount == 0) {
            throw new BusinessException("审核失败，请稍后重试");
        }
        /**
         * 更新视频状态
         */

        VideoInfoFilePost videoInfoFilePost = new VideoInfoFilePost();
        videoInfoFilePost.setUpdateType(VideoFileUpdateTypeEnum.NO_UPDATE.getStatus());

        VideoInfoFilePostQuery filePostQuery = new VideoInfoFilePostQuery();
        filePostQuery.setVideoId(videoId);
        this.videoInfoFilePostMapper.updateByParam(videoInfoFilePost, filePostQuery);

        if (VideoStatusEnum.STATUS4 == videoStatusEnum) {
            return;
        }
        VideoInfoPost infoPost = this.videoInfoPostMapper.selectByVideoId(videoId);
        /**
         * 第一次发布增加用户积分
         */
        VideoInfo dbVideoInfo = this.videoInfoMapper.selectByVideoId(videoId);
        if (dbVideoInfo == null) {
            SysSettingDto sysSettingDto = redisComponent.getSysSettingDto();
            userInfoMapper.updateCoinCountInfo(infoPost.getUserId(), sysSettingDto.getPostVideoCoinCount());
        }

        /**
         * 将发布信息复制到正式表信息
         */
        VideoInfo videoInfo = CopyTools.copy(infoPost, VideoInfo.class);
        this.videoInfoMapper.insertOrUpdate(videoInfo);

        /**
         * 更新视频信息 先删除再添加
         */
        VideoInfoFileQuery videoInfoFileQuery = new VideoInfoFileQuery();
        videoInfoFileQuery.setVideoId(videoId);
        this.videoInfoFileMapper.deleteByParam(videoInfoFileQuery);


        /**
         * 查询发布表中的视频信息
         */
        VideoInfoFilePostQuery videoInfoFilePostQuery = new VideoInfoFilePostQuery();
        videoInfoFilePostQuery.setVideoId(videoId);
        List<VideoInfoFilePost> videoInfoFilePostList = this.videoInfoFilePostMapper.selectList(videoInfoFilePostQuery);

        List<VideoInfoFile> videoInfoFileList = CopyTools.copyList(videoInfoFilePostList, VideoInfoFile.class);
        this.videoInfoFileMapper.insertBatch(videoInfoFileList);

        /**
         * 删除文件
         */
        List<String> filePathList = redisComponent.getDelFileList(videoId);
        if (filePathList != null) {
            for (String path : filePathList) {
                File file = new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER + path);
                if (file.exists()) {
                    try {
                        FileUtils.deleteDirectory(file);
                    } catch (IOException e) {
                        log.error("删除文件失败", e);
                    }
                }
            }
        }
        redisComponent.cleanDelFileList(videoId);

        /**
         * 保存信息到es
         */
        esSearchComponent.saveDoc(videoInfo);
    }

    @Override
    public void recommendVideo(String videoId) {
        VideoInfo videoInfo = videoInfoMapper.selectByVideoId(videoId);
        if (videoInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        Integer recommendType = null;
        if (VideoRecommendTypeEnum.RECOMMEND.getType().equals(videoInfo.getRecommendType())) {
            recommendType = VideoRecommendTypeEnum.NO_RECOMMEND.getType();
        } else {
            recommendType = VideoRecommendTypeEnum.RECOMMEND.getType();
        }
        VideoInfo updateInfo = new VideoInfo();
        updateInfo.setRecommendType(recommendType);
        videoInfoMapper.updateByVideoId(updateInfo, videoId);
    }
}