package com.groupaccountingsystem.service.impl;

import com.groupaccountingsystem.mapper.ActivityMemberMapper;
import com.groupaccountingsystem.mapper.GroupActivityMapper;
import com.groupaccountingsystem.mapper.UserMapper;
import com.groupaccountingsystem.pojo.ActivityMember;
import com.groupaccountingsystem.pojo.GroupActivity;
import com.groupaccountingsystem.pojo.OrdinaryUser;
import com.groupaccountingsystem.service.ActivityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ActivityServiceImpl implements ActivityService {
    private static final String ROLE_CREATOR = "创建者";
    private static final String ROLE_MEMBER = "成员";
    private static final String STATUS_CONFIRMED = "已确认";
    private static final String STATUS_UNCONFIRMED = "未确认";
    private static final String STATUS_QUIT_APPLY = "申请退出";

    private final GroupActivityMapper groupActivityMapper;
    private final ActivityMemberMapper activityMemberMapper;
    private final UserMapper userMapper;

    public ActivityServiceImpl(GroupActivityMapper groupActivityMapper, ActivityMemberMapper activityMemberMapper, UserMapper userMapper) {
        this.groupActivityMapper = groupActivityMapper;
        this.activityMemberMapper = activityMemberMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public void createActivity(GroupActivity groupActivity, Integer creatorID, List<String> inviteCodes) {
        OrdinaryUser creatorUser = userMapper.selectById(creatorID);
        if (creatorUser == null || !StringUtils.hasText(creatorUser.getPaymentQrCodePath())) {
            throw new RuntimeException("请先在个人信息中上传收款码，再创建项目");
        }
        if (groupActivity.getActualAmount() == null || groupActivity.getActualAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("创建项目时必须填写大于0的实际金额");
        }
        groupActivity.setCreatorID(creatorID);
        groupActivity.setCreateTime(new Date());
        groupActivity.setBudget(groupActivity.getActualAmount());
        groupActivity.setIsSettable(false);
        groupActivity.setConfirmInitiated(false);
        groupActivityMapper.insert(groupActivity);

        ActivityMember creator = new ActivityMember();
        creator.setActivityID(groupActivity.getActivityID());
        creator.setMemberID(creatorID);
        creator.setRole(ROLE_CREATOR);
        creator.setConfirmAttendStatus(STATUS_CONFIRMED);
        creator.setConfirmTime(new Date());
        activityMemberMapper.insert(creator);

        if (inviteCodes != null) {
            for (String inviteCode : inviteCodes) {
                OrdinaryUser invitedUser = findUserByInviteCode(inviteCode);
                if (!invitedUser.getUserID().equals(creatorID)
                        && activityMemberMapper.selectOne(groupActivity.getActivityID(), invitedUser.getUserID()) == null) {
                    insertNormalMember(groupActivity.getActivityID(), invitedUser.getUserID());
                }
            }
        }
    }

    @Override
    public void updateActivity(GroupActivity groupActivity, Integer operatorID) {
        checkCreator(groupActivity.getActivityID(), operatorID);
        GroupActivity existing = groupActivityMapper.selectById(groupActivity.getActivityID());
        if (existing == null) {
            throw new RuntimeException("项目不存在");
        }
        if (Boolean.TRUE.equals(existing.getConfirmInitiated())) {
            groupActivity.setActualAmount(existing.getActualAmount());
        } else if (groupActivity.getActualAmount() == null || groupActivity.getActualAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("实际金额必须大于0");
        }
        groupActivity.setBudget(groupActivity.getActualAmount());
        groupActivityMapper.update(groupActivity);
    }

    @Override
    public void deleteActivity(Integer activityID, Integer operatorID) {
        GroupActivity activity = groupActivityMapper.selectById(activityID);
        if (activity == null) {
            throw new RuntimeException("项目不存在");
        }
        if (!activity.getCreatorID().equals(operatorID)) {
            throw new RuntimeException("只有项目创建者可以执行该操作");
        }
        if (Boolean.TRUE.equals(activity.getIsSettable())) {
            throw new RuntimeException("已结算项目不能删除");
        }
        int deletedCount = groupActivityMapper.delete(activityID);
        if (deletedCount == 0) {
            throw new RuntimeException("项目删除失败，请刷新后重试");
        }
    }

    @Override
    public GroupActivity getActivity(Integer activityID) {
        return groupActivityMapper.selectById(activityID);
    }

    @Override
    public List<GroupActivity> searchActivities(String keyword, Boolean isSettable) {
        return groupActivityMapper.search(keyword, isSettable);
    }

    @Override
    public List<GroupActivity> searchActivitiesForUser(String keyword, Boolean isSettable, Integer userID) {
        return groupActivityMapper.searchByUser(keyword, isSettable, userID);
    }

    @Override
    public List<GroupActivity> searchCreatedUnsettledActivities(String keyword, Integer creatorID) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : null;
        return groupActivityMapper.selectByCreatorID(creatorID).stream()
                .filter(activity -> !Boolean.TRUE.equals(activity.getIsSettable()))
                .filter(activity -> normalizedKeyword == null || matchesKeyword(activity, normalizedKeyword))
                .collect(Collectors.toList());
    }

    @Override
    public List<GroupActivity> listMyActivities(Integer memberID) {
        return groupActivityMapper.selectByMemberID(memberID);
    }

    @Override
    public List<ActivityMember> listMembers(Integer activityID) {
        return activityMemberMapper.selectByActivityID(activityID);
    }

    @Override
    public void addMember(Integer activityID, String inviteCode, Integer operatorID) {
        checkCreator(activityID, operatorID);
        checkDirectMemberChangeAllowed(activityID, "创建者已发起确认，不能再自行添加成员");
        OrdinaryUser invitedUser = findUserByInviteCode(inviteCode);
        Integer memberID = invitedUser.getUserID();
        if (activityMemberMapper.selectOne(activityID, memberID) != null) {
            throw new RuntimeException("成员已在项目中");
        }
        GroupActivity activity = groupActivityMapper.selectById(activityID);
        int currentCount = activityMemberMapper.selectByActivityID(activityID).size();
        if (activity.getMaxMember() != null && currentCount >= activity.getMaxMember()) {
            throw new RuntimeException("项目成员数已达上限");
        }
        insertNormalMember(activityID, memberID);
    }

    @Override
    public void initiateConfirmation(Integer activityID, Integer operatorID) {
        checkCreator(activityID, operatorID);
        GroupActivity activity = groupActivityMapper.selectById(activityID);
        if (activity == null) {
            throw new RuntimeException("项目不存在");
        }
        if (Boolean.TRUE.equals(activity.getIsSettable())) {
            throw new RuntimeException("已结算项目不能再次发起确认");
        }
        if (activity.getActualAmount() == null || activity.getActualAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("发起确认前必须填写大于0的实际金额");
        }
        groupActivityMapper.updateConfirmInitiated(activityID, true);
    }

    @Override
    public void removeMember(Integer activityID, Integer memberID, Integer operatorID, String removeReason) {
        checkCreator(activityID, operatorID);
        checkDirectMemberChangeAllowed(activityID, "创建者已发起确认，不能再自行移除成员");
        ActivityMember member = activityMemberMapper.selectOne(activityID, memberID);
        if (member == null) {
            throw new RuntimeException("成员不存在");
        }
        if (ROLE_CREATOR.equals(member.getRole())) {
            throw new RuntimeException("不能移除项目创建者");
        }
        activityMemberMapper.delete(activityID, memberID);
    }

    @Override
    public void confirmParticipation(Integer activityID, Integer memberID) {
        GroupActivity activity = groupActivityMapper.selectById(activityID);
        if (activity == null) {
            throw new RuntimeException("项目不存在");
        }
        if (!Boolean.TRUE.equals(activity.getConfirmInitiated())) {
            throw new RuntimeException("创建者未发起确认");
        }
        ActivityMember member = getMember(activityID, memberID);
        if (!STATUS_UNCONFIRMED.equals(member.getConfirmAttendStatus())) {
            throw new RuntimeException("当前状态不能确认参与");
        }
        activityMemberMapper.updateConfirmAttendStatus(activityID, memberID, STATUS_CONFIRMED);
    }

    @Override
    public void applyQuit(Integer activityID, Integer memberID) {
        ActivityMember member = getMember(activityID, memberID);
        if (STATUS_CONFIRMED.equals(member.getConfirmAttendStatus())) {
            throw new RuntimeException("已确认成员不能申请退出");
        }
        activityMemberMapper.updateConfirmAttendStatus(activityID, memberID, STATUS_QUIT_APPLY);
    }

    @Override
    public void handleQuitApplication(Integer activityID, Integer memberID, boolean allowQuit, Integer operatorID) {
        checkCreator(activityID, operatorID);
        ActivityMember member = getMember(activityID, memberID);
        if (!STATUS_QUIT_APPLY.equals(member.getConfirmAttendStatus())) {
            throw new RuntimeException("该成员没有退出申请");
        }
        if (allowQuit) {
            activityMemberMapper.delete(activityID, memberID);
        } else {
            activityMemberMapper.updateConfirmAttendStatus(activityID, memberID, STATUS_UNCONFIRMED);
        }
    }

    private void checkCreator(Integer activityID, Integer operatorID) {
        GroupActivity activity = groupActivityMapper.selectById(activityID);
        if (activity == null || !activity.getCreatorID().equals(operatorID)) {
            throw new RuntimeException("只有项目创建者可以执行该操作");
        }
    }

    private ActivityMember getMember(Integer activityID, Integer memberID) {
        ActivityMember member = activityMemberMapper.selectOne(activityID, memberID);
        if (member == null) {
            throw new RuntimeException("项目成员不存在");
        }
        return member;
    }

    private void checkDirectMemberChangeAllowed(Integer activityID, String message) {
        GroupActivity activity = groupActivityMapper.selectById(activityID);
        if (activity == null) {
            throw new RuntimeException("项目不存在");
        }
        if (Boolean.TRUE.equals(activity.getConfirmInitiated())) {
            throw new RuntimeException(message);
        }
    }

    private OrdinaryUser findUserByInviteCode(String inviteCode) {
        if (!StringUtils.hasText(inviteCode)) {
            throw new RuntimeException("请输入成员邀请码");
        }
        OrdinaryUser user = userMapper.selectByInviteCode(inviteCode.trim());
        if (user == null) {
            throw new RuntimeException("邀请码对应的用户不存在");
        }
        return user;
    }

    private void insertNormalMember(Integer activityID, Integer memberID) {
        ActivityMember member = new ActivityMember();
        member.setActivityID(activityID);
        member.setMemberID(memberID);
        member.setRole(ROLE_MEMBER);
        member.setConfirmAttendStatus(STATUS_UNCONFIRMED);
        activityMemberMapper.insert(member);
    }

    private boolean matchesKeyword(GroupActivity activity, String keyword) {
        return String.valueOf(activity.getActivityID()).contains(keyword)
                || containsIgnoreCase(activity.getActivityName(), keyword)
                || containsIgnoreCase(activity.getDescription(), keyword);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }
}
