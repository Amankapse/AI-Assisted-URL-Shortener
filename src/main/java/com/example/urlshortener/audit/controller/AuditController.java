package com.example.urlshortener.audit.controller;

import com.example.urlshortener.audit.dto.AuditDtos.AuditEventResponse;
import com.example.urlshortener.audit.entity.AuditAction;
import com.example.urlshortener.audit.entity.AuditActorType;
import com.example.urlshortener.audit.entity.AuditResourceType;
import com.example.urlshortener.audit.service.AuditQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@Tag(name = "Audit Trail")
@RestController
@RequestMapping("/api/v1")
@Validated
public class AuditController {
    private final AuditQueryService auditQueryService;

    public AuditController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @Operation(summary = "List workspace audit events")
    @GetMapping("/workspaces/{workspaceId}/audit")
    public ResponseEntity<Page<AuditEventResponse>> workspaceAudit(
            @PathVariable("workspaceId") UUID workspaceId,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(name = "action", required = false) AuditAction action,
            @RequestParam(name = "resourceType", required = false) AuditResourceType resourceType,
            @RequestParam(name = "actorType", required = false) AuditActorType actorType,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(auditQueryService.workspaceEvents(workspaceId, from, to, action, resourceType, actorType, page, size));
    }

    @Operation(summary = "List audit events for a URL")
    @GetMapping("/urls/{urlId}/audit")
    public ResponseEntity<Page<AuditEventResponse>> urlAudit(
            @PathVariable("urlId") UUID urlId,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(name = "action", required = false) AuditAction action,
            @RequestParam(name = "actorType", required = false) AuditActorType actorType,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(auditQueryService.urlEvents(urlId, from, to, action, actorType, page, size));
    }

    @Operation(summary = "List platform audit events")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/audit")
    public ResponseEntity<Page<AuditEventResponse>> adminAudit(
            @RequestParam(name = "workspaceId", required = false) UUID workspaceId,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(name = "action", required = false) AuditAction action,
            @RequestParam(name = "resourceType", required = false) AuditResourceType resourceType,
            @RequestParam(name = "actorType", required = false) AuditActorType actorType,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(auditQueryService.adminEvents(workspaceId, from, to, action, resourceType, actorType, page, size));
    }
}
