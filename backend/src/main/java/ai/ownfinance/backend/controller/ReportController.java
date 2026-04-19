package ai.ownfinance.backend.controller;

import ai.ownfinance.backend.dto.MonthlyReportResponse;
import ai.ownfinance.backend.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Generate financial reports")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "Get Monthly Report", description = "Generates a monthly financial report for the specified year and month.")
    @GetMapping("/monthly/{year}/{month}")
    public ResponseEntity<MonthlyReportResponse> getMonthlyReport(
            @PathVariable int year,
            @PathVariable int month) {
        return ResponseEntity.ok(reportService.getMonthlyReport(year, month));
    }
}