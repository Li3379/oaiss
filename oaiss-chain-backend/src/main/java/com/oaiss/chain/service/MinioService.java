package com.oaiss.chain.service;

import com.oaiss.chain.config.MinioConfig;
import com.oaiss.chain.constant.ErrorCode;
import com.oaiss.chain.exception.BusinessException;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO对象存储服务
 * 
 * <p>提供文件上传、下载、删除、预览等功能</p>
 * 
 * @author OAISS Chain Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    /**
     * 文件上传结果
     */
    public record UploadResult(String objectName, String url, long size, String contentType) {}

    /**
     * 文件信息
     */
    public record FileInfo(String objectName, long size, String contentType, String etag) {}

    /**
     * 初始化存储桶（如果不存在则创建）
     */
    public void initBucket() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .build());
            
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .build());
                log.info("Created MinIO bucket: {}", minioConfig.getBucketName());
            }
        } catch (Exception e) {
            log.error("Failed to initialize MinIO bucket", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to initialize storage bucket");
        }
    }

    /**
     * 上传文件
     *
     * @param file   文件
     * @param folder 文件夹路径（如：reports/2024）
     * @return 上传结果
     */
    public UploadResult uploadFile(MultipartFile file, String folder) {
        return uploadFile(file, folder, null);
    }

    /**
     * 上传文件（携带上传者ID）
     *
     * @param file   文件
     * @param folder 文件夹路径（如：reports/2024）
     * @param uploaderId 上传者用户ID
     * @return 上传结果
     */
    public UploadResult uploadFile(MultipartFile file, String folder, Long uploaderId) {
        validateFile(file);

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String objectName = generateObjectName(folder, extension);

            var putObjectBuilder = PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType());

            // 存储上传者ID到自定义元数据
            if (uploaderId != null) {
                putObjectBuilder.userMetadata(java.util.Map.of(
                        "x-amz-meta-uploader-id", String.valueOf(uploaderId)));
            }

            minioClient.putObject(putObjectBuilder.build());

            String url = getFileUrl(objectName);
            log.info("File uploaded successfully: {} (uploader: {})", objectName, uploaderId);

            return new UploadResult(objectName, url, file.getSize(), file.getContentType());
        } catch (Exception e) {
            log.error("Failed to upload file", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件上传者ID
     *
     * @param objectName 对象名称
     * @return 上传者用户ID，无元数据则返回null
     */
    public Long getFileOwner(String objectName) {
        try {
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .build());

            Map<String, String> metadata = stat.userMetadata();
            if (metadata != null && metadata.containsKey("x-amz-meta-uploader-id")) {
                return Long.parseLong(metadata.get("x-amz-meta-uploader-id"));
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 上传文件（使用InputStream）
     *
     * @param inputStream   输入流
     * @param objectName    对象名称
     * @param contentType   内容类型
     * @param size          文件大小
     * @return 上传结果
     */
    public UploadResult uploadStream(InputStream inputStream, String objectName, 
                                     String contentType, long size) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build());
            
            String url = getFileUrl(objectName);
            log.info("Stream uploaded successfully: {}", objectName);
            
            return new UploadResult(objectName, url, size, contentType);
        } catch (Exception e) {
            log.error("Failed to upload stream", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 下载文件
     *
     * @param objectName 对象名称
     * @return 文件输入流
     */
    public InputStream downloadFile(String objectName) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            log.error("Failed to download file: {}", objectName, e);
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "文件不存在或下载失败");
        }
    }

    /**
     * 删除文件
     *
     * @param objectName 对象名称
     */
    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .build());
            log.info("File deleted successfully: {}", objectName);
        } catch (Exception e) {
            log.error("Failed to delete file: {}", objectName, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除文件
     *
     * @param objectNames 对象名称列表
     */
    public void deleteFiles(List<String> objectNames) {
        try {
            List<DeleteObject> objects = objectNames.stream()
                    .map(DeleteObject::new)
                    .toList();
            
            minioClient.removeObjects(RemoveObjectsArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .objects(objects)
                    .build());
            
            log.info("Files deleted successfully: {}", objectNames);
        } catch (Exception e) {
            log.error("Failed to delete files", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "批量删除文件失败: " + e.getMessage());
        }
    }

    /**
     * 检查文件是否存在
     *
     * @param objectName 对象名称
     * @return 是否存在
     */
    @Transactional(readOnly = true)
    public boolean fileExists(String objectName) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取文件信息
     *
     * @param objectName 对象名称
     * @return 文件信息
     */
    @Transactional(readOnly = true)
    public FileInfo getFileInfo(String objectName) {
        try {
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .build());
            
            return new FileInfo(
                    objectName,
                    stat.size(),
                    stat.contentType(),
                    stat.etag()
            );
        } catch (Exception e) {
            log.error("Failed to get file info: {}", objectName, e);
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "文件不存在");
        }
    }

    /**
     * 获取文件预签名URL（用于临时访问）
     *
     * @param objectName 对象名称
     * @return 预签名URL
     */
    @Transactional(readOnly = true)
    public String getPresignedUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .expiry(minioConfig.getPresignedUrlExpiry(), TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            log.error("Failed to get presigned URL: {}", objectName, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取文件URL失败");
        }
    }

    /**
     * 获取文件上传预签名URL（用于前端直传）
     *
     * @param objectName 对象名称
     * @return 预签名上传URL
     */
    public String getPresignedUploadUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .expiry(minioConfig.getPresignedUrlExpiry(), TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            log.error("Failed to get presigned upload URL: {}", objectName, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取上传URL失败");
        }
    }

    /**
     * 列出文件夹下的文件（分页）
     *
     * @param prefix 文件夹前缀
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 文件列表（带分页信息）
     */
    @Transactional(readOnly = true)
    public FileListResult listFiles(String prefix, Integer page, Integer size) {
        try {
            if (page == null || page < 1) page = 1;
            if (size == null || size < 1) size = 20;
            size = Math.min(size, 1000);

            List<FileInfo> allFiles = new ArrayList<>();
            Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .prefix(prefix)
                    .recursive(true)
                    .build());

            for (Result<Item> result : results) {
                Item item = result.get();
                allFiles.add(new FileInfo(
                        item.objectName(),
                        item.size(),
                        null,
                        item.etag()
                ));
            }

            int total = allFiles.size();
            int fromIndex = Math.min((page - 1) * size, total);
            int toIndex = Math.min(fromIndex + size, total);
            List<FileInfo> pageFiles = allFiles.subList(fromIndex, toIndex);

            return new FileListResult(pageFiles, total, page, size);
        } catch (Exception e) {
            log.error("Failed to list files: {}", prefix, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取文件列表失败");
        }
    }

    /**
     * 文件列表分页结果
     */
    public record FileListResult(List<FileInfo> files, long total, int page, int size) {}

    /**
     * 复制文件
     *
     * @param sourceObjectName 源对象名称
     * @param targetObjectName 目标对象名称
     */
    public void copyFile(String sourceObjectName, String targetObjectName) {
        try {
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(targetObjectName)
                    .source(CopySource.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(sourceObjectName)
                            .build())
                    .build());
            
            log.info("File copied from {} to {}", sourceObjectName, targetObjectName);
        } catch (Exception e) {
            log.error("Failed to copy file from {} to {}", sourceObjectName, targetObjectName, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件复制失败");
        }
    }

    /**
     * 允许的文件扩展名白名单
     */
    private static final java.util.Set<String> DANGEROUS_EXTENSIONS = java.util.Set.of(
            ".jsp", ".jspx", ".exe", ".bat", ".cmd", ".sh", ".php", ".py",
            ".dll", ".so", ".vbs", ".wsf", ".msi", ".com", ".scr", ".jar", ".war"
    );

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }

        if (file.getSize() > minioConfig.getMaxFileSize()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "文件大小超过限制，最大允许 " + (minioConfig.getMaxFileSize() / 1024 / 1024) + "MB");
        }

        // 验证文件类型（禁止危险扩展名）
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = getFileExtension(originalFilename).toLowerCase();
            if (DANGEROUS_EXTENSIONS.contains(extension)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "不允许上传此类型的文件: " + extension);
            }
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * 生成对象名称
     */
    private String generateObjectName(String folder, String extension) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String timestamp = String.valueOf(System.currentTimeMillis());
        
        if (folder != null && !folder.isEmpty()) {
            return folder + "/" + timestamp + "_" + uuid + extension;
        }
        return timestamp + "_" + uuid + extension;
    }

    /**
     * 获取文件URL
     */
    private String getFileUrl(String objectName) {
        return minioConfig.getEndpoint() + "/" + 
               minioConfig.getBucketName() + "/" + objectName;
    }
}
