package com.example.streaming;

import com.example.message.VideoCompletedMessage;
import com.example.message.VideoJobMessage;
import com.example.message.VideoResolutionOption;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoConvertJobWorkerService {

    private final RabbitTemplate rabbitTemplate;

    @Value("${dir.streaming.storage}")
    private String outputDir;

    @RabbitListener(queues = "video.jobs")
    public void handleVideoJob(VideoJobMessage msg) {
        try {
            String fileId = msg.getFileId();
            String fileName = msg.getFileName();
            String filePath = msg.getFilePath();

            List<VideoResolutionOption> renditions = msg.getRenditions();

            String outputDir = this.outputDir + fileId + "/";

            if(!Files.exists(Path.of(outputDir))) {
                Files.createDirectories(Path.of(outputDir));
            }

            // 해상도 옵션별 변환
            for (VideoResolutionOption option : renditions) {
                String scale = option.getResolution();
                String bitrate = option.getBitrate();
                String label = option.getLabel();

                String labelOutputDir = outputDir + label ;

                if(!Files.exists(Path.of(labelOutputDir))) {
                    Files.createDirectories(Path.of(labelOutputDir));
                }

                ProcessBuilder pb = new ProcessBuilder(
                        "ffmpeg",
                        "-i", filePath,
                        "-vf", "scale=" + scale,
                        "-c:v", "h264",
                        "-b:v", bitrate,
                        "-c:a", "aac",
                        "-b:a", "128k",
                        "-map", "0:v",
                        "-map", "0:a?",
                        "-hls_time", "5",
                        "-hls_playlist_type", "vod",
                        "-hls_segment_filename", labelOutputDir + "/" + label + "_%03d.ts",
                        labelOutputDir + "/" + label + ".m3u8"
                );
                pb.inheritIO();
                Process process = pb.start();
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new RuntimeException("ffmpeg 변환 실패: " + label);
                }
            }

            // master.m3u8 생성
            File master = new File(outputDir + "/master.m3u8");
            try (PrintWriter writer = new PrintWriter(master)) {
                writer.println("#EXTM3U");
                writer.println("#EXT-X-VERSION:3");
                for (VideoResolutionOption option : renditions) {
                    String label = option.getLabel();
                    String bitrate = option.getBitrate().replace("k", "000"); // HLS에 kbps 단위 필요
                    String resolution = option.getResolution().split(":")[0] + "x" +
                            option.getResolution().split(":")[1].replace("-2", "auto");
                    writer.printf("#EXT-X-STREAM-INF:BANDWIDTH=%s,RESOLUTION=%s%n", bitrate, resolution);
                    writer.println(label + "/" + label + ".m3u8");
                }
            }

            VideoCompletedMessage completed = new VideoCompletedMessage(fileId, fileName, outputDir, true, null);

            rabbitTemplate.convertAndSend("video.exchange", "video.completed", completed);
        } catch (Exception e) {
            e.printStackTrace();

            VideoCompletedMessage completed = new VideoCompletedMessage(
                    msg.getFileId(), msg.getFileName(), null, false, e.getMessage()
            );
            rabbitTemplate.convertAndSend("video.exchange", "video.completed", completed);
        }
    }
}
