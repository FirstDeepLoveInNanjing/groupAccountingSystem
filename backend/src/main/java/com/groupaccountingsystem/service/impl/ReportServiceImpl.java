package com.groupaccountingsystem.service.impl;

import com.groupaccountingsystem.mapper.ActivityMemberMapper;
import com.groupaccountingsystem.mapper.GroupActivityMapper;
import com.groupaccountingsystem.mapper.ReportMapper;
import com.groupaccountingsystem.mapper.UserMapper;
import com.groupaccountingsystem.pojo.OrdinaryUser;
import com.groupaccountingsystem.pojo.Report;
import com.groupaccountingsystem.service.ReportService;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {
    private final ReportMapper reportMapper;
    private final GroupActivityMapper groupActivityMapper;
    private final ActivityMemberMapper activityMemberMapper;
    private final UserMapper userMapper;

    public ReportServiceImpl(ReportMapper reportMapper, GroupActivityMapper groupActivityMapper,
                             ActivityMemberMapper activityMemberMapper, UserMapper userMapper) {
        this.reportMapper = reportMapper;
        this.groupActivityMapper = groupActivityMapper;
        this.activityMemberMapper = activityMemberMapper;
        this.userMapper = userMapper;
    }

    @Override
    public void publishReport(Report report, Integer reporterID, String accusedUserKeyword) {
        if (groupActivityMapper.selectById(report.getAccusedActivityID()) == null) {
            throw new RuntimeException("被举报项目不存在");
        }
        OrdinaryUser accusedUser = userMapper.selectOrdinaryByAccountKeyword(accusedUserKeyword);
        if (accusedUser == null) {
            throw new RuntimeException("未找到被举报用户，请使用用户ID、手机号或邮箱精确查找");
        }
        if (activityMemberMapper.selectOne(report.getAccusedActivityID(), reporterID) == null) {
            throw new RuntimeException("举报者不属于该项目，不能提交举报");
        }
        if (activityMemberMapper.selectOne(report.getAccusedActivityID(), accusedUser.getUserID()) == null) {
            throw new RuntimeException("被举报者不属于该项目，不能提交举报");
        }

        report.setReporterID(reporterID);
        report.setAccusedUserID(accusedUser.getUserID());
        report.setReportTime(new Date());
        report.setReportProcessStatus("待审核");
        report.setPunishmentType(null);
        report.setPunishmentEndTime(null);
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
    public void auditReport(Integer reportID, String reportProcessStatus, String punishmentType) {
        Report report = reportMapper.selectById(reportID);
        if (report == null) {
            throw new RuntimeException("举报不存在");
        }
        Date punishmentEndTime = null;
        if ("已通过".equals(reportProcessStatus)) {
            if (punishmentType == null || punishmentType.isBlank()) {
                throw new RuntimeException("通过举报时必须选择处罚");
            }
            punishmentEndTime = calculatePunishmentEndTime(punishmentType);
        } else {
            punishmentType = null;
        }
        reportMapper.auditReport(reportID, reportProcessStatus, punishmentType, punishmentEndTime);
    }

    @Override
    public List<Report> listWarningReports(Integer accusedUserID) {
        return reportMapper.selectWarningsByAccusedUserID(accusedUserID);
    }

    @Override
    public Report getActiveBanReport(Integer accusedUserID) {
        return reportMapper.selectActiveBanByAccusedUserID(accusedUserID);
    }

    private Date calculatePunishmentEndTime(String punishmentType) {
        int days;
        switch (punishmentType) {
            case "警告":
                return null;
            case "封号1天":
                days = 1;
                break;
            case "封号7天":
                days = 7;
                break;
            case "封号365天":
                days = 365;
                break;
            default:
                throw new RuntimeException("未知处罚类型");
        }
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, days);
        return calendar.getTime();
    }
}
