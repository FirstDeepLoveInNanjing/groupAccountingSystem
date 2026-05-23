package com.groupaccountingsystem.controller;

import com.groupaccountingsystem.pojo.Administrator;
import com.groupaccountingsystem.pojo.OrdinaryUser;
import com.groupaccountingsystem.pojo.Report;
import com.groupaccountingsystem.service.AdministratorService;
import com.groupaccountingsystem.service.OrdinaryUserService;
import com.groupaccountingsystem.service.ReportService;
import com.groupaccountingsystem.service.UserAccountService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;

@RestController
@RequestMapping("/api")
public class ApiLoginController {
    private final AdministratorService administratorService;
    private final OrdinaryUserService ordinaryUserService;
    private final ReportService reportService;
    private final UserAccountService userAccountService;

    public ApiLoginController(AdministratorService administratorService,
                              OrdinaryUserService ordinaryUserService,
                              ReportService reportService,
                              UserAccountService userAccountService) {
        this.administratorService = administratorService;
        this.ordinaryUserService = ordinaryUserService;
        this.reportService = reportService;
        this.userAccountService = userAccountService;
    }

    @PostMapping("/administratorLogin")
    public ApiResponse<Administrator> administratorLogin(Administrator administrator, HttpSession session) {
        Administrator loggedIn = administratorService.administratorLogin(administrator);
        if (loggedIn == null) {
            return ApiResponse.fail(401, "account or password is wrong");
        }
        session.setAttribute(ApiSession.SESSION_ADMIN, loggedIn);
        session.removeAttribute(ApiSession.SESSION_USER);
        return ApiResponse.ok(loggedIn);
    }

    @PostMapping("/ordinaryUserLogin")
    public ApiResponse<OrdinaryUser> userLogin(OrdinaryUser ordinaryUser, HttpSession session) {
        OrdinaryUser loggedIn = ordinaryUserService.ordinaryUserLogin(ordinaryUser);
        if (loggedIn == null) {
            return ApiResponse.fail(401, "account or password is wrong");
        }
        Report activeBan = reportService.getActiveBanReport(loggedIn.getUserID());
        if (activeBan != null) {
            return ApiResponse.fail(403, buildBanMessage(activeBan));
        }
        session.setAttribute(ApiSession.SESSION_USER, loggedIn);
        session.removeAttribute(ApiSession.SESSION_ADMIN);
        return ApiResponse.ok(loggedIn);
    }

    @PostMapping("/sendSmsLoginCode")
    public ApiResponse<Void> sendSmsLoginCode(@RequestParam String phoneNumber) {
        try {
            ordinaryUserService.sendSmsLoginCode(phoneNumber);
            return ApiResponse.ok();
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PostMapping("/ordinaryUserSmsLogin")
    public ApiResponse<OrdinaryUser> ordinaryUserSmsLogin(@RequestParam String phoneNumber,
                                                          @RequestParam String verifyCode,
                                                          HttpSession session) {
        try {
            OrdinaryUser loggedIn = ordinaryUserService.ordinaryUserLoginBySmsCode(phoneNumber, verifyCode);
            Report activeBan = reportService.getActiveBanReport(loggedIn.getUserID());
            if (activeBan != null) {
                return ApiResponse.fail(403, buildBanMessage(activeBan));
            }
            session.setAttribute(ApiSession.SESSION_USER, loggedIn);
            session.removeAttribute(ApiSession.SESSION_ADMIN);
            return ApiResponse.ok(loggedIn);
        } catch (RuntimeException e) {
            return ApiResponse.fail(401, e.getMessage());
        }
    }

    @PostMapping("/forgetPassword")
    public ApiResponse<Void> forgetPassword(@RequestParam String phoneNumber,
                                            @RequestParam String verifyCode,
                                            @RequestParam String newPassword,
                                            @RequestParam String confirmPassword) {
        try {
            userAccountService.resetPasswordByPhoneWithSmsCode(phoneNumber, verifyCode, newPassword, confirmPassword);
            return ApiResponse.ok();
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpSession session) {
        session.invalidate();
        return ApiResponse.ok();
    }

    private static String buildBanMessage(Report report) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return "This account is banned until " + formatter.format(report.getPunishmentEndTime());
    }
}
