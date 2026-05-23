package com.groupaccountingsystem.mapper;

import com.groupaccountingsystem.pojo.OrdinaryUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    OrdinaryUser selectById(@Param("userID") Integer userID);

    OrdinaryUser selectOrdinaryByAccountKeyword(@Param("keyword") String keyword);

    OrdinaryUser selectByInviteCode(@Param("inviteCode") String inviteCode);

    List<OrdinaryUser> selectAllOrdinaryUsers();

    List<OrdinaryUser> searchUsers(@Param("keyword") String keyword);

    int updateUserInfo(OrdinaryUser ordinaryUser);

    int updatePassword(@Param("userID") Integer userID, @Param("userPassword") String userPassword);

    int updatePaymentQrCode(@Param("userID") Integer userID,
                            @Param("paymentQrCodePath") String paymentQrCodePath);

    int deleteUser(@Param("userID") Integer userID);
}
