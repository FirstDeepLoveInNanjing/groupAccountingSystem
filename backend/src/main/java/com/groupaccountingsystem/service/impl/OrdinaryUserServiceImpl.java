package com.groupaccountingsystem.service.impl;

import com.groupaccountingsystem.mapper.OrdinaryUserMapper;
import com.groupaccountingsystem.pojo.OrdinaryUser;
import com.groupaccountingsystem.service.OrdinaryUserService;
import com.groupaccountingsystem.service.SmsVerifyCodeService;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Date;

@Service
public class OrdinaryUserServiceImpl implements OrdinaryUserService {
    private static final String INVITE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int INVITE_CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrdinaryUserMapper ordinaryUserMapper;
    private final SmsVerifyCodeService smsVerifyCodeService;

    public OrdinaryUserServiceImpl(OrdinaryUserMapper ordinaryUserMapper,
                                   SmsVerifyCodeService smsVerifyCodeService) {
        this.ordinaryUserMapper = ordinaryUserMapper;
        this.smsVerifyCodeService = smsVerifyCodeService;
    }

    @Override
    public OrdinaryUser ordinaryUserLogin(OrdinaryUser ordinaryUser) {
        return ordinaryUserMapper.selectByAccountAndPassword(ordinaryUser);
    }

    @Override
    public void sendSmsLoginCode(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new RuntimeException("Phone number is required.");
        }
        smsVerifyCodeService.sendCode(phoneNumber);
    }

    @Override
    public OrdinaryUser ordinaryUserLoginBySmsCode(String phoneNumber, String verifyCode) {
        OrdinaryUser user = ordinaryUserMapper.selectByPhoneNumber(phoneNumber);
        if (user == null) {
            throw new RuntimeException("Phone number is not registered.");
        }
        if (!smsVerifyCodeService.checkCode(phoneNumber, verifyCode)) {
            throw new RuntimeException("Invalid SMS verify code.");
        }
        return user;
    }

    @Override
    public void register(OrdinaryUser ordinaryUser, String confirmPassword) {
        registerInternal(ordinaryUser, confirmPassword);
    }

    @Override
    public void register(OrdinaryUser ordinaryUser, String confirmPassword, String verifyCode) {
        if (!smsVerifyCodeService.checkCode(ordinaryUser.getPhoneNumber(), verifyCode)) {
            throw new RuntimeException("Invalid SMS verify code.");
        }
        registerInternal(ordinaryUser, confirmPassword);
    }

    private void registerInternal(OrdinaryUser ordinaryUser, String confirmPassword) {
        if (!ordinaryUser.getUserPassword().equals(confirmPassword)) {
            throw new RuntimeException("Passwords are different.");
        }

        OrdinaryUser existByUserName = ordinaryUserMapper.selectByUserName(ordinaryUser.getUserName());
        if (existByUserName != null) {
            throw new RuntimeException("User name already exists.");
        }

        OrdinaryUser existByPhone = ordinaryUserMapper.selectByPhoneNumber(ordinaryUser.getPhoneNumber());
        if (existByPhone != null) {
            throw new RuntimeException("Phone number already exists.");
        }

        OrdinaryUser existByMailbox = ordinaryUserMapper.selectByUserMailbox(ordinaryUser.getUserMailbox());
        if (existByMailbox != null) {
            throw new RuntimeException("Email already exists.");
        }

        ordinaryUser.setUserType("普通用户");
        ordinaryUser.setRegisterTime(new Date());
        ordinaryUser.setInviteCode(generateUniqueInviteCode());
        ordinaryUserMapper.insert(ordinaryUser);
    }

    private String generateUniqueInviteCode() {
        String code;
        do {
            StringBuilder builder = new StringBuilder(INVITE_CODE_LENGTH);
            for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
                builder.append(INVITE_CODE_CHARS.charAt(RANDOM.nextInt(INVITE_CODE_CHARS.length())));
            }
            code = builder.toString();
        } while (ordinaryUserMapper.selectByInviteCode(code) != null);
        return code;
    }
}
