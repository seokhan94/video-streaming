package com.example.upload;

import com.example.message.VideoCompletedMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class VideoCompletedConsumer {

    @RabbitListener(queues = "video.completed")
    public void handleCompleted(VideoCompletedMessage msg) {
        if (msg.isSuccess()) {
            System.out.println("변환 완료: " + msg.getFileName() + " -> " + msg.getOutputDir());
            // DB 업데이트: 상태 = READY, 스트리밍 경로 저장
        } else {
            System.err.println("변환 실패: " + msg.getFileName() + " - " + msg.getErrorMessage());
            // DB 업데이트: 상태 = FAILED
        }
    }
}
