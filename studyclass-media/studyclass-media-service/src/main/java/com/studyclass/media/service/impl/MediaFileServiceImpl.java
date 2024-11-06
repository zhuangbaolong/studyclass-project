package com.studyclass.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.studyclass.base.exception.StudyClassException;
import com.studyclass.base.model.PageParams;
import com.studyclass.base.model.PageResult;
import com.studyclass.base.model.RestResponse;
import com.studyclass.media.mapper.MediaFilesMapper;
import com.studyclass.media.mapper.MediaProcessMapper;
import com.studyclass.media.model.dto.QueryMediaParamsDto;
import com.studyclass.media.model.dto.UploadFileParamsDto;
import com.studyclass.media.model.dto.UploadFileResultDto;
import com.studyclass.media.model.po.MediaFiles;
import com.studyclass.media.model.po.MediaProcess;
import com.studyclass.media.service.MediaFileService;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/9/10 8:58
 */
@Service
@Slf4j
public class MediaFileServiceImpl implements MediaFileService {

    @Autowired
    MediaFilesMapper mediaFilesMapper;

    @Autowired
    MediaFileService currentProxy;

    @Autowired
    MinioClient minioClient;

    //    存储普通文件
    @Value("${minio.bucket.files}")
    private String bucket_mediafiles;
    //存储视频
    @Value("${minio.bucket.videofiles}")
    private String bucket_video;

    @Autowired
    MediaProcessMapper mediaProcessMapper;

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

    //根据扩展名获取mimeType
    public String getMimeType(String extension) {
        if (extension == null) {
            extension = "";
        }
        //根据扩展名取出mimeType
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
        //通用mimeType，字节流
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (extensionMatch != null) {
            mimeType = extensionMatch.getMimeType();
        }
        return mimeType;
    }

    //获取文件默认路径 年/月/日

    public String getDefaultFolderPath() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String fold = simpleDateFormat.format(new Date()).replace("-", "/") + "/";
        return fold;
    }

    //上传文件到minio
    public boolean addMediaFilesToMinio(String localFilePath, String mimeType, String bucket, String objectName) {
        try {
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                    .bucket(bucket)
                    //.object("test001.mp4")
                    .filename(localFilePath)
                    .object(objectName)//文件目录
                    .contentType(mimeType)//默认根据扩展名确定文件内容类型，也可以指定
                    .build();
            minioClient.uploadObject(uploadObjectArgs);
            log.error("上传文件到minio成功,bucket:{},objectName:{}", bucket, objectName);
            System.out.println("文件上传成功");
            return true;
        } catch (Exception e) {
            log.error("上传文件出错,bucket:{},objectName:{},错误信息:{}", bucket, objectName, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    //获取文件md5格式=》id

    public String getFileMd5(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            String md5Hex = null;
            md5Hex = DigestUtils.md5Hex(fileInputStream);
            return md5Hex;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    @Transactional
    //将文件保存到数据库
    public MediaFiles addMediaFilesToDb(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto, String bucket, String objectName) {
        //将文件信息保存到数据库
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles == null) {
            mediaFiles = new MediaFiles();
            //拷贝数据
            BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
            //文件id
            mediaFiles.setId(fileMd5);
            //机构id
            mediaFiles.setCompanyId(companyId);
            //桶
            mediaFiles.setBucket(bucket_video);
            //file_path:对象名
            mediaFiles.setFilePath(objectName);
            //file_id
            mediaFiles.setFileId(fileMd5);
            //url:完整的url
            mediaFiles.setUrl("/" + bucket + "/" + objectName);
            //上传时间
            mediaFiles.setCreateDate(LocalDateTime.now());
            //状态
            mediaFiles.setStatus("1");
            //审核状态
            mediaFiles.setAuditStatus("002003");
            int insert = mediaFilesMapper.insert(mediaFiles);
            if (insert <= 0) {
                log.error("保存文件信息到数据库失败,{}", mediaFiles.toString());
                StudyClassException.cast("");
            }
            log.debug("保存文件信息到数据库成功,{}", mediaFiles.toString());
        }
        //根据mimeType判断如果是avi视频写入待处理任务
        //记录待处理的任务，向mediaprocess插入记录
        addWaitingTask(mediaFiles);
        return mediaFiles;
    }

    /**
     * 添加待处理任务
     * @param mediaFiles 媒资文件信息
     */
    private void addWaitingTask(MediaFiles mediaFiles){
        String fileId = mediaFiles.getFileId();
        LambdaQueryWrapper<MediaProcess> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MediaProcess::getFileId,fileId);
        MediaProcess mediaProcess = mediaProcessMapper.selectOne(queryWrapper);
        if (mediaProcess == null) {
            //文件名称
            String filename = mediaFiles.getFilename();
            //文件扩展名
            String exension = filename.substring(filename.lastIndexOf("."));
            //文件mimeType
            String mimeType = getMimeType(exension);
            //如果是avi视频添加到视频待处理表
            if(mimeType.equals("video/x-msvideo")){
            MediaProcess mediaProcess1 = new MediaProcess();
                BeanUtils.copyProperties(mediaFiles,mediaProcess1);
                mediaProcess1.setStatus("1");//未处理
                mediaProcess1.setCreateDate(LocalDateTime.now());
                mediaProcess1.setFailCount(0);//失败次数默认为0
                mediaProcessMapper.insert(mediaProcess1);
            }
        }else {
            log.info("===========已存在任务=============");
        }
    }


    @Override
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath,String objectName) {
        //将文件上传到minio
        //文件名
        String filename = uploadFileParamsDto.getFilename();
        //得到扩展名
        String extension = filename.substring(filename.lastIndexOf("."));
        String mimeType = getMimeType(extension);
        //objectName
        String defaultFolderPath = getDefaultFolderPath();
        //文件md5值
        String fileMd5 = getFileMd5(new File(localFilePath));
        if (StringUtils.isEmpty(objectName)) {
            objectName = defaultFolderPath + fileMd5 + extension;
        }
        //存储到minio
        boolean result = addMediaFilesToMinio(localFilePath, mimeType, bucket_mediafiles, objectName);
        if (!result) {
            StudyClassException.cast("上传文件失败");
        }
        //入库文件信息
        MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_mediafiles, objectName);
        if (mediaFiles == null) {
            //入库失败
            StudyClassException.cast("文件上传后保存信息失败");
        }
        UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
        BeanUtils.copyProperties(mediaFiles, uploadFileResultDto);
        return uploadFileResultDto;
    }

    /**
     * 查询文件
     *
     * @param fileMd5 文件的md5
     * @return
     */
    @Override
    public RestResponse<Boolean> checkFile(String fileMd5) {
        //先查询数据库
        MediaFiles mediaFile = mediaFilesMapper.selectById(fileMd5);
        if (mediaFile != null) {
            String bucket = mediaFile.getBucket();
            String filePath = mediaFile.getFilePath();
            //存在则查询minio
            GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(filePath)
                    .build();
            //查询远程服务获取到的流对象
            try {
                FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
                if (inputStream != null) {
                    return RestResponse.success(true);
                }
            } catch (Exception e) {
            }
        }
        //文件不存在
        return RestResponse.success(false);
    }

    /**
     * 查询分块文件
     *
     * @param fileMd5    文件的md5
     * @param chunkIndex 分块序号
     * @return
     */
    @Override
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {
        //分块存储路径：md5前两位为分块子目录：chunk
        //根据md5获取分块文件所在目录的路径
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        //查询minio
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket(bucket_video)
                .object(chunkFileFolderPath + chunkIndex)
                .build();
        try {
            FilterInputStream response = minioClient.getObject(getObjectArgs);
            if (response != null) {
                return RestResponse.success(true);
            }
        } catch (Exception e) {
//            throw new RuntimeException(e);
        }
        return RestResponse.success(false);
    }

    /**
     * 上传分块
     *
     * @param localChunkFilePath 上传分块地址
     * @param fileMd5            文件md5
     * @param chunk              分块序号
     * @return
     */
    @Override
    public RestResponse uploadChunk(String localChunkFilePath, String fileMd5, int chunk) {
        //分块文件路径
        String chunFilePath = getChunkFileFolderPath(fileMd5) + chunk;
        //将分块文件上传到minio
        String mimeType = getMimeType(null);
        boolean b = addMediaFilesToMinio(localChunkFilePath, mimeType, bucket_video, chunFilePath);
        if (!b) {
            return RestResponse.success(false, "上传文件失败");
        }
        return RestResponse.success(true);
    }

    /**
     * 合并文件
     * @param companyId  机构id
     * @param fileMd5  文件md5
     * @param chunkTotal 分块总和
     * @param uploadFileParamsDto 文件信息
     * @return
     */
    @Transactional
    @Override
    public RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {
        //找到分块文件调用minio的sdk进行文件合并
        //分块文件所在目录
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        List<ComposeSource> sources = Stream.iterate(0, i -> ++i)
                .limit(chunkTotal)
                .map(i -> ComposeSource.builder()
                        .bucket(bucket_video)
                        .object(chunkFileFolderPath.concat(Integer.toString(i)))
                        .build())
                .collect(Collectors.toList());
        //==============合并=============
        //源文件名称
        String filename = uploadFileParamsDto.getFilename();
        //扩展名
        String extension = filename.substring(filename.lastIndexOf("."));
        //合并文件路径
        String objectName = getFilePathByMd5(fileMd5, extension);
        //指定合并后的objectName等信息
        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs
                .builder()
                .bucket(bucket_video)
                .object(objectName) //合并后的文件
                .sources(sources)
                .build();
        //TODO 报错size 1048576 must be greater than 5242880，minio默认的分块文件大小为5M
        try {
            minioClient.composeObject(composeObjectArgs);
            log.info("合并文件成功,bucket:{},objectName:{}", bucket_video, objectName);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("合并文件出错,bucket:{},objectName:{},错误信息:{}", bucket_video, objectName, e.getMessage());
            return RestResponse.validfail(false, "合并文件出错");
        }
        //检验合并后的和源文件是否一致
        //先下载文件
        File newFile = downloadFileFromMinIO(bucket_video, objectName);
        //计算合并后的文件md5
        try (FileInputStream fileInputStream = new FileInputStream(newFile)) {
            String mergeFileMd5 = DigestUtils.md5Hex(fileInputStream);
            //比较
            if (!fileMd5.equals(mergeFileMd5)) {
                log.error("校验合并文件md5值不一致，原始文件:{},合并文件:{}", fileMd5, mergeFileMd5);
                return RestResponse.validfail(false, "文件校验失败");
            }
            //设置文件大小
            uploadFileParamsDto.setFileSize(newFile.length());
        } catch (IOException e) {
            return RestResponse.validfail(false, "文件校验失败2");
        }
        //文件入库
        MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_video, objectName);
        if (mediaFiles == null) {
            return RestResponse.validfail(false, "文件入库失败");
        }
        //清理分块文件
        clearChunkFiles(chunkFileFolderPath, chunkTotal);
        return RestResponse.success(true);
    }


    /**
     * 清除分块文件
     *
     * @param chunkFileFolderPath 分块文件路径
     * @param chunkTotal          分块文件总数
     */
    private void clearChunkFiles(String chunkFileFolderPath, int chunkTotal) {
        try {
            List<DeleteObject> deleteObjects = Stream.iterate(0, i -> ++i)
                    .limit(chunkTotal)
                    .map(i -> new DeleteObject(chunkFileFolderPath.concat(Integer.toString(i))))
                    .collect(Collectors.toList());

            RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder().bucket("video").objects(deleteObjects).build();
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
            //真正删除
            results.forEach(r -> {
                DeleteError deleteError = null;
                try {
                    deleteError = r.get();
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("清楚分块文件失败,objectname:{}", deleteError.objectName(), e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            log.error("清楚分块文件失败,chunkFileFolderPath:{}", chunkFileFolderPath, e);
        }
    }

    /**
     * 从minio下载文件
     *
     * @param bucket     桶
     * @param objectName 对象名称
     * @return 下载后的文件
     */
    public File downloadFileFromMinIO(String bucket, String objectName) {
        //临时文件
        File minioFile = null;
        FileOutputStream outputStream = null;
        try {
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
            //创建临时文件
            minioFile = File.createTempFile("minio", ".merge");
            outputStream = new FileOutputStream(minioFile);
            IOUtils.copy(stream, outputStream);
            return minioFile;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    //得到分块文件的路径

    private String getChunkFileFolderPath(String fileMd5) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + "chunk" + "/";
    }
    //根据md5值得到合并后的路径地址

    private String getFilePathByMd5(String fileMd5, String fileExt) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }
    @Override
    public MediaFiles getFileById(String mediaId) {
        MediaFiles mediaFiles = mediaFilesMapper.selectById(mediaId);
        return mediaFiles;
    }
}
