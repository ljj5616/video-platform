package com.example.videoplatform.video.storage;

import com.example.videoplatform.global.error.BusinessException;
import com.example.videoplatform.global.error.ErrorCode;
import java.io.InputStream;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class S3VideoStorage implements VideoStorage {
    private final S3Client s3Client;
    private final String bucket;
    private final String publicBaseUrl;

    public S3VideoStorage(S3Client s3Client, @Value("${video.storage.bucket}") String bucket,
                          @Value("${video.storage.public-base-url:}") String publicBaseUrl) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public String upload(String key, InputStream inputStream, long contentLength, String contentType) {
        ensureConfigured();
        try {
            var request = PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build();
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
            if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
                return publicBaseUrl.replaceAll("/+$", "") + "/" + key;
            }
            return s3Client.utilities().getUrl(builder -> builder.bucket(bucket).key(key)).toExternalForm();
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_FAILED);
        }
    }

    @Override
    public void download(String key, Path destination) {
        ensureConfigured();
        try {
            s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build(), destination);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_FAILED);
        }
    }

    @Override
    public void delete(String key) {
        if (bucket == null || bucket.isBlank()) return;
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (RuntimeException ignored) {
            // Best-effort cleanup must not hide the original failure.
        }
    }

    private void ensureConfigured() {
        if (bucket == null || bucket.isBlank()) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_FAILED);
        }
    }
}
