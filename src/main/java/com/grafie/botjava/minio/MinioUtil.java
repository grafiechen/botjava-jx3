package com.grafie.botjava.minio;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import com.grafie.botjava.util.SensitiveDataUtil;
import com.grafie.botjava.util.OutboundHttpRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * @author grafie.chen
 * @since 2025/1/22  15:36
 */
@Slf4j
@Component
public class MinioUtil {

    private final MinioClient minioClient;

    private final String bucketName;

    private final String publicUrl;

    public MinioUtil(MinioClient minioClient,
                     @Value("${minio.bucket-name}") String bucketName,
                     @Value("${minio.public-url}") String publicUrl) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
        this.publicUrl = stripTrailingSlash(publicUrl);
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
        if (file == null || !file.isFile() || file.length() == 0) {
            throw new IllegalArgumentException("待上传文件必须存在且非空");
        }
        long startNanos = System.nanoTime();
        String objectName = buildObjectName(fileName);
        log.info("外部 HTTP 请求开始，service=>MinIO，operation=>putObject，bucket=>{}，object=>{}，bytes=>{}",
                bucketName, objectName, file.length());
        try {
            // 检查桶是否存在，不存在则创建
            OutboundHttpRateLimiter.awaitPermit();
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!bucketExists) {
                log.info("外部 HTTP 请求开始，service=>MinIO，operation=>makeBucket，bucket=>{}", bucketName);
                OutboundHttpRateLimiter.awaitPermit();
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("外部 HTTP 请求完成，service=>MinIO，operation=>makeBucket，bucket=>{}", bucketName);
            }

            // 上传文件
            try (InputStream inputStream = new FileInputStream(file)) {
                OutboundHttpRateLimiter.awaitPermit();
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .stream(inputStream, file.length(), -1)
                                .contentType("image/png") // 设置文件类型
                                .build()
                );
            }

            // 返回文件访问 URL
            String url = publicUrl + "/" + bucketName + "/" + objectName;
            log.info("外部 HTTP 请求完成，service=>MinIO，operation=>putObject，bucket=>{}，object=>{}，elapsedMs=>{}，publicUrl=>{}",
                    bucketName, objectName, elapsedMillis(startNanos), url);
            return url;
        } catch (Exception e) {
            log.error("外部 HTTP 请求失败，service=>MinIO，operation=>putObject，bucket=>{}，object=>{}，elapsedMs=>{}，reason=>{}",
                    bucketName, objectName, elapsedMillis(startNanos), SensitiveDataUtil.summarize(e), e);
            throw e;
        }
    }

    private String buildObjectName(String fileName) {
        String baseName = FilenameUtils.getBaseName(fileName == null ? "" : fileName)
                .replaceAll("[^A-Za-z0-9._-]", "-");
        if (baseName.isBlank()) {
            baseName = "image";
        }
        String extension = FilenameUtils.getExtension(fileName == null ? "" : fileName)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
        String suffix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + "-" + UUID.randomUUID();
        return baseName + "-" + suffix + (extension.isBlank() ? "" : "." + extension);
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("minio.public-url 不能为空");
        }
        return value.replaceAll("/+$", "");
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
