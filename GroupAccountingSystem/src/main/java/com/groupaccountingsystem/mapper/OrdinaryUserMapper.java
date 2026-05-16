package com.groupaccountingsystem.mapper;

import com.groupaccountingsystem.pojo.OrdinaryUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrdinaryUserMapper {
    /*登录验证*/
    OrdinaryUser selectByAccountAndPassword(OrdinaryUser ordinaryUser);

    /*注册*/
    // 根据用户名查询
    OrdinaryUser selectByUserName(@Param("userName") String userName);

    // 根据手机号查询
    OrdinaryUser selectByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    // 根据邮箱查询
    OrdinaryUser selectByUserMailbox(@Param("userMailbox") String userMailbox);

    // 插入新用户
    int insert(OrdinaryUser ordinaryUser);
}
