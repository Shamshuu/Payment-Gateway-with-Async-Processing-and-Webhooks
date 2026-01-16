package com.gateway.repositories;

import com.gateway.entities.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefundRepository extends JpaRepository<Refund, String> {
    
    // Find all refunds for a specific payment to calculate totals
    List<Refund> findByPaymentId(String paymentId);

    // Helper to sum processed/pending amounts
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.paymentId = :paymentId AND r.status IN ('processed', 'pending')")
    Integer getRefundedAmount(String paymentId);
}