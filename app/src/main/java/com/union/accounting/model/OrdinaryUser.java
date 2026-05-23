package com.union.accounting.model;

public class OrdinaryUser {
    public Integer userID;
    public String userPassword;
    public String userType;
    public String account;
    public String phoneNumber;
    public String userMailbox;
    public String userName;
    public String realName;
    public String idNumber;
    public String gender;
    public String birthday;
    public String registerTime;
    public String paymentQrCodePath;
    public String inviteCode;
    public String totalIncome;
    public String totalExpense;

    public String displayName() {
        if (userName != null && !userName.isEmpty()) {
            return userName;
        }
        return userID == null ? "" : "用户 " + userID;
    }
}
