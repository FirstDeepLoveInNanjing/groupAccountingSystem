package com.groupaccountingsystem.controller;

import com.groupaccountingsystem.pojo.IncomeExpenseStatistics;
import com.groupaccountingsystem.pojo.OrdinaryUser;
import com.groupaccountingsystem.service.StatisticsService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
public class ApiStatisticsController {
    private final StatisticsService statisticsService;

    public ApiStatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/income-expense")
    public ApiResponse<IncomeExpenseStatistics> incomeExpense(HttpSession session) {
        OrdinaryUser user = ApiSession.currentUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "not logged in");
        }
        return ApiResponse.ok(statisticsService.getIncomeExpenseStatistics(user.getUserID()));
    }
}
