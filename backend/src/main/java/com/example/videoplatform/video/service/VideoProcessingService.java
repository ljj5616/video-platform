package com.example.videoplatform.video.service;

import com.example.videoplatform.video.repository.VideoRepository;
import com.example.videoplatform.video.storage.VideoStorage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class VideoProcessingService {
    private static final Logger log = LoggerFactory.getLogger(VideoProcessingService.class);
    private final VideoRepository videoRepository;
    private final VideoStorage videoStorage;
    private final String ffmpegExecutable;
    private final String ffprobeExecutable;

    public VideoProcessingService(VideoRepository videoRepository, VideoStorage videoStorage,
                                  @Value("${video.ffmpeg.executable}") String ffmpegExecutable,
                                  @Value("${video.ffmpeg.ffprobe-executable}") String ffprobeExecutable) {
        this.videoRepository = videoRepository;
        this.videoStorage = videoStorage;
        this.ffmpegExecutable = ffmpegExecutable;
        this.ffprobeExecutable = ffprobeExecutable;
    }

    @Async
    public void process(Long videoId, String originalKey, String outputId) {
        Path workDirectory = null;
        try {
            workDirectory = Files.createTempDirectory("video-processing-");
            Path input = workDirectory.resolve("source");
            Path output = workDirectory.resolve("master.m3u8");
            videoStorage.download(originalKey, input);
            transcode(input, output);

            String outputPrefix = "videos/processed/" + outputId + "/";
            String processedUrl = uploadOutputs(workDirectory, outputPrefix);
            Integer durationSeconds = probeDuration(input);
            videoRepository.findById(videoId).ifPresent(video -> {
                video.completeProcessing(processedUrl, durationSeconds);
                videoRepository.save(video);
            });
        } catch (Exception exception) {
            log.error("Video processing failed. videoId={}", videoId, exception);
            videoRepository.findById(videoId).ifPresent(video -> {
                video.failProcessing();
                videoRepository.save(video);
            });
        } finally {
            deleteDirectory(workDirectory);
        }
    }

    private void transcode(Path input, Path output) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(
                ffmpegExecutable, "-y", "-i", input.toString(),
                "-c:v", "libx264", "-preset", "medium", "-crf", "23",
                "-c:a", "aac", "-b:a", "128k",
                "-hls_time", "6", "-hls_playlist_type", "vod",
                "-hls_segment_filename", output.getParent().resolve("segment-%05d.ts").toString(),
                output.toString()
        ).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        int exitCode = process.waitFor();
        if (exitCode != 0 || !Files.isRegularFile(output) || Files.size(output) == 0) {
            throw new IOException("FFmpeg exited with code " + exitCode);
        }
    }

    private String uploadOutputs(Path directory, String outputPrefix) throws IOException {
        String playlistUrl = null;
        try (var files = Files.list(directory)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String name = file.getFileName().toString();
                if (!name.endsWith(".m3u8") && !name.endsWith(".ts")) continue;
                String contentType = name.endsWith(".m3u8") ? "application/vnd.apple.mpegurl" : "video/mp2t";
                try (var stream = Files.newInputStream(file)) {
                    String url = videoStorage.upload(outputPrefix + name, stream, Files.size(file), contentType);
                    if (name.equals("master.m3u8")) playlistUrl = url;
                }
            }
        }
        if (playlistUrl == null) throw new IOException("FFmpeg did not create an HLS playlist");
        return playlistUrl;
    }

    private Integer probeDuration(Path input) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(ffprobeExecutable, "-v", "error", "-show_entries",
                "format=duration", "-of", "default=noprint_wrappers=1:nokey=1", input.toString())
                .redirectErrorStream(true).start();
        String value;
        try (var reader = process.inputReader()) {
            value = reader.readLine();
        }
        if (process.waitFor() != 0 || value == null) throw new IOException("FFprobe failed");
        return (int) Math.ceil(Double.parseDouble(value.trim().toLowerCase(Locale.ROOT)));
    }

    private void deleteDirectory(Path directory) {
        if (directory == null) return;
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
    }
}
