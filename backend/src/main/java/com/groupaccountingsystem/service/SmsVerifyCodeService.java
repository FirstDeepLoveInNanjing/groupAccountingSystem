package com.groupaccountingsystem.service;

public interface SmsVerifyCodeService {
    void sendCode(String phoneNumber);

    boolean checkCode(String phoneNumber, String verifyCode);
}
