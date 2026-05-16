package com.groupaccountingsystem.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class Report {
    private Integer reportID;
    private Integer reporterID;
    private Integer accusedActivityID;
    private String reportReason;
    private Date reportTime;
    private String reportProcessStatus;
}
