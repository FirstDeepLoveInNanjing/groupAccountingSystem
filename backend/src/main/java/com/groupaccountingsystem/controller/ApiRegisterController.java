package com.groupaccountingsystem.controller;

import com.groupaccountingsystem.pojo.OrdinaryUser;
import com.groupaccountingsystem.service.OrdinaryUserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApiRegisterController {
    private final OrdinaryUserService ordinaryUserService;

    public ApiRegisterController(OrdinaryUserService ordinaryUserService) {
        this.ordinaryUserService = ordinaryUserService;
    }

    @PostMapping("/signup")
    public ApiResponse<Void> signup(RegisterRequest request) {
        try {
            OrdinaryUser user = new OrdinaryUser();
            user.setUserName(request.getUserName());
            user.setUserPassword(request.getUserPassword());
            user.setPhoneNumber(request.getPhoneNumber());
            user.setUserMailbox(request.getUserMailbox());
            ordinaryUserService.register(user, request.getConfirmPassword(), request.getVerifyCode());
            return ApiResponse.ok();
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    public static class RegisterRequest {
        private String userName;
        private String userPassword;
        private String confirmPassword;
        private String phoneNumber;
        private String userMailbox;
        private String verifyCode;

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getUserPassword() {
            return userPassword;
        }

        public void setUserPassword(String userPassword) {
            this.userPassword = userPassword;
        }

        public String getConfirmPassword() {
            return confirmPassword;
        }

        public void setConfirmPassword(String confirmPassword) {
            this.confirmPassword = confirmPassword;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public String getUserMailbox() {
            return userMailbox;
        }

        public void setUserMailbox(String userMailbox) {
            this.userMailbox = userMailbox;
        }

        public String getVerifyCode() {
            return verifyCode;
        }

        public void setVerifyCode(String verifyCode) {
            this.verifyCode = verifyCode;
        }
    }
}
