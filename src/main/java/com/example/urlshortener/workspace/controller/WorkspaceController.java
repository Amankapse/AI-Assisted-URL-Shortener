package com.example.urlshortener.workspace.controller;

import com.example.urlshortener.workspace.dto.WorkspaceDtos.AddMemberRequest;
import com.example.urlshortener.workspace.dto.WorkspaceDtos.CreateWorkspaceRequest;
import com.example.urlshortener.workspace.dto.WorkspaceDtos.MemberResponse;
import com.example.urlshortener.workspace.dto.WorkspaceDtos.UpdateMemberRoleRequest;
import com.example.urlshortener.workspace.dto.WorkspaceDtos.WorkspaceResponse;
import com.example.urlshortener.workspace.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {
    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public ResponseEntity<List<WorkspaceResponse>> list() {
        return ResponseEntity.ok(workspaceService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkspaceResponse> get(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(workspaceService.get(id));
    }

    @PostMapping
    public ResponseEntity<WorkspaceResponse> create(@Valid @RequestBody CreateWorkspaceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workspaceService.create(request));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<MemberResponse>> members(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(workspaceService.listMembers(id));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<MemberResponse> addMember(@PathVariable("id") UUID id, @Valid @RequestBody AddMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workspaceService.addMember(id, request));
    }

    @PatchMapping("/{id}/members/{userId}")
    public ResponseEntity<MemberResponse> updateMember(@PathVariable("id") UUID id,
                                                       @PathVariable("userId") UUID userId,
                                                       @Valid @RequestBody UpdateMemberRoleRequest request) {
        return ResponseEntity.ok(workspaceService.updateMember(id, userId, request));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable("id") UUID id, @PathVariable("userId") UUID userId) {
        workspaceService.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }
}
