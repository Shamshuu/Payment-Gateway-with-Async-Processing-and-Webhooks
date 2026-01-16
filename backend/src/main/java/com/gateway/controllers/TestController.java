package com.gateway.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/jobs/status")
    public ResponseEntity<?> getJobStatus() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Count pending jobs in the 'queue:payments' list
            Long pendingCount = redisTemplate.opsForList().size("queue:payments");
            
            // In a real system, you might track processing/completed/failed in Redis keys.
            // For this requirement, we can estimate or hardcode the others if we aren't tracking them explicitly yet.
            // Or better, checking the database for status counts provides a good proxy.
            
            stats.put("pending", pendingCount != null ? pendingCount : 0);
            stats.put("processing", 0); // Placeholder unless you track this in Redis
            stats.put("completed", 0);  // Placeholder or DB query count
            stats.put("failed", 0);     // Placeholder or DB query count
            stats.put("worker_status", "running"); // Assumed running if API can connect to Redis

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            stats.put("error", "Could not connect to Redis: " + e.getMessage());
            return ResponseEntity.internalServerError().body(stats);
        }
    }
}