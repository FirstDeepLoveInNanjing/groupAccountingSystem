package com.groupaccountingsystem.controller;

import com.groupaccountingsystem.pojo.GroupActivity;
import com.groupaccountingsystem.pojo.OrdinaryUser;
import com.groupaccountingsystem.pojo.Pay;
import com.groupaccountingsystem.service.ActivityService;
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

import java.util.List;

@RestController
@RequestMapping("/api/settlement")
public class ApiSettlementController {
    private final SettlementService settlementService;
    private final ImageStorageService imageStorageService;
    private final ActivityService activityService;
    private final UserAccountService userAccountService;

    public ApiSettlementController(SettlementService settlementService, ImageStorageService imageStorageService,
                                   ActivityService activityService, UserAccountService userAccountService) {
        this.settlementService = settlementService;
        this.imageStorageService = imageStorageService;
        this.activityService = activityService;
        this.userAccountService = userAccountService;
    }

    @PostMapping("/init")
    public ApiResponse<Void> init(Integer activityID, String description, HttpSession session) {
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        try {
            settlementService.initSettlement(activityID, description, user.getUserID());
            return ApiResponse.ok();
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PostMapping("/batch")
    public ApiResponse<Void> batch(@RequestParam(required = false) List<Integer> activityIDs,
                                   String description,
                                   HttpSession session) {
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        try {
            settlementService.initBatchSettlement(activityIDs, description, user.getUserID());
            return ApiResponse.ok();
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @GetMapping("/my-pays")
    public ApiResponse<List<Pay>> myPays(HttpSession session) {
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        return ApiResponse.ok(settlementService.listUserPays(user.getUserID()));
    }

    @GetMapping("/pay")
    public ApiResponse<Pay> pay(Integer payID, HttpSession session) {
        if (ApiSession.currentUser(session) == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        Pay pay = settlementService.getPay(payID);
        return pay == null ? ApiResponse.fail(404, "pay not found") : ApiResponse.ok(pay);
    }

    @GetMapping("/pay-detail")
    public ApiResponse<PaymentDetailResponse> payDetail(Integer payID, HttpSession session) {
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        Pay pay = settlementService.getPay(payID);
        if (pay == null || !user.getUserID().equals(pay.getUserID())) {
            return ApiResponse.fail(404, "pay not found");
        }
        GroupActivity activity = activityService.getActivity(pay.getActivityID());
        OrdinaryUser creator = activity == null ? null : userAccountService.getUserInfo(activity.getCreatorID());
        PaymentDetailResponse detail = new PaymentDetailResponse();
        detail.setPay(pay);
        detail.setActivity(activity);
        detail.setCreator(creator);
        return ApiResponse.ok(detail);
    }

    @PostMapping("/pay/finish")
    public ApiResponse<Void> finishPay(Integer payID, String payRemark, String payMethod, MultipartFile paymentProof,
                                       HttpSession session) {
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        try {
            String proofPath = imageStorageService.storeImage(paymentProof, "payment-proofs");
            settlementService.executePayment(payID, user.getUserID(), payRemark, proofPath, payMethod);
            return ApiResponse.ok();
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PostMapping("/payment-finish")
    public ApiResponse<Void> finishPayment(Integer payID, String payRemark, String payMethod, MultipartFile paymentProof,
                                           HttpSession session) {
        return finishPay(payID, payRemark, payMethod, paymentProof, session);
    }

    public static class PaymentDetailResponse {
        private Pay pay;
        private GroupActivity activity;
        private OrdinaryUser creator;

        public Pay getPay() {
            return pay;
        }

        public void setPay(Pay pay) {
            this.pay = pay;
        }

        public GroupActivity getActivity() {
            return activity;
        }

        public void setActivity(GroupActivity activity) {
            this.activity = activity;
        }

        public OrdinaryUser getCreator() {
            return creator;
        }

        public void setCreator(OrdinaryUser creator) {
            this.creator = creator;
        }
    }
}
