package com.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.workers.PaymentWorker;
import com.gateway.workers.RefundWorker;
import com.gateway.workers.WebhookWorker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    // --- 1. The Redis Template (This was missing!) ---
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String for Keys
        template.setKeySerializer(new StringRedisSerializer());
        
        // Use JSON for Values (so we can store objects)
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer);
        
        return template;
    }

    // --- 2. The Listener Container ---
    @Bean
    RedisMessageListenerContainer redisContainer(RedisConnectionFactory connectionFactory,
                                                 MessageListenerAdapter paymentListenerAdapter,
                                                 MessageListenerAdapter webhookListenerAdapter,
                                                 MessageListenerAdapter refundListenerAdapter) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        
        // Register all listeners
        container.addMessageListener(paymentListenerAdapter, new PatternTopic("queue:payments"));
        container.addMessageListener(webhookListenerAdapter, new PatternTopic("queue:webhooks"));
        container.addMessageListener(refundListenerAdapter, new PatternTopic("queue:refunds"));
        
        return container;
    }

    // --- 3. The Listener Adapters ---
    @Bean
    MessageListenerAdapter paymentListenerAdapter(PaymentWorker paymentWorker) {
        return new MessageListenerAdapter(paymentWorker, "onMessage");
    }

    @Bean
    MessageListenerAdapter webhookListenerAdapter(WebhookWorker webhookWorker) {
        return new MessageListenerAdapter(webhookWorker, "onMessage");
    }

    @Bean
    MessageListenerAdapter refundListenerAdapter(RefundWorker refundWorker) {
        return new MessageListenerAdapter(refundWorker, "onMessage");
    }
}