package com.groupaccountingsystem.controller;

import com.groupaccountingsystem.pojo.Administrator;
import com.groupaccountingsystem.pojo.OrdinaryUser;
import com.groupaccountingsystem.service.AdministratorService;
import com.groupaccountingsystem.service.OrdinaryUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;


/*登录*/
@RestController
@RequestMapping("/api")
public class ApiLoginController {

    private static final String SESSION_ADMIN = "admin";
    private static final String SESSION_USER = "user";

    private final AdministratorService administratorService;
    private final OrdinaryUserService ordinaryUserService;

    public ApiLoginController(
            AdministratorService administratorService,
            OrdinaryUserService ordinaryUserService
            ) {
        this.administratorService = administratorService;
        this.ordinaryUserService = ordinaryUserService;
    }
    @PostMapping("/administratorLogin")
    public ResponseEntity<Map<String, Object>> administratorLogin(Administrator administrator, HttpSession session) {
        Administrator loggedIn = administratorService.administratorLogin(administrator);
        if (loggedIn == null) {
            return unauthorized("账号或密码有误");
        }
        session.setAttribute(SESSION_ADMIN, loggedIn);
        session.removeAttribute(SESSION_USER);
        //putAdministratorMainDataInSession(session, loggedIn);
        return ResponseEntity.ok(singleSuccess());
    }

    @PostMapping("/ordinaryUserLogin")
    public ResponseEntity<Map<String, Object>> userLogin(OrdinaryUser ordinaryUser, HttpSession session) {
        OrdinaryUser loggedIn = ordinaryUserService.ordinaryUserLogin(ordinaryUser);
        if (loggedIn == null) {
            return unauthorized("账号或密码有误");
        }
        session.setAttribute(SESSION_USER, loggedIn);
        return ResponseEntity.ok(singleSuccess());
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(singleSuccess());
    }

    /*@GetMapping("/toAdministratorMain")*/

    /*@GetMapping("/toOrdinaryUserMain")*/


    /** 管理员登录后写入 session：身份 + 主页统计（与原先 administratorMain 依赖的 key 一致）。 */


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
}
