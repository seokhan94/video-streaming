package com.example.message;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoResolutionOption {
    private String resolution;  // 예: "1920:-2"
    private String bitrate;     // 예: "5000k"
    private String label;       // 예: "1080p"
}
