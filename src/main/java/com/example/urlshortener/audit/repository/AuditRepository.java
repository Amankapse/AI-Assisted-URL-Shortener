package com.example.urlshortener.audit.repository;

import com.example.urlshortener.audit.entity.AuditAction;
import com.example.urlshortener.audit.entity.AuditActorType;
import com.example.urlshortener.audit.entity.AuditEventEntity;
import com.example.urlshortener.audit.entity.AuditResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface AuditRepository extends JpaRepository<AuditEventEntity, UUID>, JpaSpecificationExecutor<AuditEventEntity> {
    long countByAction(AuditAction action);
}
