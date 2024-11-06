package com.studyclass.media.service;

import com.studyclass.base.model.PageParams;
import com.studyclass.base.model.PageResult;
import com.studyclass.base.model.RestResponse;
import com.studyclass.media.model.dto.QueryMediaParamsDto;
import com.studyclass.media.model.dto.UploadFileParamsDto;
import com.studyclass.media.model.dto.UploadFileResultDto;
import com.studyclass.media.model.po.MediaFiles;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * @author Mr.M
 * @version 1.0
 * @description 媒资文件管理业务类
 * @date 2022/9/10 8:55
 */
public interface MediaFileService {

    /**
     * @param pageParams          分页参数
     * @param queryMediaParamsDto 查询条件
     * @return com.studyclass.base.model.PageResult<com.studyclass.media.model.po.MediaFiles>
     * @description 媒资文件查询方法
     * @author Mr.M
     * @date 2022/9/10 8:57
     */
    public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto);

    /**
     * 上传文件
     * @param companyId 机构id
     * @param uploadFileParamsDto 文件信息
     * @param localFilePath 文件本地地址
     *        objectName 如果传入按目录存储，如果不穿按年月日存储
     * @return
     */
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath,String objectName);

    public MediaFiles addMediaFilesToDb(Long companyId,String fileMd5,UploadFileParamsDto uploadFileParamsDto,String bucket,String objectName);


    /**
     * @description 检查文件是否存在
     * @param fileMd5 文件的md5
     * @return com.xuecheng.base.model.RestResponse<java.lang.Boolean> false不存在，true存在
     * @author Mr.M
     * @date 2022/9/13 15:38
     */
    public RestResponse<Boolean> checkFile(String fileMd5);

    /**
     * @description 检查分块是否存在
     * @param fileMd5  文件的md5
     * @param chunkIndex  分块序号
     * @return com.xuecheng.base.model.RestResponse<java.lang.Boolean> false不存在，true存在
     * @author Mr.M
     * @date 2022/9/13 15:39
     */
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex);

    RestResponse uploadChunk(String localChunkFilePath, String fileMd5, int chunk);

    /**
     * @description 合并分块
     * @param companyId  机构id
     * @param fileMd5  文件md5
     * @param chunkTotal 分块总和
     * @param uploadFileParamsDto 文件信息
     * @return com.xuecheng.base.model.RestResponse
     * @author Mr.M
     * @date 2022/9/13 15:56
     */
    public RestResponse mergechunks(Long companyId,String fileMd5,int chunkTotal,UploadFileParamsDto uploadFileParamsDto);

    public File downloadFileFromMinIO(String bucket, String objectName);

    public boolean addMediaFilesToMinio(String localFilePath, String mimeType, String bucket, String objectName);

    /**
     * 根据媒资id查询媒资信息
     * @param mediaId
     * @return
     */
    MediaFiles getFileById(String mediaId);
}
