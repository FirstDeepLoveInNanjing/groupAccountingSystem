package com.groupaccountingsystem.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Pay {
    private Integer payID;
    private Integer activityID;
    private Integer userID;
    private BigDecimal payAmount;
    private String payStatus;
    private String payMethod;
    private Date payTime;
    private String payRemark;
}
