package com.groupaccountingsystem.service;

import com.groupaccountingsystem.pojo.ActivityMember;
import com.groupaccountingsystem.pojo.GroupActivity;

import java.util.List;

public interface ActivityService {
    void createActivity(GroupActivity groupActivity, Integer creatorID);

    void updateActivity(GroupActivity groupActivity, Integer operatorID);

    void deleteActivity(Integer activityID, Integer operatorID);

    GroupActivity getActivity(Integer activityID);

    List<GroupActivity> searchActivities(String keyword, Boolean isSettable);

    List<GroupActivity> listMyActivities(Integer memberID);

    List<ActivityMember> listMembers(Integer activityID);

    void addMember(Integer activityID, Integer memberID, Integer operatorID);

    void removeMember(Integer activityID, Integer memberID, Integer operatorID);

    void confirmParticipation(Integer activityID, Integer memberID);

    void applyQuit(Integer activityID, Integer memberID);

    void handleQuitApplication(Integer activityID, Integer memberID, boolean allowQuit, Integer operatorID);
}
