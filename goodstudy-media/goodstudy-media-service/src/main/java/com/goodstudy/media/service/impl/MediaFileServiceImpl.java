package com.goodstudy.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.goodstudy.base.exception.GoodStudyException;
import com.goodstudy.base.model.PageParams;
import com.goodstudy.base.model.PageResult;
import com.goodstudy.media.mapper.MediaFilesMapper;
import com.goodstudy.media.model.dto.UploadFileParamsDto;
import com.goodstudy.media.model.dto.UploadFileResultDto;
import com.goodstudy.media.service.MediaFileService;
import com.goodstudy.media.model.dto.QueryMediaParamsDto;
import com.goodstudy.media.model.po.MediaFiles;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * @author Mr.M
 * @version 1.0
 * @description 媒资文件管理接口实现类
 * @date 2022/9/10 8:58
 */
@Slf4j
@Service
public class MediaFileServiceImpl implements MediaFileService {

    @Autowired
    private MinioClient minioClient;

    /**
     * minio文件存储桶名称
     */
    @Value("${minio.bucket.files}")
    private String bucketFiles;

    @Autowired
    MediaFileService currentProxy;

    @Autowired
    MediaFilesMapper mediaFilesMapper;

    @Override
    public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {
        //构建查询条件对象
        LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();
        //分页对象
        Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
        // 获取数据列表
        List<MediaFiles> list = pageResult.getRecords();
        // 获取数据总数
        long total = pageResult.getTotal();
        // 构建结果集
        PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
        return mediaListResult;

    }

    /**
     * 上传文件
     *
     * @param companyId           java.lang.Long
     * @param uploadFileParamsDto com.goodstudy.media.model.dto.UploadFileParamsDto
     * @param localFilePath       java.lang.String
     * @return com.goodstudy.media.model.dto.UploadFileResultDto
     * @author Jack
     * @date 2023/4/4 16:08
     * @update_by Jack
     * @update_at 2023/4/4 16:08
     * @creed Talk is cheap, show me the comment !!!
     */
    @Override
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath) {
        File file = new File(localFilePath);
        if (!file.exists()) {
            GoodStudyException.cast("文件不存在");
        }
        // 文件名称
        String fileName = uploadFileParamsDto.getFilename();
        // 文件扩展名
        String extension = fileName.substring(fileName.lastIndexOf("."));
        // 文件mimeType
        String mimeType = getMimeType(extension);
        // 文件md5
        String fileMd5 = getFileMd5(file);
        // 文件的默认目录
        String defaultFolderPath = getDefaultFolderPath();
        // 存储到minio中的对象名(带目录)
        String objectName = defaultFolderPath + fileMd5 + extension;
        boolean b = addMediaFilesToMinIO(localFilePath, mimeType, bucketFiles, objectName);
        if (!b) {
            GoodStudyException.cast("上传文件到minio失败");
        }
        // 文件大小
        long fileSize = file.length();
        uploadFileParamsDto.setFileSize(fileSize);
        // 将文件信息存储到数据库
        MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucketFiles, objectName);
        // 返回结果
        UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
        BeanUtils.copyProperties(mediaFiles, uploadFileResultDto);
        return uploadFileResultDto;
    }

    /**
     * 上传文件到minio
     *
     * @param localFilePath java.lang.String
     * @param mimeType      java.lang.String
     * @param bucket        java.lang.String
     * @param objectName    java.lang.String
     * @return boolean
     * @author Jack
     * @date 2023/4/4 15:13
     * @update_by Jack
     * @update_at 2023/4/4 15:13
     * @creed Talk is cheap, show me the comment !!!
     */
    public boolean addMediaFilesToMinIO(String localFilePath, String mimeType, String bucket, String objectName) {
        try {
            UploadObjectArgs testbucket = UploadObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .filename(localFilePath)
                    .contentType(mimeType)
                    .build();
            minioClient.uploadObject(testbucket);
            log.debug("上传文件到minio成功,bucket:{},objectName:{}", bucket, objectName);
            System.out.println("上传成功");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("上传文件到minio出错,bucket:{},objectName:{},错误原因:{}", bucket, objectName, e.getMessage(), e);
            return false;
        }
    }


    /**
     * 上传文件到数据库
     *
     * @param
     * @return com.goodstudy.media.model.po.MediaFiles
     * @author Jack
     * @date 2023/4/4 15:19
     * @update_by Jack
     * @update_at 2023/4/4 15:19
     * @creed Talk is cheap, show me the comment !!!
     */
    @Transactional
    public MediaFiles addMediaFilesToDb(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto, String bucket, String objectName) {
        // 从数据库查询文件
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles == null) {
            // 不存在则添加
            mediaFiles = new MediaFiles();
            BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
            mediaFiles.setId(fileMd5);
            mediaFiles.setFileId(fileMd5);
            mediaFiles.setCompanyId(companyId);
            mediaFiles.setUrl("/" + bucket + "/" + objectName);
            mediaFiles.setBucket(bucket);
            mediaFiles.setFilePath(objectName);
            mediaFiles.setCreateDate(LocalDateTime.now());
            mediaFiles.setAuditStatus("002003");
            mediaFiles.setStatus("1");
            //保存文件信息到文件表
            int insert = mediaFilesMapper.insert(mediaFiles);
            if (insert < 0) {
                log.error("保存文件信息到数据库失败,{}", mediaFiles.toString());
                GoodStudyException.cast("保存文件信息失败");
            }
            log.debug("保存文件信息到数据库成功,{}", mediaFiles.toString());
        }
        return mediaFiles;
    }

    /**
     * 获取默认的文件夹路径 年/月/日
     *
     * @return
     */
    private String getDefaultFolderPath() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        // 当前时间所对应的文件夹名称
        String folder = sdf.format(new Date()).replace("-", "/") + "/";
        return folder;
    }

    /**
     * 获取文件的md5值
     *
     * @param File
     * @return
     */
    private String getFileMd5(File File) {
        try (FileInputStream fileInputStream = new FileInputStream(File)) {
            String fileMd5 = DigestUtils.md5Hex(fileInputStream);
            return fileMd5;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    /**
     * 根据扩展名获取mimeType
     *
     * @param extension
     * @return
     */
    private String getMimeType(String extension) {
        if (extension == null) {
            extension = "";
        }
        // 根据扩展名取出mimeType
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
        // 默认值
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (extensionMatch != null) {
            mimeType = extensionMatch.getMimeType();
        }
        return mimeType;
    }
}