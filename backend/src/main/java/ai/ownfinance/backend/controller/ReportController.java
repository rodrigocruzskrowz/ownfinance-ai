package ai.ownfinance.backend.controller;

import ai.ownfinance.backend.dto.MonthlyReportResponse;
import ai.ownfinance.backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/monthly/{year}/{month}")
    public ResponseEntity<MonthlyReportResponse> getMonthlyReport(
            @PathVariable int year,
            @PathVariable int month) {
        return ResponseEntity.ok(reportService.getMonthlyReport(year, month));
    }
}