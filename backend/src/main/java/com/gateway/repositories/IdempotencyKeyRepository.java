package com.gateway.repositories;

import com.gateway.entities.IdempotencyKey;
import com.gateway.entities.IdempotencyKeyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, IdempotencyKeyId> {
}