package com.groupaccountingsystem.controller;

import com.groupaccountingsystem.pojo.OrdinaryUser;
import com.groupaccountingsystem.pojo.Report;
import com.groupaccountingsystem.service.ReportService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/report")
public class ApiReportController {
    private final ReportService reportService;

    public ApiReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/publish")
    public ApiResponse<Void> publish(Report report, String accusedUserKeyword, HttpSession session) {
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        try {
            reportService.publishReport(report, user.getUserID(), accusedUserKeyword);
            return ApiResponse.ok();
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @GetMapping("/my")
    public ApiResponse<List<Report>> myReports(HttpSession session) {
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        return ApiResponse.ok(reportService.listUserReports(user.getUserID()));
    }

    @GetMapping("/warnings")
    public ApiResponse<List<Report>> warnings(HttpSession session) {
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        return ApiResponse.ok(reportService.listWarningReports(user.getUserID()));
    }

    @PostMapping("/cancel")
    public ApiResponse<Void> cancel(Integer reportID, HttpSession session) {
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        try {
            reportService.cancelReport(reportID, user.getUserID());
            return ApiResponse.ok();
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @GetMapping("/admin/search")
    public ApiResponse<List<Report>> adminSearch(String keyword, String reportProcessStatus, HttpSession session) {
        if (ApiSession.currentAdmin(session) == null) {
            return ApiResponse.fail(403, "admin required");
        }
        return ApiResponse.ok(reportService.searchReports(keyword, reportProcessStatus));
    }

    @PostMapping("/admin/audit")
    public ApiResponse<Void> audit(Integer reportID, String reportProcessStatus, String punishmentType,
                                   HttpSession session) {
        if (ApiSession.currentAdmin(session) == null) {
            return ApiResponse.fail(403, "admin required");
        }
        try {
            reportService.auditReport(reportID, reportProcessStatus, punishmentType);
            return ApiResponse.ok();
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }
}
