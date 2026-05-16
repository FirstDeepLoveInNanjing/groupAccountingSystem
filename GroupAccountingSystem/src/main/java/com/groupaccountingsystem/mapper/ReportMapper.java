package com.groupaccountingsystem.mapper;

import com.groupaccountingsystem.pojo.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReportMapper {
    int insert(Report report);

    int delete(@Param("reportID") Integer reportID, @Param("reporterID") Integer reporterID);

    int auditReport(@Param("reportID") Integer reportID, @Param("reportProcessStatus") String reportProcessStatus);

    Report selectById(@Param("reportID") Integer reportID);

    List<Report> selectByReporterID(@Param("reporterID") Integer reporterID);

    List<Report> search(@Param("keyword") String keyword, @Param("reportProcessStatus") String reportProcessStatus);
}
