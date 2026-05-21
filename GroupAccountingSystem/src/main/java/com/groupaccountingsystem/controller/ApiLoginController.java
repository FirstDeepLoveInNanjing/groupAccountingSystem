package com.groupaccountingsystem.controller;

import com.groupaccountingsystem.pojo.Administrator;
import com.groupaccountingsystem.pojo.OrdinaryUser;
import com.groupaccountingsystem.pojo.Report;
import com.groupaccountingsystem.service.AdministratorService;
import com.groupaccountingsystem.service.OrdinaryUserService;
import com.groupaccountingsystem.service.ReportService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiLoginController {

    private static final String SESSION_ADMIN = "admin";
    private static final String SESSION_USER = "user";

    private final AdministratorService administratorService;
    private final OrdinaryUserService ordinaryUserService;
    private final ReportService reportService;

    public ApiLoginController(AdministratorService administratorService,
                              OrdinaryUserService ordinaryUserService,
                              ReportService reportService) {
        this.administratorService = administratorService;
        this.ordinaryUserService = ordinaryUserService;
        this.reportService = reportService;
    }

    @PostMapping("/administratorLogin")
    public ResponseEntity<Map<String, Object>> administratorLogin(Administrator administrator, HttpSession session) {
        Administrator loggedIn = administratorService.administratorLogin(administrator);
        if (loggedIn == null) {
            return unauthorized("账号或密码有误");
        }
        session.setAttribute(SESSION_ADMIN, loggedIn);
        session.removeAttribute(SESSION_USER);
        return ResponseEntity.ok(singleSuccess());
    }

    @PostMapping("/ordinaryUserLogin")
    public ResponseEntity<Map<String, Object>> userLogin(OrdinaryUser ordinaryUser, HttpSession session) {
        OrdinaryUser loggedIn = ordinaryUserService.ordinaryUserLogin(ordinaryUser);
        if (loggedIn == null) {
            return unauthorized("账号或密码有误");
        }
        Report activeBan = reportService.getActiveBanReport(loggedIn.getUserID());
        if (activeBan != null) {
            return unauthorized(buildBanMessage(activeBan));
        }
        session.setAttribute(SESSION_USER, loggedIn);
        session.removeAttribute(SESSION_ADMIN);
        return ResponseEntity.ok(singleSuccess());
    }

    @PostMapping("/sendSmsLoginCode")
    public ResponseEntity<Map<String, Object>> sendSmsLoginCode(@RequestParam String phoneNumber) {
        try {
            ordinaryUserService.sendSmsLoginCode(phoneNumber);
            return ResponseEntity.ok(singleSuccess());
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        }
    }

    @PostMapping("/ordinaryUserSmsLogin")
    public ResponseEntity<Map<String, Object>> ordinaryUserSmsLogin(@RequestParam String phoneNumber,
                                                                    @RequestParam String verifyCode,
                                                                    HttpSession session) {
        try {
            OrdinaryUser loggedIn = ordinaryUserService.ordinaryUserLoginBySmsCode(phoneNumber, verifyCode);
            Report activeBan = reportService.getActiveBanReport(loggedIn.getUserID());
            if (activeBan != null) {
                return unauthorized(buildBanMessage(activeBan));
            }
            session.setAttribute(SESSION_USER, loggedIn);
            session.removeAttribute(SESSION_ADMIN);
            return ResponseEntity.ok(singleSuccess());
        } catch (RuntimeException e) {
            return unauthorized(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(singleSuccess());
    }

    private static Map<String, Object> singleSuccess() {
        Map<String, Object> m = new HashMap<>(2);
        m.put("success", true);
        return m;
    }

    private static ResponseEntity<Map<String, Object>> unauthorized(String message) {
        Map<String, Object> m = new HashMap<>(4);
        m.put("success", false);
        m.put("message", message);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(m);
    }

    private static ResponseEntity<Map<String, Object>> badRequest(String message) {
        Map<String, Object> m = new HashMap<>(4);
        m.put("success", false);
        m.put("message", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(m);
    }

    private static String buildBanMessage(Report report) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy年MM月dd日HH时mm分");
        return "您在【" + punishmentActivityName(report) + "】中有不合规行为，已被封号，截止到"
                + formatter.format(report.getPunishmentEndTime());
    }

    private static String punishmentActivityName(Report report) {
        if (report.getAccusedActivityName() != null && !report.getAccusedActivityName().isBlank()) {
            return report.getAccusedActivityName();
        }
        return String.valueOf(report.getAccusedActivityID());
    }
}
