package com.groupaccountingsystem.mapper;

import com.groupaccountingsystem.pojo.Pay;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface PayMapper {
    int insert(Pay pay);

    int updatePayment(Pay pay);

    Pay selectById(@Param("payID") Integer payID);

    Pay selectByActivityAndUser(@Param("activityID") Integer activityID, @Param("userID") Integer userID);

    List<Pay> selectByActivityID(@Param("activityID") Integer activityID);

    List<Pay> selectByUserID(@Param("userID") Integer userID);

    BigDecimal sumPaidByUserID(@Param("userID") Integer userID);

    BigDecimal sumPaidToActivityCreator(@Param("creatorID") Integer creatorID);

    BigDecimal sumPaidByActivityID(@Param("activityID") Integer activityID);
}
