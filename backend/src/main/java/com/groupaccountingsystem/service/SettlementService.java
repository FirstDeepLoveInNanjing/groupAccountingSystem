package com.groupaccountingsystem.service;

import com.groupaccountingsystem.pojo.Collect;
import com.groupaccountingsystem.pojo.GroupActivity;
import com.groupaccountingsystem.pojo.Pay;

import java.util.List;

public interface SettlementService {
    void initSettlement(Integer activityID, String description, Integer initiatorID);

    void initBatchSettlement(List<Integer> activityIDs, String description, Integer initiatorID);

    void fillSettleEligibility(List<GroupActivity> activities, Integer operatorID);

    Collect getCollect(Integer activityID);

    List<Pay> listActivityPays(Integer activityID);

    List<Pay> listUserPays(Integer userID);

    Pay getPay(Integer payID);

    void executePayment(Integer payID, Integer userID, String payRemark, String paymentProofPath);

    void executePayment(Integer payID, Integer userID, String payRemark, String paymentProofPath, String payMethod);
}
