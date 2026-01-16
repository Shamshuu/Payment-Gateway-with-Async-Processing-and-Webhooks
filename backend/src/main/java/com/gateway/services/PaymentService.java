package com.gateway.services;

import com.gateway.dto.PaymentRequest;
import com.gateway.models.Order;
import com.gateway.models.Payment;
import com.gateway.repositories.OrderRepository;
import com.gateway.repositories.PaymentRepository;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.Random;
import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final Random random = new Random();

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    public Payment processPayment(PaymentRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        boolean isSuccess = simulateBankResponse();

        Payment payment = new Payment();
        // Generate String ID
        payment.setId("pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        payment.setOrderId(order.getId());
        payment.setMerchantId(order.getMerchantId());
        payment.setAmount(order.getAmount());
        payment.setCurrency(order.getCurrency());
        payment.setMethod(request.getMethod());
        payment.setStatus(isSuccess ? "success" : "failed");

        paymentRepository.save(payment);

        if (isSuccess) {
            order.setStatus("paid");
            orderRepository.save(order);
        }

        return payment;
    }

    private boolean simulateBankResponse() {
        return random.nextDouble() > 0.1; // 90% success rate
    }

    public Payment getPayment(String id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    public List<Payment> getPaymentsForMerchant(String merchantId) {
        return paymentRepository.findByMerchantId(merchantId);
    }
}