package com.example.upload;

import com.example.message.VideoJobMessage;
import com.example.message.VideoResolutionOption;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/videos")
@RequiredArgsConstructor
public class VideoUploadController {

    private final RabbitTemplate rabbitTemplate;

    @Value("${dir.streaming.storage}")
    private String videoOriginPath;
    @Value("${dir.streaming.storage}")
    private String videoStreamingStoragePath;


    @PostMapping("/upload")
    public ResponseEntity<String> uploadVideo(@RequestParam("file")MultipartFile file) throws IOException {

        // 1. 파일 저장
        String originalFilename = file.getOriginalFilename();
        String fileId = UUID.randomUUID().toString();

        String savedPath = videoOriginPath + fileId + "/" +  originalFilename;

        if(!Files.exists(Path.of(savedPath))) {
            Files.createDirectories(Path.of(savedPath).getParent());
        }
        file.transferTo(new File(savedPath));

        // 2. 멀티 해상도 옵션 정의
        List<VideoResolutionOption> renditions = List.of(
                new VideoResolutionOption("1920:-2", "5000k", "1080p"),
                new VideoResolutionOption("1280:-2", "3000k", "720p"),
                new VideoResolutionOption("854:-2",  "1500k", "480p")
        );

        // 3. 메시지 생성 후 큐 적재
        VideoJobMessage msg = new VideoJobMessage(fileId, originalFilename, savedPath, renditions);
        rabbitTemplate.convertAndSend("video.exchange", "video.jobs", msg);

        return ResponseEntity.ok("업로드 성공, 변환 대기중");
    }

    @GetMapping("/{fileId}/master.m3u8")
    @CrossOrigin(origins = "*")
    public ResponseEntity<Resource> masterPlaylist(@PathVariable String fileId) throws IOException {
        return serveFile(fileId, "master.m3u8", "application/vnd.apple.mpegurl");
    }

    @GetMapping("/{fileId}/{resolution}/{filename:.+}")
    @CrossOrigin(origins = "*")
    public ResponseEntity<Resource> streamVideo(
            @PathVariable String fileId,
            @PathVariable String resolution,
            @PathVariable String filename) throws IOException {

        return serveFile(fileId + "/" + resolution, filename, filename.endsWith(".m3u8") ?
                "application/vnd.apple.mpegurl" : "video/MP2T");
    }

    private ResponseEntity<Resource> serveFile(String relativePath, String filename, String contentType) throws IOException {
        Path path = Paths.get(videoStreamingStoragePath, relativePath, filename);
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(resource);
    }
}
