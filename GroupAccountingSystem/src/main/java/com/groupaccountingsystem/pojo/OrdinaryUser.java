package com.groupaccountingsystem.pojo;
import lombok.Data;

import java.util.Date;

@Data
public class OrdinaryUser extends User {
    private String phoneNumber;
    private String userMailbox;
    private String userName;
    private String realName;
    private String idNumber;
    private String gender;
    private String birthday;
    private Date registerTime;
    private String paymentQrCodePath;
    private String inviteCode;
}
