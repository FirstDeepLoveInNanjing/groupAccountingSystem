package com.groupaccountingsystem.service.impl;

import com.groupaccountingsystem.mapper.ActivityMemberMapper;
import com.groupaccountingsystem.mapper.CollectMapper;
import com.groupaccountingsystem.mapper.GroupActivityMapper;
import com.groupaccountingsystem.mapper.PayMapper;
import com.groupaccountingsystem.pojo.ActivityMember;
import com.groupaccountingsystem.pojo.Collect;
import com.groupaccountingsystem.pojo.GroupActivity;
import com.groupaccountingsystem.pojo.Pay;
import com.groupaccountingsystem.service.SettlementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SettlementServiceImpl implements SettlementService {
    private final GroupActivityMapper groupActivityMapper;
    private final ActivityMemberMapper activityMemberMapper;
    private final CollectMapper collectMapper;
    private final PayMapper payMapper;

    public SettlementServiceImpl(GroupActivityMapper groupActivityMapper, ActivityMemberMapper activityMemberMapper,
                                 CollectMapper collectMapper, PayMapper payMapper) {
        this.groupActivityMapper = groupActivityMapper;
        this.activityMemberMapper = activityMemberMapper;
        this.collectMapper = collectMapper;
        this.payMapper = payMapper;
    }

    @Override
    @Transactional
    public void initSettlement(Integer activityID, BigDecimal actualAmount, String description, Integer initiatorID) {
        GroupActivity activity = groupActivityMapper.selectById(activityID);
        if (activity == null || !activity.getCreatorID().equals(initiatorID)) {
            throw new RuntimeException("只有项目创建者可以发起结算");
        }
        if (collectMapper.selectByActivityID(activityID) != null) {
            throw new RuntimeException("该项目已经发起过结算");
        }
        List<ActivityMember> confirmedMembers = activityMemberMapper.selectByActivityID(activityID).stream()
                .filter(member -> "已确认".equals(member.getConfirmAttendStatus()))
                .collect(Collectors.toList());
        if (confirmedMembers.isEmpty()) {
            throw new RuntimeException("没有已确认成员，无法结算");
        }
        List<ActivityMember> targetMembers = confirmedMembers.stream()
                .filter(member -> !member.getMemberID().equals(initiatorID))
                .collect(Collectors.toList());
        if (targetMembers.isEmpty()) {
            targetMembers = confirmedMembers;
        }
        BigDecimal shareAmount = actualAmount.divide(BigDecimal.valueOf(targetMembers.size()), 2, RoundingMode.HALF_UP);
        String targetUsers = targetMembers.stream()
                .map(member -> String.valueOf(member.getMemberID()))
                .collect(Collectors.joining(","));

        Collect collect = new Collect();
        collect.setActivityID(activityID);
        collect.setAmount(actualAmount);
        collect.setDescription(description);
        collect.setInitiatorID(initiatorID);
        collect.setTargetUsers(targetUsers);
        collect.setCreateTime(new Date());
        collect.setCollectStatus("未完成");
        collectMapper.insert(collect);

        for (ActivityMember member : targetMembers) {
            Pay pay = new Pay();
            pay.setActivityID(activityID);
            pay.setUserID(member.getMemberID());
            pay.setPayAmount(shareAmount);
            pay.setPayStatus("未支付");
            payMapper.insert(pay);
        }

        activity.setActualAmount(actualAmount);
        activity.setSettlementTime(new Date());
        activity.setIsSettable(true);
        groupActivityMapper.updateSettlement(activity);
    }

    @Override
    public Collect getCollect(Integer activityID) {
        return collectMapper.selectByActivityID(activityID);
    }

    @Override
    public List<Pay> listActivityPays(Integer activityID) {
        return payMapper.selectByActivityID(activityID);
    }

    @Override
    public List<Pay> listUserPays(Integer userID) {
        return payMapper.selectByUserID(userID);
    }

    @Override
    public Pay getPay(Integer payID) {
        return payMapper.selectById(payID);
    }

    @Override
    @Transactional
    public void executePayment(Integer payID, Integer userID, String payMethod, String payRemark) {
        Pay pay = payMapper.selectById(payID);
        if (pay == null || !pay.getUserID().equals(userID)) {
            throw new RuntimeException("付款记录不存在");
        }
        pay.setPayStatus("已支付");
        pay.setPayMethod(payMethod);
        pay.setPayRemark(payRemark);
        pay.setPayTime(new Date());
        payMapper.updatePayment(pay);

        List<Pay> pays = payMapper.selectByActivityID(pay.getActivityID());
        boolean allPaid = pays.stream().allMatch(item -> "已支付".equals(item.getPayStatus()));
        if (allPaid) {
            collectMapper.updateCollectStatus(pay.getActivityID(), "已完成");
        }
    }
}
