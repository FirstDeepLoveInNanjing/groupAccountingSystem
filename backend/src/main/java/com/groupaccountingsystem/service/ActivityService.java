package com.groupaccountingsystem.service;

import com.groupaccountingsystem.pojo.ActivityMember;
import com.groupaccountingsystem.pojo.GroupActivity;

import java.util.List;

public interface ActivityService {
    void createActivity(GroupActivity groupActivity, Integer creatorID, List<String> inviteCodes);

    void updateActivity(GroupActivity groupActivity, Integer operatorID);

    void deleteActivity(Integer activityID, Integer operatorID);

    GroupActivity getActivity(Integer activityID);

    List<GroupActivity> searchActivities(String keyword, Boolean isSettable);

    List<GroupActivity> searchActivitiesForUser(String keyword, Boolean isSettable, Integer userID);

    List<GroupActivity> searchCreatedUnsettledActivities(String keyword, Integer creatorID);

    List<GroupActivity> listCreatedActivities(Integer creatorID);

    List<GroupActivity> listMyActivities(Integer memberID);

    List<ActivityMember> listMembers(Integer activityID);

    void addMember(Integer activityID, String inviteCode, Integer operatorID);

    void initiateConfirmation(Integer activityID, Integer operatorID);

    void removeMember(Integer activityID, Integer memberID, Integer operatorID, String removeReason);

    void confirmParticipation(Integer activityID, Integer memberID);

    void applyQuit(Integer activityID, Integer memberID);

    void handleQuitApplication(Integer activityID, Integer memberID, boolean allowQuit, Integer operatorID);
}
