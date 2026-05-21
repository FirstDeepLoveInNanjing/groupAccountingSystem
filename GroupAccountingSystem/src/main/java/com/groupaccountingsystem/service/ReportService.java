package com.groupaccountingsystem.service;

import com.groupaccountingsystem.pojo.Report;

import java.util.List;

public interface ReportService {
    void publishReport(Report report, Integer reporterID, String accusedUserKeyword);

    void cancelReport(Integer reportID, Integer reporterID);

    Report viewReport(Integer reportID);

    List<Report> listUserReports(Integer reporterID);

    List<Report> searchReports(String keyword, String reportProcessStatus);

    void auditReport(Integer reportID, String reportProcessStatus, String punishmentType);

    List<Report> listWarningReports(Integer accusedUserID);

    Report getActiveBanReport(Integer accusedUserID);
}
