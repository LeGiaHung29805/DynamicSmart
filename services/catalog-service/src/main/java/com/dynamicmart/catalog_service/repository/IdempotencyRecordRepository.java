package com.dynamicmart.catalog_service.repository;

import com.dynamicmart.catalog_service.entity.IdempotencyRecord;
import com.dynamicmart.catalog_service.entity.IdempotencyRecordId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, IdempotencyRecordId> {
}
