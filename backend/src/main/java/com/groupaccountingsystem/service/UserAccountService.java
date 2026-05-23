package com.groupaccountingsystem.service;

import com.groupaccountingsystem.pojo.OrdinaryUser;

import java.util.List;

public interface UserAccountService {
    OrdinaryUser getUserInfo(Integer userID);

    OrdinaryUser getUserInfoWithBill(Integer userID);

    List<OrdinaryUser> searchUsers(String keyword);

    List<OrdinaryUser> listUsers();

    List<OrdinaryUser> listUsers(String keyword);

    void changeUserInfo(OrdinaryUser ordinaryUser);

    void changePassword(Integer userID, String oldPassword, String newPassword, String confirmPassword);

    void sendPasswordResetCode(String phoneNumber);

    void resetPasswordByPhoneWithSmsCode(String phoneNumber, String verifyCode, String newPassword, String confirmPassword);

    void resetPasswordByAccount(String account, String newPassword, String confirmPassword);

    void updatePaymentQrCode(Integer userID, String paymentQrCodePath);

    void deleteUser(Integer userID);
}
