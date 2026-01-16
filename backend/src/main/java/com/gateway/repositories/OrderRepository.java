package com.gateway.repositories;

import com.gateway.models.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByMerchantId(String merchantId);
}