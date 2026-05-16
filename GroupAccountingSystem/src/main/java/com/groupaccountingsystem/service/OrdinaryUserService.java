package com.groupaccountingsystem.service;

import com.groupaccountingsystem.pojo.OrdinaryUser;

public interface OrdinaryUserService {
    // 普通用户登录
    OrdinaryUser ordinaryUserLogin(OrdinaryUser ordinaryUser);

    // 普通用户注册
    void register(OrdinaryUser ordinaryUser, String confirmPassword);
}