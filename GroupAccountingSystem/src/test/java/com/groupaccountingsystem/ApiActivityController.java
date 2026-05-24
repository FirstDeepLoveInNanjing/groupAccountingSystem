package com.groupaccountingsystem.controller;

import com.groupaccountingsystem.pojo.ActivityMember;
import com.groupaccountingsystem.pojo.Collect;
import com.groupaccountingsystem.pojo.GroupActivity;
import com.groupaccountingsystem.pojo.OrdinaryUser;
import com.groupaccountingsystem.pojo.Pay;
import com.groupaccountingsystem.service.ActivityService;
import com.groupaccountingsystem.service.SettlementService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/activity")
public class ApiActivityController {
    private final ActivityService activityService;
    private final SettlementService settlementService;

    public ApiActivityController(ActivityService activityService, SettlementService settlementService) {
        this.activityService = activityService;
        this.settlementService = settlementService;
    }

    @GetMapping("/list")
    public ApiResponse<List<GroupActivity>> list(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Boolean isSettable,
                                                 @RequestParam(required = false) Boolean myCreatedUnsettled,
                                                 HttpSession session) {
        if (!ApiSession.loggedIn(session)) {
            return ApiResponse.fail(401, "not logged in");
        }
        OrdinaryUser user = ApiSession.currentUser(session);
        List<GroupActivity> activities;
        if (user == null) {
            activities = activityService.searchActivities(keyword, isSettable);
        } else if (Boolean.TRUE.equals(myCreatedUnsettled)) {
            activities = activityService.searchCreatedUnsettledActivities(keyword, user.getUserID());
            settlementService.fillSettleEligibility(activities, user.getUserID());
        } else {
            activities = activityService.searchActivitiesForUser(keyword, isSettable, user.getUserID());
            settlementService.fillSettleEligibility(activities, user.getUserID());
        }
        return ApiResponse.ok(activities);
    }

    @GetMapping("/my")
    public ApiResponse<List<GroupActivity>> my(HttpSession session) {
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        return ApiResponse.ok(activityService.listMyActivities(user.getUserID()));
    }

    @GetMapping("/detail")
    public ApiResponse<ActivityDetailResponse> detail(Integer activityID, HttpSession session) {
        if (!ApiSession.loggedIn(session)) {
            return ApiResponse.fail(401, "not logged in");
        }
        GroupActivity activity = activityService.getActivity(activityID);
        if (activity == null) {
            return ApiResponse.fail(404, "activity not found");
        }
        List<ActivityMember> members = activityService.listMembers(activityID);
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user != null && members.stream().noneMatch(member -> user.getUserID().equals(member.getMemberID()))) {
            return ApiResponse.fail(403, "not activity member");
        }
        if (user != null) {
            settlementService.fillSettleEligibility(Collections.singletonList(activity), user.getUserID());
            members.stream()
                    .filter(member -> user.getUserID().equals(member.getMemberID()))
                    .findFirst()
                    .ifPresent(member -> activity.setCurrentUserConfirmStatus(member.getConfirmAttendStatus()));
        }
        ActivityDetailResponse data = new ActivityDetailResponse();
        data.setActivity(activity);
        data.setMembers(members);
        data.setCollect(settlementService.getCollect(activityID));
        data.setPays(settlementService.listActivityPays(activityID));
        data.setIsCreator(user != null && user.getUserID().equals(activity.getCreatorID()));
        return ApiResponse.ok(data);
    }

    @PostMapping("/create")
    public ApiResponse<GroupActivity> create(GroupActivity groupActivity,
                                             @RequestParam(required = false) List<String> inviteCodes,
                                             HttpSession session) {
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        try {
            activityService.createActivity(groupActivity, user.getUserID(), inviteCodes);
            return ApiResponse.ok(groupActivity);
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PostMapping("/update")
    public ApiResponse<Void> update(GroupActivity groupActivity, HttpSession session) {
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        try {
            activityService.updateActivity(groupActivity, user.getUserID());
            return ApiResponse.ok();
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PostMapping("/delete")
    public ApiResponse<Void> delete(Integer activityID, HttpSession session) {
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        try {
            activityService.deleteActivity(activityID, user.getUserID());
            return ApiResponse.ok();
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @GetMapping("/members")
    public ApiResponse<List<ActivityMember>> members(Integer activityID, HttpSession session) {
        if (!ApiSession.loggedIn(session)) {
            return ApiResponse.fail(401, "not logged in");
        }
        return ApiResponse.ok(activityService.listMembers(activityID));
    }

    @PostMapping("/add-member")
    public ApiResponse<Void> addMember(Integer activityID, String inviteCode, HttpSession session) {
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        try {
            activityService.addMember(activityID, inviteCode, user.getUserID());
            return ApiResponse.ok();
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PostMapping("/remove-member")
    public ApiResponse<Void> removeMember(Integer activityID, Integer memberID, String removeReason,
                                          HttpSession session) {
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        try {
            activityService.removeMember(activityID, memberID, user.getUserID(), removeReason);
            return ApiResponse.ok();
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PostMapping("/init-confirm")
    public ApiResponse<Void> initConfirm(Integer activityID, HttpSession session) {
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        try {
            activityService.initiateConfirmation(activityID, user.getUserID());
            return ApiResponse.ok();
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PostMapping("/confirm")
    public ApiResponse<Void> confirm(Integer activityID, HttpSession session) {
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        try {
            activityService.confirmParticipation(activityID, user.getUserID());
            return ApiResponse.ok();
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PostMapping("/apply-quit")
    public ApiResponse<Void> applyQuit(Integer activityID, HttpSession session) {
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        try {
            activityService.applyQuit(activityID, user.getUserID());
            return ApiResponse.ok();
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PostMapping("/handle-quit")
    public ApiResponse<Void> handleQuit(Integer activityID, Integer memberID, boolean allowQuit,
                                        HttpSession session) {
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        try {
            activityService.handleQuitApplication(activityID, memberID, allowQuit, user.getUserID());
            return ApiResponse.ok();
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @GetMapping("/settlement")
    public ApiResponse<ActivitySettlementResponse> settlement(Integer activityID, HttpSession session) {
        if (!ApiSession.loggedIn(session)) {
            return ApiResponse.fail(401, "not logged in");
        }
        Collect collect = settlementService.getCollect(activityID);
        List<Pay> pays = settlementService.listActivityPays(activityID);
        ActivitySettlementResponse data = new ActivitySettlementResponse();
        data.setActivity(activityService.getActivity(activityID));
        data.setCollect(collect);
        data.setPays(pays);
        return ApiResponse.ok(data);
    }

    public static class ActivityDetailResponse {
        private GroupActivity activity;
        private List<ActivityMember> members;
        private Collect collect;
        private List<Pay> pays;
        private Boolean isCreator;

        public GroupActivity getActivity() {
            return activity;
        }

        public void setActivity(GroupActivity activity) {
            this.activity = activity;
        }

        public List<ActivityMember> getMembers() {
            return members;
        }

        public void setMembers(List<ActivityMember> members) {
            this.members = members;
        }

        public Collect getCollect() {
            return collect;
        }

        public void setCollect(Collect collect) {
            this.collect = collect;
        }

        public List<Pay> getPays() {
            return pays;
        }

        public void setPays(List<Pay> pays) {
            this.pays = pays;
        }

        public Boolean getIsCreator() {
            return isCreator;
        }

        public void setIsCreator(Boolean creator) {
            isCreator = creator;
        }
    }

    public static class ActivitySettlementResponse {
        private GroupActivity activity;
        private Collect collect;
        private List<Pay> pays;

        public GroupActivity getActivity() {
            return activity;
        }

        public void setActivity(GroupActivity activity) {
            this.activity = activity;
        }

        public Collect getCollect() {
            return collect;
        }

        public void setCollect(Collect collect) {
            this.collect = collect;
        }

        public List<Pay> getPays() {
            return pays;
        }

        public void setPays(List<Pay> pays) {
            this.pays = pays;
        }
    }
}
