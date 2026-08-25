package com.silo.reporting.controller;

import com.silo.common.response.ApiResponse;
import com.silo.reporting.dto.DashboardResponse;
import com.silo.reporting.dto.MemberReportSummaryResponse;
import com.silo.reporting.dto.TopContributorResponse;
import com.silo.reporting.service.DashboardBroadcastService;
import com.silo.reporting.service.ReportingQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "Read-only dashboard and reporting endpoints")
public class ReportingController {

    private final ReportingQueryService reportingQueryService;
    private final DashboardBroadcastService dashboardBroadcastService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('OFFICER')")
    @Operation(
            summary = "Active loans, total contributions, outstanding balance, and default rate",
            description = "Officer only. Members see their own summary via /members/{id}/summary instead.")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(reportingQueryService.getDashboard()));
    }

    @GetMapping("/members/{id}/summary")
    @PreAuthorize("hasRole('OFFICER') or #id.toString() == authentication.name")
    @Operation(
            summary = "A member's contribution and loan summary",
            description = "Restricted to the member themselves or an officer.")
    public ResponseEntity<ApiResponse<MemberReportSummaryResponse>> getMemberSummary(
            @Parameter(description = "Member id") @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(reportingQueryService.getMemberSummary(id)));
    }

    @GetMapping("/top-contributors")
    @PreAuthorize("hasRole('OFFICER')")
    @Operation(summary = "Members ranked by total contributions", description = "Officer only.")
    public ResponseEntity<ApiResponse<List<TopContributorResponse>>> getTopContributors(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(reportingQueryService.getTopContributors(limit)));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('OFFICER')")
    @Operation(summary = "Server-Sent Events stream of live dashboard updates", description = "Officer only.")
    public SseEmitter stream() {
        return dashboardBroadcastService.register();
    }
}
