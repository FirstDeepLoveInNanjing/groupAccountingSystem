package com.groupaccountingsystem.pojo;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class GroupActivity {
    private Integer activityID;
    private String activityName;
    private String description;
    private BigDecimal budget;
    private BigDecimal actualAmount;
    private Integer creatorID;
    private Date createTime;
    private Date settlementTime;
    private Boolean isSettable;
    private Boolean confirmInitiated;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private Date confirmDeadline;
    private Integer maxMember;
    private String creatorName;
    private String currentUserConfirmStatus;
    private Boolean canSettle;
    private String settleUnavailableReason;
}
