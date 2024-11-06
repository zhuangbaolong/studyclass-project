package com.studyclass.media.api;

import com.studyclass.base.model.PageParams;
import com.studyclass.base.model.PageResult;
import com.studyclass.media.model.dto.UploadFileParamsDto;
import com.studyclass.media.model.dto.UploadFileResultDto;
import com.studyclass.media.model.po.MediaFiles;
import com.studyclass.media.model.dto.QueryMediaParamsDto;
import com.studyclass.media.service.MediaFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * @author Mr.M
 * @version 1.0
 * @description 媒资文件管理接口
 * @date 2022/9/6 11:29
 */
@Api(value = "媒资文件管理接口", tags = "媒资文件管理接口")
@RestController
public class MediaFilesController {


    @Autowired
    MediaFileService mediaFileService;


    @ApiOperation("媒资列表查询接口")
    @PostMapping("/files")
    public PageResult<MediaFiles> list(PageParams pageParams, @RequestBody QueryMediaParamsDto queryMediaParamsDto) {
        Long companyId = 1232141425L;
        return mediaFileService.queryMediaFiels(companyId, pageParams, queryMediaParamsDto);

    }

    @ApiOperation("上传文件")
    @RequestMapping(value = "/upload/coursefile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadFileResultDto uploadFile(@RequestPart("filedata") MultipartFile filedata,
                                      @RequestParam(value = "objectName", required = false) String objectName) throws IOException {
        //准备文件上传信息
        UploadFileParamsDto uploadFileParamsDto = new UploadFileParamsDto();
        //原始文件名称
        uploadFileParamsDto.setFilename(filedata.getOriginalFilename());
        //文件大小
        uploadFileParamsDto.setFileSize(filedata.getSize());
        //文件类型
        uploadFileParamsDto.setTags("001001");
        //创建临时文件
        File tempFile = File.createTempFile("minio", ".temp");
        tempFile.getName();
        tempFile.getPath();
        filedata.transferTo(tempFile);
        //文件路径
        String localFilePath = tempFile.getAbsolutePath();
        //调用
        UploadFileResultDto uploadFileResultDto = mediaFileService.uploadFile(1232141425L, uploadFileParamsDto, localFilePath, objectName);
        return uploadFileResultDto;
    }

}
