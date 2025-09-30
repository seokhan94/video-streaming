package com.example.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoCompletedMessage {
    private String fileId;
    private String fileName;
    private String outputDir;    // 스트리밍 파일이 저장된 경로
    private boolean success;     // 성공/실패 여부
    private String errorMessage;
}
