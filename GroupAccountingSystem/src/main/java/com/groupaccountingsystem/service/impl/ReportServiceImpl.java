package com.groupaccountingsystem.service.impl;

import com.groupaccountingsystem.mapper.GroupActivityMapper;
import com.groupaccountingsystem.mapper.ReportMapper;
import com.groupaccountingsystem.pojo.Report;
import com.groupaccountingsystem.service.ReportService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {
    private final ReportMapper reportMapper;
    private final GroupActivityMapper groupActivityMapper;

    public ReportServiceImpl(ReportMapper reportMapper, GroupActivityMapper groupActivityMapper) {
        this.reportMapper = reportMapper;
        this.groupActivityMapper = groupActivityMapper;
    }

    @Override
    public void publishReport(Report report, Integer reporterID) {
        if (groupActivityMapper.selectById(report.getAccusedActivityID()) == null) {
            throw new RuntimeException("被举报项目不存在");
        }
        report.setReporterID(reporterID);
        report.setReportTime(new Date());
        report.setReportProcessStatus("待审核");
        reportMapper.insert(report);
    }

    @Override
    public void cancelReport(Integer reportID, Integer reporterID) {
        reportMapper.delete(reportID, reporterID);
    }

    @Override
    public Report viewReport(Integer reportID) {
        return reportMapper.selectById(reportID);
    }

    @Override
    public List<Report> listUserReports(Integer reporterID) {
        return reportMapper.selectByReporterID(reporterID);
    }

    @Override
    public List<Report> searchReports(String keyword, String reportProcessStatus) {
        return reportMapper.search(keyword, reportProcessStatus);
    }

    @Override
    public void auditReport(Integer reportID, String reportProcessStatus) {
        reportMapper.auditReport(reportID, reportProcessStatus);
    }
}
