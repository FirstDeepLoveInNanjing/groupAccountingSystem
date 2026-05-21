package com.groupaccountingsystem.controller;

import com.groupaccountingsystem.pojo.OrdinaryUser;
import com.groupaccountingsystem.service.OrdinaryUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/*注册*/
@RestController
@RequestMapping("/api")
public class ApiRegisterController {

    @Autowired
    private OrdinaryUserService ordinaryUserService;

    // 改为接收表单参数，去掉 @RequestBody
    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(RegisterRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            OrdinaryUser user = new OrdinaryUser();
            user.setUserName(request.getUserName());
            user.setUserPassword(request.getUserPassword());
            user.setPhoneNumber(request.getPhoneNumber());
            user.setUserMailbox(request.getUserMailbox());

            ordinaryUserService.register(user, request.getConfirmPassword());

            response.put("success", true);
            response.put("message", "注册成功");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // 内部类接收注册请求参数（保持原样，无需修改）
    public static class RegisterRequest {
        private String userName;        // 用户名
        private String userPassword;    // 密码
        private String confirmPassword; // 确认密码
        private String phoneNumber;     // 手机号
        private String userMailbox;     // 邮箱

        // getter 和 setter（必须提供）
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public String getUserPassword() { return userPassword; }
        public void setUserPassword(String userPassword) { this.userPassword = userPassword; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getUserMailbox() { return userMailbox; }
        public void setUserMailbox(String userMailbox) { this.userMailbox = userMailbox; }
    }
}