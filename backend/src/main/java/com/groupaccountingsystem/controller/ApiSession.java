package com.groupaccountingsystem.controller;

import com.groupaccountingsystem.pojo.Administrator;
import com.groupaccountingsystem.pojo.OrdinaryUser;
import jakarta.servlet.http.HttpSession;

final class ApiSession {
    static final String SESSION_ADMIN = "admin";
    static final String SESSION_USER = "user";

    private ApiSession() {
    }

    static OrdinaryUser currentUser(HttpSession session) {
        Object user = session.getAttribute(SESSION_USER);
        return user instanceof OrdinaryUser ? (OrdinaryUser) user : null;
    }

    static Administrator currentAdmin(HttpSession session) {
        Object admin = session.getAttribute(SESSION_ADMIN);
        return admin instanceof Administrator ? (Administrator) admin : null;
    }

    static boolean loggedIn(HttpSession session) {
        return currentUser(session) != null || currentAdmin(session) != null;
    }
}
