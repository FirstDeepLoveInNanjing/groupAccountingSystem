package com.groupaccountingsystem.controller;

import com.groupaccountingsystem.pojo.GroupActivity;
import com.groupaccountingsystem.pojo.OrdinaryUser;
import com.groupaccountingsystem.pojo.Pay;
import com.groupaccountingsystem.pojo.Report;
import com.groupaccountingsystem.service.ActivityService;
import com.groupaccountingsystem.service.ImageStorageService;
import com.groupaccountingsystem.service.ReportService;
import com.groupaccountingsystem.service.SettlementService;
import com.groupaccountingsystem.service.UserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Controller
public class SystemController {
    private final UserAccountService userAccountService;
    private final ActivityService activityService;
    private final SettlementService settlementService;
    private final ReportService reportService;
    private final ImageStorageService imageStorageService;

    public SystemController(UserAccountService userAccountService, ActivityService activityService,
                            SettlementService settlementService, ReportService reportService,
                            ImageStorageService imageStorageService) {
        this.userAccountService = userAccountService;
        this.activityService = activityService;
        this.settlementService = settlementService;
        this.reportService = reportService;
        this.imageStorageService = imageStorageService;
    }

    @GetMapping("/api/searchUsers")
    @ResponseBody
    public ApiResponse<List<OrdinaryUser>> apiSearchUsers(String keyword) {
        return ApiResponse.ok(userAccountService.searchUsers(keyword));
    }

    @GetMapping("/manageUser")
    public String manageUser(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/signin";
        model.addAttribute("users", userAccountService.listUsers());
        return "manageUser";
    }

    @GetMapping("/resultSearchUser")
    public String resultSearchUser(String keyword, Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/signin";
        model.addAttribute("keyword", keyword);
        model.addAttribute("users", userAccountService.searchUsers(keyword));
        return "resultSearchUser";
    }

    @PostMapping("/admin/deleteUser")
    public String adminDeleteUser(Integer userID, RedirectAttributes attributes, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/signin";
        try {
            userAccountService.deleteUser(userID);
            attributes.addFlashAttribute("message", "用户已删除");
        } catch (RuntimeException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manageUser";
    }

    @GetMapping("/getUserInfo")
    public String getUserInfo(Model model, HttpSession session) {
        OrdinaryUser user = currentUser(session);
        if (user == null) return "redirect:/signin";
        model.addAttribute("userInfo", userAccountService.getUserInfo(user.getUserID()));
        return "getUserInfo";
    }

    @GetMapping("/PaymentQrCode")
    public String paymentQrCode(Model model, HttpSession session) {
        OrdinaryUser user = currentUser(session);
        if (user == null) return "redirect:/signin";
        model.addAttribute("userInfo", userAccountService.getUserInfo(user.getUserID()));
        return "PaymentQrCode";
    }

    @PostMapping("/PaymentQrCode")
    public String uploadPaymentQrCode(MultipartFile paymentQrCode, HttpSession session, RedirectAttributes attributes) {
        OrdinaryUser user = currentUser(session);
        if (user == null) return "redirect:/signin";
        try {
            String path = imageStorageService.storeImage(paymentQrCode, "payment-qrcodes");
            userAccountService.updatePaymentQrCode(user.getUserID(), path);
            session.setAttribute(PageController.SESSION_USER, userAccountService.getUserInfo(user.getUserID()));
            attributes.addFlashAttribute("message", "收款码已上传");
        } catch (RuntimeException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/PaymentQrCode";
    }

    @GetMapping("/changeUserInfo")
    public String changeUserInfo(Model model, HttpSession session) {
        OrdinaryUser user = currentUser(session);
        if (user == null) return "redirect:/signin";
        model.addAttribute("userInfo", userAccountService.getUserInfo(user.getUserID()));
        return "changeUserInfo";
    }

    @PostMapping("/changeUserInfo")
    public String changeUserInfoSubmit(OrdinaryUser ordinaryUser, HttpSession session, RedirectAttributes attributes) {
        OrdinaryUser user = currentUser(session);
        if (user == null) return "redirect:/signin";
        ordinaryUser.setUserID(user.getUserID());
        userAccountService.changeUserInfo(ordinaryUser);
        session.setAttribute(PageController.SESSION_USER, userAccountService.getUserInfo(user.getUserID()));
        attributes.addFlashAttribute("message", "个人信息已修改");
        return "redirect:/getUserInfo";
    }

    @GetMapping("/changePassword")
    public String changePassword(HttpSession session) {
        return currentUser(session) == null ? "redirect:/signin" : "changePassword";
    }

    @PostMapping("/changePassword")
    public String changePasswordSubmit(String oldPassword, String newPassword, String confirmPassword,
                                       HttpSession session, RedirectAttributes attributes) {
        OrdinaryUser user = currentUser(session);
        if (user == null) return "redirect:/signin";
        try {
            userAccountService.changePassword(user.getUserID(), oldPassword, newPassword, confirmPassword);
            attributes.addFlashAttribute("message", "密码已修改，请重新登录");
            session.invalidate();
            return "redirect:/signin";
        } catch (RuntimeException e) {
            attributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/changePassword";
        }
    }

    @GetMapping("/cancelUser")
    public String cancelUser(HttpSession session) {
        return currentUser(session) == null ? "redirect:/signin" : "cancelUser";
    }

    @PostMapping("/cancelUser")
    public String cancelUserSubmit(HttpSession session) {
        OrdinaryUser user = currentUser(session);
        if (user == null) return "redirect:/signin";
        userAccountService.deleteUser(user.getUserID());
        session.invalidate();
        return "redirect:/signin";
    }

    @GetMapping("/forgetPassword")
    public String forgetPassword() {
        return "forgetPassword";
    }

    @PostMapping("/forgetPassword/sendCode")
    public String sendForgetPasswordCode(String phoneNumber, RedirectAttributes attributes) {
        try {
            userAccountService.sendPasswordResetCode(phoneNumber);
            attributes.addFlashAttribute("message", "SMS verify code has been sent.");
            attributes.addFlashAttribute("phoneNumber", phoneNumber);
        } catch (RuntimeException e) {
            attributes.addFlashAttribute("error", e.getMessage());
            attributes.addFlashAttribute("phoneNumber", phoneNumber);
        }
        return "redirect:/forgetPassword";
    }

    @PostMapping("/forgetPassword")
    public String forgetPasswordSubmit(String phoneNumber, String verifyCode, String newPassword,
                                       String confirmPassword, RedirectAttributes attributes) {
        try {
            userAccountService.resetPasswordByPhoneWithSmsCode(phoneNumber, verifyCode, newPassword, confirmPassword);
            attributes.addFlashAttribute("message", "密码已重置，请登录");
            return "redirect:/signin";
        } catch (RuntimeException e) {
            attributes.addFlashAttribute("error", e.getMessage());
            attributes.addFlashAttribute("phoneNumber", phoneNumber);
            return "redirect:/forgetPassword";
        }
    }

    @GetMapping("/CreateActivity")
    public String createActivity(HttpSession session) {
        return currentUser(session) == null ? "redirect:/signin" : "CreateActivity";
    }

    @PostMapping("/activity/create")
    public String createActivitySubmit(GroupActivity groupActivity,
                                       @RequestParam(required = false) List<String> inviteCodes,
                                       HttpSession session, RedirectAttributes attributes) {
        OrdinaryUser user = currentUser(session);
        if (user == null) return "redirect:/signin";
        try {
            activityService.createActivity(groupActivity, user.getUserID(), inviteCodes);
            attributes.addFlashAttribute("message", "项目已创建");
            return "redirect:/SearchActivity";
        } catch (RuntimeException e) {
            attributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/CreateActivity";
        }
    }

    @GetMapping({"/SearchActivity", "/resultSearchActivity"})
    public String searchActivity(String keyword, Boolean isSettable, Boolean myCreatedUnsettled, Model model, HttpSession session) {
        if (!isLoggedIn(session)) return "redirect:/signin";
        model.addAttribute("keyword", keyword);
        model.addAttribute("isSettable", isSettable);
        model.addAttribute("myCreatedUnsettled", myCreatedUnsettled);
        OrdinaryUser user = currentUser(session);
        model.addAttribute("currentUser", user);
        List<GroupActivity> activities;
        if (user == null) {
            activities = activityService.searchActivities(keyword, isSettable);
        } else {
            if (Boolean.TRUE.equals(myCreatedUnsettled)) {
                activities = activityService.searchCreatedUnsettledActivities(keyword, user.getUserID());
            } else {
                activities = activityService.searchActivitiesForUser(keyword, isSettable, user.getUserID());
            }
            settlementService.fillSettleEligibility(activities, user.getUserID());
            if (Boolean.TRUE.equals(myCreatedUnsettled)) {
                activities.sort(Comparator.comparing((GroupActivity activity) -> Boolean.TRUE.equals(activity.getCanSettle())).reversed()
                        .thenComparing(GroupActivity::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())));
            }
        }
        model.addAttribute("activities", activities);
        return "SearchActivity";
    }

    @GetMapping("/DisplayActivity")
    public String displayActivity(Integer activityID, Model model, HttpSession session) {
        if (!isLoggedIn(session)) return "redirect:/signin";
        GroupActivity activity = activityService.getActivity(activityID);
        OrdinaryUser user = currentUser(session);
        List<com.groupaccountingsystem.pojo.ActivityMember> members = activityService.listMembers(activityID);
        if (user != null && members.stream().noneMatch(member -> user.getUserID().equals(member.getMemberID()))) {
            return "redirect:/SearchActivity";
        }
        model.addAttribute("activity", activity);
        model.addAttribute("members", members);
        model.addAttribute("collect", settlementService.getCollect(activityID));
        model.addAttribute("pays", settlementService.listActivityPays(activityID));
        model.addAttribute("currentUser", user);
        model.addAttribute("isCreator", user != null && activity != null && user.getUserID().equals(activity.getCreatorID()));
        return "DisplayActivity";
    }

    @GetMapping("/EditActivity")
    public String editActivity(Integer activityID, Model model, HttpSession session) {
        if (currentUser(session) == null) return "redirect:/signin";
        model.addAttribute("activity", activityService.getActivity(activityID));
        return "EditActivity";
    }

    @PostMapping("/activity/update")
    public String updateActivity(GroupActivity groupActivity, HttpSession session, RedirectAttributes attributes) {
        OrdinaryUser user = currentUser(session);
        if (user == null) return "redirect:/signin";
        try {
            activityService.updateActivity(groupActivity, user.getUserID());
            attributes.addFlashAttribute("message", "项目信息已修改");
            return "redirect:/DisplayActivity?activityID=" + groupActivity.getActivityID();
        } catch (RuntimeException e) {
            attributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/EditActivity?activityID=" + groupActivity.getActivityID();
        }
    }

    @GetMapping("/EndActivity")
    public String endActivity(Integer activityID, Model model, HttpSession session) {
        if (currentUser(session) == null) return "redirect:/signin";
        model.addAttribute("activity", activityService.getActivity(activityID));
        return "EndActivity";
    }

    @PostMapping("/activity/delete")
    public String deleteActivity(Integer activityID, HttpSession session, RedirectAttributes attributes) {
        OrdinaryUser user = currentUser(session);
        if (user == null) return "redirect:/signin";
        try {
            activityService.deleteActivity(activityID, user.getUserID());
            attributes.addFlashAttribute("message", "未结算项目已删除");
        } catch (RuntimeException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/SearchActivity";
    }

    @GetMapping("/InitConfirmActivity")
    public String initConfirmActivity(Integer activityID, Model model, HttpSession session) {
        if (currentUser(session) == null) return "redirect:/signin";
        model.addAttribute("activity", activityService.getActivity(activityID));
        model.addAttribute("members", activityService.listMembers(activityID));
        return "InitConfirmActivity";
    }

    @PostMapping("/activity/initConfirm")
    public String initiateConfirmation(Integer activityID, HttpSession session, RedirectAttributes attributes) {
        OrdinaryUser user = currentUser(session);
        if (user == null) return "redirect:/signin";
        try {
            activityService.initiateConfirmation(activityID, user.getUserID());
            attributes.addFlashAttribute("message", "已发起项目确认");
        } catch (RuntimeException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/SearchActivity";
    }

    @GetMapping("/ConfirmActivity")
    public String confirmActivity(Model model, HttpSession session) {
        OrdinaryUser user = currentUser(session);
        if (user == null) return "redirect:/signin";
        model.addAttribute("activities", activityService.listMyActivities(user.getUserID()));
        return "ConfirmActivity";
    }

    @PostMapping("/activity/confirm")
    public String confirmParticipation(Integer activityID, HttpSession session, RedirectAttributes attributes) {
        OrdinaryUser user = currentUser(session);
        if (user == null) return "redirect:/signin";
        try {
            activityService.confirmParticipation(activityID, user.getUserID());
            attributes.addFlashAttribute("message", "已确认参与");
        } catch (RuntimeException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/SearchActivity";
    }

    @PostMapping("/activity/applyQuit")
    public String applyQuit(Integer activityID, HttpSession session, RedirectAttributes attributes) {
        OrdinaryUser user = currentUser(session);
        if (user == null) return "redirect:/signin";
        try {
            activityService.applyQuit(activityID, user.getUserID());
            attributes.addFlashAttribute("message", "已提交退出申请");
        } catch (RuntimeException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/SearchActivity";
    }

    @GetMapping({"/AddMember", "/SearchMember", "/RemoveMember", "/ProcessWithdrawApplication"})
    public String memberPage(Integer activityID, Model model, HttpSession session, HttpServletRequest request) {
        if (currentUser(session) == null) return "redirect:/signin";
        model.addAttribute("activity", activityService.getActivity(activityID));
        model.addAttribute("members", activityService.listMembers(activityID));
        return requestTemplateName(request);
    }

    @PostMapping("/activity/addMember")
    public String addMember(Integer activityID, String inviteCode, HttpSession session, RedirectAttributes attributes) {
        OrdinaryUser user = currentUser(session);
        if (user == null) return "redirect:/signin";
        try {
            activityService.addMember(activityID, inviteCode, user.getUserID());
            attributes.addFlashAttribute("message", "成员已添加");
        } catch (RuntimeException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/AddMember?activityID=" + activityID;
    }

    @PostMapping("/activity/removeMember")
    public String removeMember(Integer activityID, Integer memberID, String removeReason,
                               HttpSession session, RedirectAttributes attributes) {
        OrdinaryUser user = currentUser(session);
        if (user == null) return "redirect:/signin";
        try {
            activityService.removeMember(activityID, memberID, user.getUserID(), removeReason);
            attributes.addFlashAttribute("message", "成员已移除");
        } catch (RuntimeException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/RemoveMember?activityID=" + activityID;
    }

    @PostMapping("/activity/handleQuit")
    public String handleQuit(Integer activityID, Integer memberID, boolean allowQuit,
                             HttpSession session, RedirectAttributes attributes) {
        OrdinaryUser user = currentUser(session);
        if (user == null) return "redirect:/signin";
        try {
            activityService.handleQuitApplication(activityID, memberID, allowQuit, user.getUserID());
            attributes.addFlashAttribute("message", "退出申请已处理");
        } catch (RuntimeException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ProcessWithdrawApplication?activityID=" + activityID;
    }

    @GetMapping("/InitSettlement")
    public String initSettlement(Integer activityID, Model model, HttpSession session) {
        if (currentUser(session) == null) return "redirect:/signin";
        model.addAttribute("activity", activityService.getActivity(activityID));
        return "InitSettlement";
    }

    @PostMapping("/settlement/init")
    public String initSettlementSubmit(Integer activityID, String description,
                                       HttpSession session, RedirectAttributes attributes) {
        OrdinaryUser user = currentUser(session);
        if (user == null) return "redirect:/signin";
        try {
            settlementService.initSettlement(activityID, description, user.getUserID());
            attributes.addFlashAttribute("message", "结算已发起");
            return "redirect:/DisplaySettlement?activityID=" + activityID;
        } catch (RuntimeException e) {
            attributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/InitSettlement?activityID=" + activityID;
        }
    }

    @PostMapping("/settlement/batch/prepare")
    public String batchSettlementPrepare(@RequestParam(required = false) List<Integer> activityIDs,
                                         Model model, HttpSession session, RedirectAttributes attributes) {
        OrdinaryUser user = currentUser(session);
        if (user == null) return "redirect:/signin";
        if (activityIDs == null || activityIDs.isEmpty()) {
            attributes.addFlashAttribute("error", "请先选择需要批量结算的项目");
            return "redirect:/SearchActivity?myCreatedUnsettled=true&isSettable=false";
        }

        List<GroupActivity> activities = new ArrayList<>();
        for (Integer activityID : activityIDs) {
            GroupActivity activity = activityService.getActivity(activityID);
            if (activity != null) {
                activities.add(activity);
            }
        }
        settlementService.fillSettleEligibility(activities, user.getUserID());
        model.addAttribute("activities", activities);
        model.addAttribute("activityIDs", activityIDs);
        return "BatchSettlement";
    }

    @PostMapping("/settlement/batch")
    public String batchSettlementSubmit(@RequestParam(required = false) List<Integer> activityIDs,
                                        String description,
                                        HttpSession session, RedirectAttributes attributes) {
        OrdinaryUser user = currentUser(session);
        if (user == null) return "redirect:/signin";
        try {
            settlementService.initBatchSettlement(activityIDs, description, user.getUserID());
            attributes.addFlashAttribute("message", "已批量结算 " + activityIDs.size() + " 个项目");
        } catch (RuntimeException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/SearchActivity?myCreatedUnsettled=true&isSettable=false";
    }

    @GetMapping("/DisplaySettlement")
    public String displaySettlement(Integer activityID, Model model, HttpSession session) {
        if (!isLoggedIn(session)) return "redirect:/signin";
        OrdinaryUser user = currentUser(session);
        if (user != null && activityService.listMembers(activityID).stream().noneMatch(member -> user.getUserID().equals(member.getMemberID()))) {
            return "redirect:/SearchActivity";
        }
        model.addAttribute("activity", activityService.getActivity(activityID));
        model.addAttribute("collect", settlementService.getCollect(activityID));
        model.addAttribute("pays", settlementService.listActivityPays(activityID));
        return "DisplaySettlement";
    }

    @GetMapping("/PaymentInformation")
    public String paymentInformation(Integer payID, Model model, HttpSession session) {
        OrdinaryUser user = currentUser(session);
        if (user == null) return "redirect:/signin";
        model.addAttribute("pay", payID == null ? null : settlementService.getPay(payID));
        model.addAttribute("pays", settlementService.listUserPays(user.getUserID()));
        return "PaymentInformation";
    }

    @GetMapping({"/ChoosePaymentMethod", "/ThirdPartPayment"})
    public String choosePaymentMethod(Integer payID, Model model, HttpSession session, HttpServletRequest request) {
        if (currentUser(session) == null) return "redirect:/signin";
        Pay pay = settlementService.getPay(payID);
        model.addAttribute("pay", pay);
        if (pay != null) {
            GroupActivity activity = activityService.getActivity(pay.getActivityID());
            model.addAttribute("activity", activity);
            if (activity != null) {
                model.addAttribute("creator", userAccountService.getUserInfo(activity.getCreatorID()));
            }
        }
        return requestTemplateName(request);
    }

    @PostMapping("/payment/finish")
    public String paymentFinish(Integer payID, String payRemark, MultipartFile paymentProof,
                                HttpSession session, RedirectAttributes attributes) {
        OrdinaryUser user = currentUser(session);
        if (user == null) return "redirect:/signin";
        try {
            String proofPath = imageStorageService.storeImage(paymentProof, "payment-proofs");
            settlementService.executePayment(payID, user.getUserID(), payRemark, proofPath);
            attributes.addFlashAttribute("message", "付款完成");
            return "redirect:/PaymentFinish?payID=" + payID;
        } catch (RuntimeException e) {
            attributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/ChoosePaymentMethod?payID=" + payID;
        }
    }

    @GetMapping("/PaymentFinish")
    public String paymentFinishPage(Integer payID, Model model, HttpSession session) {
        if (currentUser(session) == null) return "redirect:/signin";
        Pay pay = settlementService.getPay(payID);
        model.addAttribute("pay", pay);
        return "PaymentFinish";
    }

    @GetMapping("/FillReportInformation")
    public String fillReportInformation(Model model, HttpSession session) {
        OrdinaryUser user = currentUser(session);
        if (user == null) return "redirect:/signin";
        model.addAttribute("activities", activityService.listMyActivities(user.getUserID()));
        return "FillReportInformation";
    }

    @PostMapping("/report/publish")
    public String publishReport(Report report, String accusedUserKeyword, HttpSession session, RedirectAttributes attributes) {
        OrdinaryUser user = currentUser(session);
        if (user == null) return "redirect:/signin";
        try {
            reportService.publishReport(report, user.getUserID(), accusedUserKeyword);
            attributes.addFlashAttribute("message", "举报已提交");
            return "redirect:/DisplayReportStatus";
        } catch (RuntimeException e) {
            attributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/FillReportInformation";
        }
    }

    @GetMapping({"/CancelReport", "/DisplayReportStatus"})
    public String userReportPage(Model model, HttpSession session, HttpServletRequest request) {
        OrdinaryUser user = currentUser(session);
        if (user == null) return "redirect:/signin";
        model.addAttribute("reports", reportService.listUserReports(user.getUserID()));
        return requestTemplateName(request);
    }

    @PostMapping("/report/cancel")
    public String cancelReport(Integer reportID, HttpSession session, RedirectAttributes attributes) {
        OrdinaryUser user = currentUser(session);
        if (user == null) return "redirect:/signin";
        reportService.cancelReport(reportID, user.getUserID());
        attributes.addFlashAttribute("message", "待审核举报已撤销");
        return "redirect:/CancelReport";
    }

    @GetMapping({"/auditReport", "/findReport"})
    public String reportAdminPage(String keyword, String reportProcessStatus, Model model, HttpSession session,
                                  HttpServletRequest request) {
        if (!isAdmin(session)) return "redirect:/signin";
        model.addAttribute("reports", reportService.searchReports(keyword, reportProcessStatus));
        model.addAttribute("keyword", keyword);
        model.addAttribute("reportProcessStatus", reportProcessStatus);
        return "findReport";
    }

    @PostMapping("/report/audit")
    public String auditReport(Integer reportID, String reportProcessStatus, String punishmentType,
                              RedirectAttributes attributes, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/signin";
        try {
            reportService.auditReport(reportID, reportProcessStatus, punishmentType);
            attributes.addFlashAttribute("message", "举报已审核");
        } catch (RuntimeException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/findReport";
    }

    private boolean isLoggedIn(HttpSession session) {
        return session.getAttribute(PageController.SESSION_ADMIN) != null || session.getAttribute(PageController.SESSION_USER) != null;
    }

    private boolean isAdmin(HttpSession session) {
        return session.getAttribute(PageController.SESSION_ADMIN) != null;
    }

    private OrdinaryUser currentUser(HttpSession session) {
        return (OrdinaryUser) session.getAttribute(PageController.SESSION_USER);
    }

    private String requestTemplateName(HttpServletRequest request) {
        return request.getServletPath().substring(1);
    }
}
