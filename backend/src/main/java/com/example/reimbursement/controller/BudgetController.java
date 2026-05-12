package com.example.reimbursement.controller;

import com.example.reimbursement.entity.Budget;
import com.example.reimbursement.service.BudgetService;
import com.example.reimbursement.store.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private DataStore dataStore;

    @GetMapping("/{departmentId}")
    public ResponseEntity<Map<String, Object>> getBudget(@PathVariable String departmentId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Budget budget = budgetService.getBudget(departmentId);
            if (budget == null) {
                result.put("success", false);
                result.put("message", "部门预算不存在");
                return ResponseEntity.ok(result);
            }
            result.put("success", true);
            result.put("data", budget);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkBudget(@RequestParam String departmentId,
                                                           @RequestParam BigDecimal amount) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean available = budgetService.checkBudgetAvailable(departmentId, amount);
            result.put("success", true);
            result.put("data", available);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll() {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("success", true);
            result.put("data", dataStore.budgets.values());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
}
