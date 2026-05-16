package com.groupaccountingsystem.service;

import com.groupaccountingsystem.pojo.Report;

import java.util.List;

public interface ReportService {
    void publishReport(Report report, Integer reporterID);

    void cancelReport(Integer reportID, Integer reporterID);

    Report viewReport(Integer reportID);

    List<Report> listUserReports(Integer reporterID);

    List<Report> searchReports(String keyword, String reportProcessStatus);

    void auditReport(Integer reportID, String reportProcessStatus);
}
