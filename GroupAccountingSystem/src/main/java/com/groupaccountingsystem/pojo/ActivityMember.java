package com.groupaccountingsystem.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class ActivityMember {
    private Integer activityID;
    private Integer memberID;
    private String role;
    private String confirmAttendStatus;
    private Date confirmTime;
}
