package com.groupaccountingsystem.mapper;

import com.groupaccountingsystem.pojo.OrdinaryUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    OrdinaryUser selectById(@Param("userID") Integer userID);

    List<OrdinaryUser> selectAllOrdinaryUsers();

    List<OrdinaryUser> searchUsers(@Param("keyword") String keyword);

    int updateUserInfo(OrdinaryUser ordinaryUser);

    int updatePassword(@Param("userID") Integer userID, @Param("userPassword") String userPassword);

    int deleteUser(@Param("userID") Integer userID);
}
