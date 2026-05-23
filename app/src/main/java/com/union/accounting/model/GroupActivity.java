package com.union.accounting.model;

import java.math.BigDecimal;

public class GroupActivity {
    public Integer activityID;
    public String activityName;
    public String description;
    public BigDecimal budget;
    public BigDecimal actualAmount;
    public Integer creatorID;
    public String createTime;
    public String settlementTime;
    public Boolean isSettable;
    public Boolean confirmInitiated;
    public String confirmDeadline;
    public Integer maxMember;
    public String creatorName;
    public String currentUserConfirmStatus;
    public Boolean canSettle;
    public String settleUnavailableReason;

    public String titleLine() {
        return "#" + activityID + " " + nullToEmpty(activityName);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
