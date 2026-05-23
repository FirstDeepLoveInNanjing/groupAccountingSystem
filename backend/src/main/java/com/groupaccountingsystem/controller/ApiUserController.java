package com.groupaccountingsystem.controller;

import com.groupaccountingsystem.pojo.GroupActivity;
import com.groupaccountingsystem.pojo.Pay;
import com.groupaccountingsystem.service.ActivityService;
import com.groupaccountingsystem.pojo.OrdinaryUser;
import com.groupaccountingsystem.service.ImageStorageService;
import com.groupaccountingsystem.service.SettlementService;
import com.groupaccountingsystem.service.UserAccountService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/user")
public class ApiUserController {
    private final UserAccountService userAccountService;
    private final ImageStorageService imageStorageService;
    private final ActivityService activityService;
    private final SettlementService settlementService;

    public ApiUserController(UserAccountService userAccountService, ImageStorageService imageStorageService,
                             ActivityService activityService, SettlementService settlementService) {
        this.userAccountService = userAccountService;
        this.imageStorageService = imageStorageService;
        this.activityService = activityService;
        this.settlementService = settlementService;
    }

    @GetMapping("/me")
    public ApiResponse<OrdinaryUser> me(HttpSession session) {
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        return ApiResponse.ok(userAccountService.getUserInfoWithBill(user.getUserID()));
    }

    @GetMapping("/bill")
    public ApiResponse<PersonalBillResponse> bill(HttpSession session) {
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        OrdinaryUser userWithBill = userAccountService.getUserInfoWithBill(user.getUserID());
        PersonalBillResponse response = new PersonalBillResponse();
        response.setTotalIncome(userWithBill == null ? BigDecimal.ZERO : userWithBill.getTotalIncome());
        response.setTotalExpense(userWithBill == null ? BigDecimal.ZERO : userWithBill.getTotalExpense());
        response.setCreateActivityList(activityService.listCreatedActivities(user.getUserID()));
        response.setJoinActivityList(activityService.listMyActivities(user.getUserID()));
        response.setPayList(settlementService.listUserPays(user.getUserID()));
        return ApiResponse.ok(response);
    }

    @GetMapping("/search")
    public ApiResponse<List<OrdinaryUser>> searchUsers(@RequestParam(required = false) String keyword,
                                                       HttpSession session) {
        if (!ApiSession.loggedIn(session)) {
            return ApiResponse.fail(401, "not logged in");
        }
        return ApiResponse.ok(userAccountService.searchUsers(keyword));
    }

    @GetMapping("/list")
    public ApiResponse<List<OrdinaryUser>> listUsers(@RequestParam(required = false) String keyword,
                                                     HttpSession session) {
        if (ApiSession.currentAdmin(session) == null) {
            return ApiResponse.fail(403, "admin required");
        }
        return ApiResponse.ok(userAccountService.listUsers(keyword));
    }

    @PostMapping("/update")
    public ApiResponse<OrdinaryUser> update(OrdinaryUser ordinaryUser, HttpSession session) {
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        try {
            ordinaryUser.setUserID(user.getUserID());
            userAccountService.changeUserInfo(ordinaryUser);
            OrdinaryUser refreshed = userAccountService.getUserInfo(user.getUserID());
            session.setAttribute(ApiSession.SESSION_USER, refreshed);
            return ApiResponse.ok(refreshed);
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PostMapping("/password")
    public ApiResponse<Void> changePassword(String oldPassword, String newPassword, String confirmPassword,
                                            HttpSession session) {
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        try {
            userAccountService.changePassword(user.getUserID(), oldPassword, newPassword, confirmPassword);
            session.invalidate();
            return ApiResponse.ok();
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PostMapping("/payment-qrcode")
    public ApiResponse<OrdinaryUser> uploadPaymentQrCode(MultipartFile paymentQrCode, HttpSession session) {
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        try {
            String path = imageStorageService.storeImage(paymentQrCode, "payment-qrcodes");
            userAccountService.updatePaymentQrCode(user.getUserID(), path);
            OrdinaryUser refreshed = userAccountService.getUserInfo(user.getUserID());
            session.setAttribute(ApiSession.SESSION_USER, refreshed);
            return ApiResponse.ok(refreshed);
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PostMapping("/delete")
    public ApiResponse<Void> deleteCurrent(HttpSession session) {
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        userAccountService.deleteUser(user.getUserID());
        session.invalidate();
        return ApiResponse.ok();
    }

    @PostMapping("/admin/delete")
    public ApiResponse<Void> adminDelete(Integer userID, HttpSession session) {
        if (ApiSession.currentAdmin(session) == null) {
            return ApiResponse.fail(403, "admin required");
        }
        try {
            userAccountService.deleteUser(userID);
            return ApiResponse.ok();
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    public static class PersonalBillResponse {
        private BigDecimal totalIncome;
        private BigDecimal totalExpense;
        private List<GroupActivity> createActivityList;
        private List<GroupActivity> joinActivityList;
        private List<Pay> payList;

        public BigDecimal getTotalIncome() {
            return totalIncome;
        }

        public void setTotalIncome(BigDecimal totalIncome) {
            this.totalIncome = totalIncome;
        }

        public BigDecimal getTotalExpense() {
            return totalExpense;
        }

        public void setTotalExpense(BigDecimal totalExpense) {
            this.totalExpense = totalExpense;
        }

        public List<GroupActivity> getCreateActivityList() {
            return createActivityList;
        }

        public void setCreateActivityList(List<GroupActivity> createActivityList) {
            this.createActivityList = createActivityList;
        }

        public List<GroupActivity> getJoinActivityList() {
            return joinActivityList;
        }

        public void setJoinActivityList(List<GroupActivity> joinActivityList) {
            this.joinActivityList = joinActivityList;
        }

        public List<Pay> getPayList() {
            return payList;
        }

        public void setPayList(List<Pay> payList) {
            this.payList = payList;
        }
    }
}
