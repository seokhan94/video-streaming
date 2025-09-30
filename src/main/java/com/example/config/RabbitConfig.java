package com.example.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    public DirectExchange videoExchange() {
        return new DirectExchange("video.exchange");
    }

    // 완료 메시지 큐
    @Bean
    public Queue videoCompletedQueue() {
        return new Queue("video.completed", true);
    }

    // 바인딩 (video.completed)
    @Bean
    public Binding completedBinding(DirectExchange videoExchange, Queue videoCompletedQueue) {
        return BindingBuilder.bind(videoCompletedQueue)
                .to(videoExchange)
                .with("video.completed");
    }

    @Bean
    public Queue videoJobQueue() {
        return new Queue("video.jobs", true);
    }

    @Bean
    public Binding jobBinding(DirectExchange videoExchange, Queue videoJobQueue) {
        return BindingBuilder.bind(videoJobQueue)
                .to(videoExchange)
                .with("video.jobs");
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
        return rabbitTemplate;
    }

    /**
     * 직렬화(메세지를 JSON 으로 변환하는 Message Converter)
     */
    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
