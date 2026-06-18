package com.seip.expense.service;

import com.seip.expense.exception.FileStorageException;
import io.minio.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final MinioClient minioClient;

    @Value("${minio.endpoint}")
    private String minioEndpoint;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @PostConstruct
    public void initBucket() {
        try {
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Created MinIO bucket: {}", bucketName);
            } else {
                log.info("MinIO bucket already exists: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to initialize MinIO bucket {}: {}", bucketName, e.getMessage(), e);
            // Don't fail startup — MinIO may not be available in all environments
        }
    }

    /**
     * Uploads a file to MinIO and returns the public URL.
     *
     * @param bucketName  target bucket
     * @param objectName  full object path including folders
     * @param data        file input stream
     * @param size        file size in bytes
     * @param contentType MIME type
     * @return public URL to access the file
     */
    public String uploadFile(String bucketName, String objectName,
                             InputStream data, long size, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(data, size, -1)
                            .contentType(contentType)
                            .build());

            String url = minioEndpoint + "/" + bucketName + "/" + objectName;
            log.info("Uploaded file to MinIO: {}", url);
            return url;

        } catch (Exception e) {
            log.error("Failed to upload file to MinIO bucket {}/{}: {}", 
                    bucketName, objectName, e.getMessage(), e);
            throw new FileStorageException("Failed to upload file to storage", e);
        }
    }

    /**
     * Deletes an object from MinIO.
     */
    public void deleteFile(String bucketName, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
            log.info("Deleted file from MinIO: {}/{}", bucketName, objectName);
        } catch (Exception e) {
            log.error("Failed to delete file from MinIO: {}", e.getMessage(), e);
            throw new FileStorageException("Failed to delete file from storage", e);
        }
    }
}
