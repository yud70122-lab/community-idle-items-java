package com.community.idle.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.community.idle.config.OssProperties;
import com.community.idle.config.WatermarkProperties;
import com.community.idle.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class OssUtil {

    private final OssProperties ossProperties;
    private final WatermarkProperties watermarkProperties;
    private final WatermarkUtil watermarkUtil;
    private final OSS ossClient;

    public OssUtil(OssProperties ossProperties, WatermarkProperties watermarkProperties, WatermarkUtil watermarkUtil) {
        this.ossProperties = ossProperties;
        this.watermarkProperties = watermarkProperties;
        this.watermarkUtil = watermarkUtil;
        CredentialsProvider credentialsProvider = new DefaultCredentialProvider(
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret()
        );
        this.ossClient = new OSSClientBuilder().build(ossProperties.getEndpoint(), credentialsProvider);
    }

    public String uploadFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String fileName = generateFileName(originalFilename);
        return uploadFile(file, fileName);
    }

    public String uploadFileWithWatermark(MultipartFile file, String watermarkText) {
        if (!watermarkProperties.getEnabled() || !isImageFile(file.getOriginalFilename())) {
            return uploadFile(file);
        }

        String originalFilename = file.getOriginalFilename();
        String fileName = generateFileName(originalFilename);

        try (InputStream watermarkedStream = watermarkUtil.addWatermark(file, watermarkText)) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    ossProperties.getBucketName(),
                    fileName,
                    watermarkedStream,
                    metadata
            );
            ossClient.putObject(putObjectRequest);
            return generateFileUrl(fileName);
        } catch (Exception e) {
            log.warn("图片水印处理失败，使用原图上传: {}", e.getMessage());
            return uploadFile(file, fileName);
        }
    }

    public String uploadFile(MultipartFile file, String fileName) {
        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    ossProperties.getBucketName(),
                    fileName,
                    inputStream,
                    metadata
            );
            ossClient.putObject(putObjectRequest);
            return generateFileUrl(fileName);
        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException("文件上传失败");
        }
    }

    public String uploadFile(InputStream inputStream, String fileName, String contentType) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    ossProperties.getBucketName(),
                    fileName,
                    inputStream,
                    metadata
            );
            ossClient.putObject(putObjectRequest);
            return generateFileUrl(fileName);
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException("文件上传失败");
        }
    }

    private boolean isImageFile(String filename) {
        if (filename == null) return false;
        String lowerName = filename.toLowerCase();
        return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")
                || lowerName.endsWith(".png") || lowerName.endsWith(".bmp")
                || lowerName.endsWith(".gif");
    }

    public void deleteFile(String fileUrl) {
        try {
            String fileName = extractFileNameFromUrl(fileUrl);
            ossClient.deleteObject(ossProperties.getBucketName(), fileName);
            log.info("文件删除成功: {}", fileName);
        } catch (Exception e) {
            log.error("文件删除失败: {}", e.getMessage(), e);
            throw new BusinessException("文件删除失败");
        }
    }

    public String generatePresignedUrl(String fileName) {
        return generatePresignedUrl(fileName, ossProperties.getUrlExpireTime());
    }

    public String generatePresignedUrl(String fileName, int expireSeconds) {
        try {
            Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000L);
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                    ossProperties.getBucketName(),
                    fileName
            );
            request.setExpiration(expiration);
            URL url = ossClient.generatePresignedUrl(request);
            return url.toString();
        } catch (Exception e) {
            log.error("生成签名URL失败: {}", e.getMessage(), e);
            throw new BusinessException("生成签名URL失败");
        }
    }

    private String generateFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uuid = UUID.randomUUID().toString().replace("-", "");
        long timestamp = System.currentTimeMillis();
        return String.format("%d/%s%s", timestamp / 10000, uuid, extension);
    }

    private String generateFileUrl(String fileName) {
        return String.format("https://%s.%s/%s",
                ossProperties.getBucketName(),
                ossProperties.getEndpoint(),
                fileName
        );
    }

    private String extractFileNameFromUrl(String fileUrl) {
        String prefix = String.format("https://%s.%s/",
                ossProperties.getBucketName(),
                ossProperties.getEndpoint()
        );
        if (fileUrl.startsWith(prefix)) {
            return fileUrl.substring(prefix.length());
        }
        if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
            int firstSlash = fileUrl.indexOf('/', 8);
            if (firstSlash != -1) {
                return fileUrl.substring(firstSlash + 1);
            }
        }
        return fileUrl;
    }
}
