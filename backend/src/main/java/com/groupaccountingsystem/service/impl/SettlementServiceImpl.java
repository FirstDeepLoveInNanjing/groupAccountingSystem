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
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SettlementServiceImpl implements SettlementService {
    private static final String STATUS_CONFIRMED = "已确认";
    private static final String PAY_STATUS_UNPAID = "未支付";
    private static final String PAY_STATUS_PAID = "已支付";
    private static final String COLLECT_STATUS_UNFINISHED = "未完成";
    private static final String COLLECT_STATUS_FINISHED = "已完成";

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
    public void initSettlement(Integer activityID, String description, Integer initiatorID) {
        GroupActivity activity = groupActivityMapper.selectById(activityID);
        String unavailableReason = getSettlementUnavailableReason(activity, initiatorID);
        if (unavailableReason != null) {
            throw new RuntimeException(unavailableReason);
        }
        performSettlement(activity, description, initiatorID);
    }

    @Override
    @Transactional
    public void initBatchSettlement(List<Integer> activityIDs, String description, Integer initiatorID) {
        if (activityIDs == null || activityIDs.isEmpty()) {
            throw new RuntimeException("请先选择需要批量结算的项目");
        }

        Set<Integer> uniqueActivityIDs = new LinkedHashSet<>(activityIDs);
        List<String> unavailableItems = new ArrayList<>();
        List<GroupActivity> activities = new ArrayList<>();
        for (Integer activityID : uniqueActivityIDs) {
            GroupActivity activity = groupActivityMapper.selectById(activityID);
            String unavailableReason = getSettlementUnavailableReason(activity, initiatorID);
            if (unavailableReason != null) {
                unavailableItems.add("ID " + activityID + "：" + unavailableReason);
            } else {
                activities.add(activity);
            }
        }

        if (!unavailableItems.isEmpty()) {
            throw new RuntimeException("以下项目目前不可结算：" + String.join("；", unavailableItems));
        }

        String batchDescription = StringUtils.hasText(description) ? description : "批量结算";
        for (GroupActivity activity : activities) {
            performSettlement(activity, batchDescription, initiatorID);
        }
    }

    @Override
    public void fillSettleEligibility(List<GroupActivity> activities, Integer operatorID) {
        if (activities == null) {
            return;
        }
        for (GroupActivity activity : activities) {
            String unavailableReason = getSettlementUnavailableReason(activity, operatorID);
            activity.setCanSettle(unavailableReason == null);
            activity.setSettleUnavailableReason(unavailableReason);
        }
    }

    private String getSettlementUnavailableReason(GroupActivity activity, Integer initiatorID) {
        if (activity == null) {
            return "项目不存在";
        }
        if (initiatorID == null || !activity.getCreatorID().equals(initiatorID)) {
            return "只有项目创建者可以发起结算";
        }
        if (Boolean.TRUE.equals(activity.getIsSettable())) {
            return "该项目已经发起过结算";
        }
        if (!Boolean.TRUE.equals(activity.getConfirmInitiated())) {
            return "请先发起项目确认";
        }
        if (collectMapper.selectByActivityID(activity.getActivityID()) != null) {
            return "该项目已经发起过结算";
        }
        List<ActivityMember> members = activityMemberMapper.selectByActivityID(activity.getActivityID());
        boolean allConfirmed = !members.isEmpty()
                && members.stream().allMatch(member -> STATUS_CONFIRMED.equals(member.getConfirmAttendStatus()));
        if (!allConfirmed) {
            return "需要所有项目成员确认才能发起结算";
        }
        BigDecimal actualAmount = activity.getActualAmount();
        if (actualAmount == null || actualAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return "项目实际金额无效";
        }
        return null;
    }

    private void performSettlement(GroupActivity activity, String description, Integer initiatorID) {
        Integer activityID = activity.getActivityID();
        BigDecimal actualAmount = activity.getActualAmount();
        List<ActivityMember> members = activityMemberMapper.selectByActivityID(activity.getActivityID());
        List<ActivityMember> targetMembers = members.stream()
                .filter(member -> !member.getMemberID().equals(initiatorID))
                .collect(Collectors.toList());
        BigDecimal shareAmount = actualAmount.divide(BigDecimal.valueOf(members.size()), 2, RoundingMode.HALF_UP);
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
        collect.setCollectStatus(targetMembers.isEmpty() ? COLLECT_STATUS_FINISHED : COLLECT_STATUS_UNFINISHED);
        collectMapper.insert(collect);

        for (ActivityMember member : targetMembers) {
            Pay pay = new Pay();
            pay.setActivityID(activityID);
            pay.setUserID(member.getMemberID());
            pay.setPayAmount(shareAmount);
            pay.setPayStatus(PAY_STATUS_UNPAID);
            payMapper.insert(pay);
        }

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
    public void executePayment(Integer payID, Integer userID, String payRemark, String paymentProofPath) {
        executePayment(payID, userID, payRemark, paymentProofPath, "收款码转账");
    }

    @Override
    @Transactional
    public void executePayment(Integer payID, Integer userID, String payRemark, String paymentProofPath, String payMethod) {
        Pay pay = payMapper.selectById(payID);
        if (pay == null || !pay.getUserID().equals(userID)) {
            throw new RuntimeException("付款记录不存在");
        }
        if (!StringUtils.hasText(paymentProofPath)) {
            throw new RuntimeException("请先上传付款记录图片，再确认付款");
        }
        pay.setPayStatus(PAY_STATUS_PAID);
        pay.setPayMethod(StringUtils.hasText(payMethod) ? payMethod : "收款码转账");
        pay.setPayRemark(payRemark);
        pay.setPaymentProofPath(paymentProofPath);
        pay.setPayTime(new Date());
        payMapper.updatePayment(pay);

        List<Pay> pays = payMapper.selectByActivityID(pay.getActivityID());
        boolean allPaid = pays.stream().allMatch(item -> PAY_STATUS_PAID.equals(item.getPayStatus()));
        if (allPaid) {
            collectMapper.updateCollectStatus(pay.getActivityID(), COLLECT_STATUS_FINISHED);
        }
    }
}
