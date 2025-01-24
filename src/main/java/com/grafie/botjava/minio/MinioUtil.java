package com.grafie.botjava.minio;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author grafie.chen
 * @since 2025/1/22  15:36
 */
@Component
public class MinioUtil {

    private final MinioClient minioClient;

    private final String bucketName;

    private final String endpoint;

    public MinioUtil(@Value("${minio.endpoint}") String endpoint,
                     @Value("${minio.access-key}") String accessKey,
                     @Value("${minio.secret-key}") String secretKey,
                     @Value("${minio.bucket-name}") String bucketName) {
        this.bucketName = bucketName;
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        this.endpoint = endpoint;
    }

    /**
     * 上传文件到 MinIO
     *
     * @param file     待上传的文件
     * @param fileName 文件在 MinIO 中的存储名称
     * @return 文件的访问 URL
     * @throws Exception 如果操作失败
     */
    public String uploadFile(File file, String fileName) throws Exception {
        // 格式化名称，避免重复
        fileName = FilenameUtils.getBaseName(fileName)
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + FilenameUtils.getExtension(fileName);
        // 检查桶是否存在，不存在则创建
        boolean bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
        );
        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }

        // 上传文件
        try (InputStream inputStream = new FileInputStream(file)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(inputStream, file.length(), -1)
                            .contentType("image/png") // 设置文件类型
                            .build()
            );
        }

        // 返回文件访问 URL
        return endpoint + "/" + bucketName + "/" + fileName;
    }
}
