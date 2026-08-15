package com.silo.notification.controller;

import com.silo.common.response.ApiResponse;
import com.silo.notification.dto.NotificationLogResponse;
import com.silo.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
    @Operation(summary = "View a member's notification history")
    public ResponseEntity<ApiResponse<List<NotificationLogResponse>>> getHistory(
            @Parameter(description = "Member id") @PathVariable UUID memberId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getHistory(memberId)));
    }
}
