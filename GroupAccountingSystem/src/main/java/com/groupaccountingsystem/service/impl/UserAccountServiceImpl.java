package com.groupaccountingsystem.service.impl;

import com.groupaccountingsystem.mapper.OrdinaryUserMapper;
import com.groupaccountingsystem.mapper.UserMapper;
import com.groupaccountingsystem.pojo.OrdinaryUser;
import com.groupaccountingsystem.service.SmsVerifyCodeService;
import com.groupaccountingsystem.service.UserAccountService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserAccountServiceImpl implements UserAccountService {
    private final UserMapper userMapper;
    private final OrdinaryUserMapper ordinaryUserMapper;
    private final SmsVerifyCodeService smsVerifyCodeService;

    public UserAccountServiceImpl(UserMapper userMapper,
                                  OrdinaryUserMapper ordinaryUserMapper,
                                  SmsVerifyCodeService smsVerifyCodeService) {
        this.userMapper = userMapper;
        this.ordinaryUserMapper = ordinaryUserMapper;
        this.smsVerifyCodeService = smsVerifyCodeService;
    }

    @Override
    public OrdinaryUser getUserInfo(Integer userID) {
        return userMapper.selectById(userID);
    }

    @Override
    public List<OrdinaryUser> searchUsers(String keyword) {
        return userMapper.searchUsers(keyword);
    }

    @Override
    public List<OrdinaryUser> listUsers() {
        return userMapper.selectAllOrdinaryUsers();
    }

    @Override
    public void changeUserInfo(OrdinaryUser ordinaryUser) {
        userMapper.updateUserInfo(ordinaryUser);
    }

    @Override
    public void changePassword(Integer userID, String oldPassword, String newPassword, String confirmPassword) {
        OrdinaryUser user = userMapper.selectById(userID);
        if (user == null || !user.getUserPassword().equals(oldPassword)) {
            throw new RuntimeException("原密码不正确");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("两次输入的新密码不一致");
        }
        userMapper.updatePassword(userID, newPassword);
    }

    @Override
    public void sendPasswordResetCode(String phoneNumber) {
        OrdinaryUser user = ordinaryUserMapper.selectByPhoneNumber(phoneNumber);
        if (user == null) {
            throw new RuntimeException("Phone number is not registered.");
        }
        smsVerifyCodeService.sendCode(phoneNumber);
    }

    @Override
    public void resetPasswordByPhoneWithSmsCode(String phoneNumber, String verifyCode, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("New passwords are different.");
        }
        OrdinaryUser user = ordinaryUserMapper.selectByPhoneNumber(phoneNumber);
        if (user == null) {
            throw new RuntimeException("Phone number is not registered.");
        }
        if (!smsVerifyCodeService.checkCode(phoneNumber, verifyCode)) {
            throw new RuntimeException("Invalid SMS verify code.");
        }
        userMapper.updatePassword(user.getUserID(), newPassword);
    }

    @Override
    public void resetPasswordByAccount(String account, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("两次输入的新密码不一致");
        }
        OrdinaryUser user = ordinaryUserMapper.selectByPhoneNumber(account);
        if (user == null) {
            user = ordinaryUserMapper.selectByUserMailbox(account);
        }
        if (user == null) {
            throw new RuntimeException("未找到该手机号或邮箱对应的用户");
        }
        userMapper.updatePassword(user.getUserID(), newPassword);
    }

    @Override
    public void updatePaymentQrCode(Integer userID, String paymentQrCodePath) {
        userMapper.updatePaymentQrCode(userID, paymentQrCodePath);
    }

    @Override
    public void deleteUser(Integer userID) {
        userMapper.deleteUser(userID);
    }
}
