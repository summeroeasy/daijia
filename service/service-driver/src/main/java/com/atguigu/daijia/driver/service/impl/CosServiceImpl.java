package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.driver.config.TencentCloudProperties;
import com.atguigu.daijia.driver.service.CosService;
import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.*;

import com.qcloud.cos.region.Region;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CosServiceImpl implements CosService {

    @Autowired
    private TencentCloudProperties tencentCloudProperties;

    @Override
    public CosUploadVo upload(MultipartFile file, String path) {
        //获取cosClient客户端
        COSClient cosClient = this.getCosClient();
        //文件上传
        //元数据信息
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(file.getSize());
        meta.setContentType(file.getContentType());
        meta.setContentEncoding("utf-8");

        //向存储桶中保存文件
        String fileType = file.getOriginalFilename()
                .substring(file.getOriginalFilename().lastIndexOf(".")); //文件后缀名
        String uploadPath = "/driver/" + path + "/" +
                UUID.randomUUID().toString().replaceAll("-", "") + fileType;
        // 01.jpg
        // /driver/auth/0o98754.jpg
        PutObjectRequest putObjectRequest = null;
        try {
            //1 bucket名称
            putObjectRequest = new PutObjectRequest(tencentCloudProperties.getBucketPrivate(),
                    uploadPath,
                    file.getInputStream(),
                    meta);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        putObjectRequest.setStorageClass(StorageClass.Standard);
        PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest); //上传文件
        cosClient.shutdown();

        //返回vo对象
        CosUploadVo cosUploadVo = new CosUploadVo();
        cosUploadVo.setUrl(uploadPath);
        // 图片临时访问url，回显使用
        String imageUrl = getImageUrl(uploadPath);
        cosUploadVo.setShowUrl(imageUrl);
        return cosUploadVo;
    }

    private COSClient getCosClient() {
        //1 初始化用户身份信息
        String secretId = tencentCloudProperties.getSecretId();
        String secretKey = tencentCloudProperties.getSecretKey();
        BasicCOSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        //2 设置bucket地域 cos地域
        Region region = new Region(tencentCloudProperties.getRegion());
        ClientConfig clientConfig = new ClientConfig(region);
        //建议使用https协议
        clientConfig.setHttpProtocol(HttpProtocol.https);
        //3 生成cos客户端
        COSClient cosClient = new COSClient(cred, clientConfig);
        return cosClient;
    }

    //获取图片url
    @Override
    public String getImageUrl(String path) {
        if (!StringUtils.hasText(path)) {
            return "";
        }
        COSClient cosClient = this.getCosClient();
        GeneratePresignedUrlRequest request
                = new GeneratePresignedUrlRequest(tencentCloudProperties.getBucketPrivate(),
                path,
                HttpMethodName.GET);
        request.setExpiration(new DateTime().plusMinutes(15).toDate());
        URL url = cosClient.generatePresignedUrl(request);
        cosClient.shutdown();
        return url.toString();
    }
}
