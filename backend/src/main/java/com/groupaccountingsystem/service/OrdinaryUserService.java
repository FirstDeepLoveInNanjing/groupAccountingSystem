package com.groupaccountingsystem.service;

import com.groupaccountingsystem.pojo.OrdinaryUser;

public interface OrdinaryUserService {
    OrdinaryUser ordinaryUserLogin(OrdinaryUser ordinaryUser);

    void sendSmsLoginCode(String phoneNumber);

    OrdinaryUser ordinaryUserLoginBySmsCode(String phoneNumber, String verifyCode);

    void register(OrdinaryUser ordinaryUser, String confirmPassword);

    void register(OrdinaryUser ordinaryUser, String confirmPassword, String verifyCode);
}
