package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Api(tags = "common apis")
@Slf4j
@RestController
@RequestMapping("admin/common")
public class CommonController {

    @Autowired
    private AliOssUtil aliOssUtil;

    @ApiOperation("upload file to OSS")
    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) {

        String objectName = file.getOriginalFilename();
        String extension = objectName.substring(objectName.lastIndexOf("."));

        objectName = UUID.randomUUID().toString() + extension;

        try {
            String fileURL = aliOssUtil.upload(file.getBytes(), objectName);
            return Result.success(fileURL);
        } catch (IOException e) {
            log.error("upload to OSS failed: {}", e.getMessage());
        }

        return Result.error(MessageConstant.UPLOAD_FAILED);
    }

}
