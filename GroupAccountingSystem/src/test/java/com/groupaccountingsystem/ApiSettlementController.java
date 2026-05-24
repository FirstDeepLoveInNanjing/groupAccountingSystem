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
    /*
     * Android 端支付/结算相关 REST 接口集中放在这个 Controller。
     *
     * 它只负责三件事：
     * 1. 从 session 中判断当前 App 用户是谁；
     * 2. 把 Android 传来的参数转换成后端 Service 能处理的数据；
     * 3. 把 Service 的执行结果包装成统一的 ApiResponse 返回给 Android。
     *
     * 真正的业务规则，例如“谁能发起结算”“付款后 collect 什么时候变成已完成”，
     * 都在 SettlementServiceImpl 中处理，Controller 不直接改数据库。
     */
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
        // Android 创建者点击“发起结算”后调用这里。
        // 只允许已登录普通用户发起，是否真的是项目创建者由 Service 再检查。
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        try {
            // 会检查：项目存在、当前用户是创建者、项目未结算、已发起确认、所有成员已确认、金额有效。
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
        // Android 批量结算入口。activityIDs 是多个项目 ID，Service 会逐个校验能否结算。
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
        // Android “我的付款”列表：只查当前登录用户自己的 pay 记录。
        // 前端拿到列表后按 payStatus 分成“未支付”和“已支付”两组展示。
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        return ApiResponse.ok(settlementService.listUserPays(user.getUserID()));
    }

    @GetMapping("/pay")
    public ApiResponse<Pay> pay(Integer payID, HttpSession session) {
        // 轻量版付款详情接口，只返回 pay 本身。
        // 目前 Android 主要使用 /pay-detail，因为付款页面还需要项目和收款方信息。
        if (ApiSession.currentUser(session) == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        Pay pay = settlementService.getPay(payID);
        return pay == null ? ApiResponse.fail(404, "pay not found") : ApiResponse.ok(pay);
    }

    @GetMapping("/pay-detail")
    public ApiResponse<PaymentDetailResponse> payDetail(Integer payID, HttpSession session) {
        // Android 点击某条付款记录后进入付款详情时调用。
        // 返回 pay + activity + creator 三部分：
        // pay：付款金额、状态、备注、凭证路径；
        // activity：付款对应的项目；
        // creator：项目创建者，也就是收款方，里面包含 paymentQrCodePath 收款码路径。
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        Pay pay = settlementService.getPay(payID);
        if (pay == null || !user.getUserID().equals(pay.getUserID())) {
            // 防止 A 用户通过 payID 查看或支付 B 用户的付款记录。
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
    public ApiResponse<Void> finishPay(@RequestParam("payID") Integer payID,
                                       @RequestParam(value = "payRemark", required = false) String payRemark,
                                       @RequestParam("paymentProof") MultipartFile paymentProof,
                                       HttpSession session) {
        // Android 上传付款凭证并确认付款。
        //
        // 请求类型是 multipart/form-data：
        // - payID：要完成的付款记录 ID；
        // - payRemark：付款备注，可为空；
        // - paymentProof：付款凭证图片，必须是 jpg/jpeg/png。
        //
        // 注意：Android 端目前还会传 payMethod，但当前后端没有接收该参数。
        // 真正写入数据库的支付方式在 SettlementServiceImpl.executePayment() 中固定为“收款码转账”。
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        try {
            // 先把图片保存到 uploads/payment-proofs，再把返回的 /uploads/... 路径写入 pay 表。
            String proofPath = imageStorageService.storeImage(paymentProof, "payment-proofs");
            settlementService.executePayment(payID, user.getUserID(), payRemark, proofPath);
            return ApiResponse.ok();
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PostMapping("/payment-finish")
    public ApiResponse<Void> finishPayment(@RequestParam("payID") Integer payID,
                                           @RequestParam(value = "payRemark", required = false) String payRemark,
                                           @RequestParam("paymentProof") MultipartFile paymentProof,
                                           HttpSession session) {
        // 兼容旧版本 Android/接口命名留下的别名，内部仍然走同一套完成付款逻辑。
        return finishPay(payID, payRemark, paymentProof, session);
    }

    public static class PaymentDetailResponse {
        // Android 付款详情页需要的不只是 pay，还要项目名和创建者收款码。
        // 所以这里单独封装一个 DTO，避免 Android 再额外请求多个接口。
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
