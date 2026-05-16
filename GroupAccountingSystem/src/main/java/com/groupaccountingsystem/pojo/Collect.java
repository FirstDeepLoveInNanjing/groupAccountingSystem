package com.groupaccountingsystem.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Collect {
    private Integer activityID;
    private BigDecimal amount;
    private String description;
    private Integer initiatorID;
    private String targetUsers;
    private Date createTime;
    private String collectStatus;
}
