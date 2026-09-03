package com.example.videoplatform.video.storage;

import java.io.InputStream;
import java.nio.file.Path;

public interface VideoStorage {
    String upload(String key, InputStream inputStream, long contentLength, String contentType);
    void download(String key, Path destination);
    void delete(String key);
}
