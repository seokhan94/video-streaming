package com.example.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoJobMessage {
    private String fileId;
    private String fileName;
    private String filePath;

    private List<VideoResolutionOption> renditions; // 변환 해상도 옵션
}
