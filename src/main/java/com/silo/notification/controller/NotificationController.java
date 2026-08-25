package com.silo.notification.controller;

import com.silo.common.response.ApiResponse;
import com.silo.notification.dto.NotificationLogResponse;
import com.silo.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "A member's notification history")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/member/{memberId}")
    @PreAuthorize("hasRole('OFFICER') or #memberId.toString() == authentication.name")
    @Operation(
            summary = "View a member's notification history",
            description = "Restricted to the member themselves or an officer.")
    public ResponseEntity<ApiResponse<List<NotificationLogResponse>>> getHistory(
            @Parameter(description = "Member id") @PathVariable UUID memberId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getHistory(memberId)));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read", description = "Restricted to the notification's owner.")
    public ResponseEntity<ApiResponse<NotificationLogResponse>> markAsRead(
            @Parameter(description = "Notification id") @PathVariable UUID id,
            Authentication authentication) {
        UUID callerId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(notificationService.markAsRead(id, callerId)));
    }

    @PatchMapping("/member/{memberId}/read-all")
    @PreAuthorize("#memberId.toString() == authentication.name")
    @Operation(summary = "Mark all of a member's notifications as read", description = "Restricted to the member themselves.")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @Parameter(description = "Member id") @PathVariable UUID memberId) {
        notificationService.markAllAsRead(memberId);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }
}
