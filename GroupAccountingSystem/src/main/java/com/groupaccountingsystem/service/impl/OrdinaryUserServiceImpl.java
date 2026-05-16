package com.groupaccountingsystem.service.impl;

import com.groupaccountingsystem.mapper.OrdinaryUserMapper;
import com.groupaccountingsystem.pojo.OrdinaryUser;
import com.groupaccountingsystem.service.OrdinaryUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class OrdinaryUserServiceImpl implements OrdinaryUserService {

    @Autowired
    private OrdinaryUserMapper ordinaryUserMapper;
    /*登录*/
    @Override
    public OrdinaryUser ordinaryUserLogin(OrdinaryUser ordinaryUser) {
        return ordinaryUserMapper.selectByAccountAndPassword(ordinaryUser);
    }
    /*注册*/
    @Override
    public void register(OrdinaryUser ordinaryUser, String confirmPassword) {
        // 1. 校验密码一致性
        if (!ordinaryUser.getUserPassword().equals(confirmPassword)) {
            throw new RuntimeException("两次输入的密码不一致");
        }

        // 2. 校验用户名唯一性
        OrdinaryUser existByUserName = ordinaryUserMapper.selectByUserName(ordinaryUser.getUserName());
        if (existByUserName != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 3. 校验手机号唯一性
        OrdinaryUser existByPhone = ordinaryUserMapper.selectByPhoneNumber(ordinaryUser.getPhoneNumber());
        if (existByPhone != null) {
            throw new RuntimeException("手机号已被注册");
        }

        // 4. 校验邮箱唯一性
        OrdinaryUser existByMailbox = ordinaryUserMapper.selectByUserMailbox(ordinaryUser.getUserMailbox());
        if (existByMailbox != null) {
            throw new RuntimeException("邮箱已被注册");
        }

        // 5. 补全其他字段
        ordinaryUser.setUserType("普通用户");
        ordinaryUser.setRegisterTime(new Date());
        // 注意：userID 如果是自增主键，不需要设置；如果是手动生成，请在此处生成（如 UUID）

        // 6. 插入数据库
        ordinaryUserMapper.insert(ordinaryUser);
    }
}