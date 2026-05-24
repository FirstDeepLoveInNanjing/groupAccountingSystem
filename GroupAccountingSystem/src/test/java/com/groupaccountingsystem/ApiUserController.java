package com.groupaccountingsystem.controller;

import com.groupaccountingsystem.pojo.OrdinaryUser;
import com.groupaccountingsystem.service.ImageStorageService;
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
@RequestMapping("/api/user")
public class ApiUserController {
    private final UserAccountService userAccountService;
    private final ImageStorageService imageStorageService;

    public ApiUserController(UserAccountService userAccountService, ImageStorageService imageStorageService) {
        this.userAccountService = userAccountService;
        this.imageStorageService = imageStorageService;
    }

    @GetMapping("/me")
    public ApiResponse<OrdinaryUser> me(HttpSession session) {
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        return ApiResponse.ok(userAccountService.getUserInfo(user.getUserID()));
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
    public ApiResponse<List<OrdinaryUser>> listUsers(HttpSession session) {
        if (ApiSession.currentAdmin(session) == null) {
            return ApiResponse.fail(403, "admin required");
        }
        return ApiResponse.ok(userAccountService.listUsers());
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
    public ApiResponse<OrdinaryUser> uploadPaymentQrCode(@RequestParam("paymentQrCode") MultipartFile paymentQrCode,
                                                         HttpSession session) {
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
        try {
            userAccountService.deleteUser(user.getUserID());
            session.invalidate();
            return ApiResponse.ok();
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
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
}
