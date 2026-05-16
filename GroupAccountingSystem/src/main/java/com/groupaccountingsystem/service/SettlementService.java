package com.groupaccountingsystem.service;

import com.groupaccountingsystem.pojo.Collect;
import com.groupaccountingsystem.pojo.Pay;

import java.math.BigDecimal;
import java.util.List;

public interface SettlementService {
    void initSettlement(Integer activityID, BigDecimal actualAmount, String description, Integer initiatorID);

    Collect getCollect(Integer activityID);

    List<Pay> listActivityPays(Integer activityID);

    List<Pay> listUserPays(Integer userID);

    Pay getPay(Integer payID);

    void executePayment(Integer payID, Integer userID, String payMethod, String payRemark);
}
