package com.groupaccountingsystem.service.impl;

import com.groupaccountingsystem.mapper.ActivityMemberMapper;
import com.groupaccountingsystem.mapper.GroupActivityMapper;
import com.groupaccountingsystem.mapper.UserMapper;
import com.groupaccountingsystem.pojo.ActivityMember;
import com.groupaccountingsystem.pojo.GroupActivity;
import com.groupaccountingsystem.service.ActivityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
public class ActivityServiceImpl implements ActivityService {
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
    public void createActivity(GroupActivity groupActivity, Integer creatorID) {
        groupActivity.setCreatorID(creatorID);
        groupActivity.setCreateTime(new Date());
        groupActivity.setActualAmount(BigDecimal.ZERO);
        groupActivity.setIsSettable(false);
        groupActivityMapper.insert(groupActivity);

        ActivityMember creator = new ActivityMember();
        creator.setActivityID(groupActivity.getActivityID());
        creator.setMemberID(creatorID);
        creator.setRole("创建者");
        creator.setConfirmAttendStatus("已确认");
        creator.setConfirmTime(new Date());
        activityMemberMapper.insert(creator);
    }

    @Override
    public void updateActivity(GroupActivity groupActivity, Integer operatorID) {
        checkCreator(groupActivity.getActivityID(), operatorID);
        groupActivityMapper.update(groupActivity);
    }

    @Override
    public void deleteActivity(Integer activityID, Integer operatorID) {
        checkCreator(activityID, operatorID);
        groupActivityMapper.delete(activityID);
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
    public List<GroupActivity> listMyActivities(Integer memberID) {
        return groupActivityMapper.selectByMemberID(memberID);
    }

    @Override
    public List<ActivityMember> listMembers(Integer activityID) {
        return activityMemberMapper.selectByActivityID(activityID);
    }

    @Override
    public void addMember(Integer activityID, Integer memberID, Integer operatorID) {
        checkCreator(activityID, operatorID);
        if (userMapper.selectById(memberID) == null) {
            throw new RuntimeException("成员不存在");
        }
        if (activityMemberMapper.selectOne(activityID, memberID) != null) {
            throw new RuntimeException("成员已在项目中");
        }
        GroupActivity activity = groupActivityMapper.selectById(activityID);
        int currentCount = activityMemberMapper.selectByActivityID(activityID).size();
        if (activity.getMaxMember() != null && currentCount >= activity.getMaxMember()) {
            throw new RuntimeException("项目成员数已达上限");
        }
        ActivityMember member = new ActivityMember();
        member.setActivityID(activityID);
        member.setMemberID(memberID);
        member.setRole("成员");
        member.setConfirmAttendStatus("未确认");
        activityMemberMapper.insert(member);
    }

    @Override
    public void removeMember(Integer activityID, Integer memberID, Integer operatorID) {
        checkCreator(activityID, operatorID);
        ActivityMember member = activityMemberMapper.selectOne(activityID, memberID);
        if (member != null && "创建者".equals(member.getRole())) {
            throw new RuntimeException("不能移除项目创建者");
        }
        activityMemberMapper.delete(activityID, memberID);
    }

    @Override
    public void confirmParticipation(Integer activityID, Integer memberID) {
        ActivityMember member = getMember(activityID, memberID);
        if (!"未确认".equals(member.getConfirmAttendStatus())) {
            throw new RuntimeException("当前状态不能确认参与");
        }
        activityMemberMapper.updateConfirmAttendStatus(activityID, memberID, "已确认");
    }

    @Override
    public void applyQuit(Integer activityID, Integer memberID) {
        ActivityMember member = getMember(activityID, memberID);
        if ("已确认".equals(member.getConfirmAttendStatus())) {
            throw new RuntimeException("已确认成员不能回到未确认或申请退出状态");
        }
        activityMemberMapper.updateConfirmAttendStatus(activityID, memberID, "申请退出");
    }

    @Override
    public void handleQuitApplication(Integer activityID, Integer memberID, boolean allowQuit, Integer operatorID) {
        checkCreator(activityID, operatorID);
        ActivityMember member = getMember(activityID, memberID);
        if (!"申请退出".equals(member.getConfirmAttendStatus())) {
            throw new RuntimeException("该成员没有退出申请");
        }
        if (allowQuit) {
            activityMemberMapper.delete(activityID, memberID);
        } else {
            activityMemberMapper.updateConfirmAttendStatus(activityID, memberID, "未确认");
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
}
