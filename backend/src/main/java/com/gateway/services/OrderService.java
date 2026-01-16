package com.gateway.services;

import com.gateway.dto.OrderRequest;
import com.gateway.models.Order;
import com.gateway.models.Merchant;
import com.gateway.repositories.OrderRepository;
import com.gateway.repositories.MerchantRepository;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final MerchantRepository merchantRepository;

    public OrderService(OrderRepository orderRepository, MerchantRepository merchantRepository) {
        this.orderRepository = orderRepository;
        this.merchantRepository = merchantRepository;
    }

    public Order createOrder(OrderRequest request, String merchantApiKey) {
        Merchant merchant = merchantRepository.findAll().stream()
                .filter(m -> m.getApiKey().equals(merchantApiKey))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Invalid API Key"));

        Order order = new Order();
        // Generate String ID
        order.setId("order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        order.setAmount(request.getAmount());
        order.setCurrency(request.getCurrency());
        order.setReceipt(request.getReceipt());
        order.setStatus("created");
        // Assign String ID from Merchant
        order.setMerchantId(merchant.getId()); 

        return orderRepository.save(order);
    }

    public Order getOrder(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }
}