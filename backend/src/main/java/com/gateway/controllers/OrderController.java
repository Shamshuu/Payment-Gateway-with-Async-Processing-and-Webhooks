package com.gateway.controllers;

import com.gateway.dto.OrderRequest;
import com.gateway.models.Order;
import com.gateway.services.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest request, 
                                         @RequestHeader("X-Api-Key") String apiKey) {
        // Now passing the correct OrderRequest object
        Order order = orderService.createOrder(request, apiKey);
        
        return ResponseEntity.ok(Map.of(
            "id", order.getId(),
            "amount", order.getAmount(),
            "currency", order.getCurrency(),
            "status", order.getStatus(),
            "receipt", order.getReceipt() == null ? "" : order.getReceipt()
        ));
    }

    @GetMapping("/{id}/public")
    public ResponseEntity<?> getOrderPublic(@PathVariable String id) {
        Order order = orderService.getOrder(id);
        return ResponseEntity.ok(Map.of(
            "id", order.getId(),
            "amount", order.getAmount(),
            "currency", order.getCurrency()
        ));
    }
}