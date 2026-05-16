package com.groupaccountingsystem.service;

import com.groupaccountingsystem.pojo.OrdinaryUser;

import java.util.List;

public interface UserAccountService {
    OrdinaryUser getUserInfo(Integer userID);

    List<OrdinaryUser> searchUsers(String keyword);

    List<OrdinaryUser> listUsers();

    void changeUserInfo(OrdinaryUser ordinaryUser);

    void changePassword(Integer userID, String oldPassword, String newPassword, String confirmPassword);

    void resetPasswordByAccount(String account, String newPassword, String confirmPassword);

    void deleteUser(Integer userID);
}
