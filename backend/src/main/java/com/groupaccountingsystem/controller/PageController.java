package com.groupaccountingsystem.controller;

import com.groupaccountingsystem.pojo.Administrator;
import com.groupaccountingsystem.pojo.OrdinaryUser;
import com.groupaccountingsystem.pojo.Report;
import com.groupaccountingsystem.service.AdministratorService;
import com.groupaccountingsystem.service.OrdinaryUserService;
import com.groupaccountingsystem.service.ReportService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.SimpleDateFormat;

@Controller
public class PageController {
    public static final String SESSION_ADMIN = "admin";
    public static final String SESSION_USER = "user";

    private final AdministratorService administratorService;
    private final OrdinaryUserService ordinaryUserService;
    private final ReportService reportService;

    public PageController(AdministratorService administratorService, OrdinaryUserService ordinaryUserService,
                          ReportService reportService) {
        this.administratorService = administratorService;
        this.ordinaryUserService = ordinaryUserService;
        this.reportService = reportService;
    }

    @GetMapping({"/", "/signin"})
    public String signin() {
        return "signin";
    }

    @GetMapping("/signupOrdinaryUser")
    public String signupOrdinaryUser() {
        return "signupOrdinaryUser";
    }

    @PostMapping("/administratorLogin")
    public String administratorLogin(Administrator administrator, HttpSession session, RedirectAttributes attributes) {
        Administrator loggedIn = administratorService.administratorLogin(administrator);
        if (loggedIn == null) {
            attributes.addFlashAttribute("error", "管理员账号或密码有误");
            return "redirect:/signin";
        }
        session.setAttribute(SESSION_ADMIN, loggedIn);
        session.removeAttribute(SESSION_USER);
        return "redirect:/index";
    }

    @PostMapping("/ordinaryUserLogin")
    public String ordinaryUserLogin(@RequestParam String account,
                                    @RequestParam String userPassword,
                                    HttpSession session,
                                    RedirectAttributes attributes) {
        OrdinaryUser ordinaryUser = new OrdinaryUser();
        ordinaryUser.setAccount(account);
        ordinaryUser.setUserPassword(userPassword);
        OrdinaryUser loggedIn = ordinaryUserService.ordinaryUserLogin(ordinaryUser);
        if (loggedIn == null) {
            attributes.addFlashAttribute("error", "普通用户账号或密码有误");
            return "redirect:/signin";
        }
        Report activeBan = reportService.getActiveBanReport(loggedIn.getUserID());
        if (activeBan != null) {
            attributes.addFlashAttribute("banMessage", buildBanMessage(activeBan));
            return "redirect:/signin";
        }
        session.setAttribute(SESSION_USER, loggedIn);
        session.removeAttribute(SESSION_ADMIN);
        return "redirect:/index";
    }

    @PostMapping("/sendSmsLoginCode")
    public String sendSmsLoginCode(@RequestParam String phoneNumber,
                                   RedirectAttributes attributes) {
        try {
            ordinaryUserService.sendSmsLoginCode(phoneNumber);
            attributes.addFlashAttribute("message", "SMS verify code has been sent.");
            attributes.addFlashAttribute("smsPhoneNumber", phoneNumber);
        } catch (RuntimeException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/signin";
    }

    @PostMapping("/ordinaryUserSmsLogin")
    public String ordinaryUserSmsLogin(@RequestParam String phoneNumber,
                                       @RequestParam String verifyCode,
                                       HttpSession session,
                                       RedirectAttributes attributes) {
        try {
            OrdinaryUser loggedIn = ordinaryUserService.ordinaryUserLoginBySmsCode(phoneNumber, verifyCode);
            Report activeBan = reportService.getActiveBanReport(loggedIn.getUserID());
            if (activeBan != null) {
                attributes.addFlashAttribute("banMessage", buildBanMessage(activeBan));
                attributes.addFlashAttribute("smsPhoneNumber", phoneNumber);
                return "redirect:/signin";
            }
            session.setAttribute(SESSION_USER, loggedIn);
            session.removeAttribute(SESSION_ADMIN);
            return "redirect:/index";
        } catch (RuntimeException e) {
            attributes.addFlashAttribute("error", e.getMessage());
            attributes.addFlashAttribute("smsPhoneNumber", phoneNumber);
            return "redirect:/signin";
        }
    }

    @PostMapping("/signupOrdinaryUser")
    public String signup(OrdinaryUser ordinaryUser,
                         @RequestParam String confirmPassword,
                         RedirectAttributes attributes) {
        try {
            ordinaryUserService.register(ordinaryUser, confirmPassword);
            attributes.addFlashAttribute("message", "注册成功，请登录");
            return "redirect:/signin";
        } catch (RuntimeException e) {
            attributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/signupOrdinaryUser";
        }
    }

    @GetMapping("/index")
    public String index(HttpSession session, Model model) {
        Object admin = session.getAttribute(SESSION_ADMIN);
        Object user = session.getAttribute(SESSION_USER);
        if (admin == null && user == null) {
            return "redirect:/signin";
        }
        model.addAttribute("admin", admin);
        model.addAttribute("user", user);
        if (user instanceof OrdinaryUser ordinaryUser) {
            model.addAttribute("warningReports", reportService.listWarningReports(ordinaryUser.getUserID()));
        }
        return "index";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/signin";
    }

    private String buildBanMessage(Report report) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy年MM月dd日HH时mm分");
        return "您在项目【" + punishmentActivityName(report) + "】中有不合规行为，已被封号，截止到"
                + formatter.format(report.getPunishmentEndTime());
    }

    private String punishmentActivityName(Report report) {
        if (report.getAccusedActivityName() != null && !report.getAccusedActivityName().isBlank()) {
            return report.getAccusedActivityName();
        }
        return String.valueOf(report.getAccusedActivityID());
    }
}
