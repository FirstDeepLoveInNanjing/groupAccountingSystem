package com.groupaccountingsystem.pojo;

import lombok.Data;

@Data
public class User {
    private Integer userID;
    private String userPassword;
    private String userType;
    // 登录时用于接收手机号或邮箱，不对应数据库字段。
    private String account;
}
