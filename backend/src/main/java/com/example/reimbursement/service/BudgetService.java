package com.example.reimbursement.service;

import com.example.reimbursement.entity.Budget;
import com.example.reimbursement.store.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class BudgetService {

    @Autowired
    private DataStore dataStore;

    public Budget getBudget(String departmentId) {
        return dataStore.budgets.get(departmentId);
    }

    public boolean checkBudgetAvailable(String departmentId, BigDecimal amount) {
        Budget budget = dataStore.budgets.get(departmentId);
        if (budget == null) {
            return false;
        }
        return budget.getAvailableBudget().compareTo(amount) >= 0;
    }

    public synchronized void reserveBudget(String departmentId, BigDecimal amount) {
        Budget budget = dataStore.budgets.get(departmentId);
        if (budget == null) {
            throw new RuntimeException("部门预算不存在");
        }
        if (budget.getAvailableBudget().compareTo(amount) < 0) {
            throw new RuntimeException("预算不足");
        }
        budget.setReservedBudget(budget.getReservedBudget().add(amount));
    }

    public synchronized void releaseBudget(String departmentId, BigDecimal amount) {
        Budget budget = dataStore.budgets.get(departmentId);
        if (budget == null) {
            return;
        }
        if (budget.getReservedBudget().compareTo(amount) >= 0) {
            budget.setReservedBudget(budget.getReservedBudget().subtract(amount));
        }
    }

    public synchronized void consumeBudget(String departmentId, BigDecimal amount) {
        Budget budget = dataStore.budgets.get(departmentId);
        if (budget == null) {
            throw new RuntimeException("部门预算不存在");
        }
        if (budget.getReservedBudget().compareTo(amount) >= 0) {
            budget.setReservedBudget(budget.getReservedBudget().subtract(amount));
            budget.setUsedBudget(budget.getUsedBudget().add(amount));
        } else {
            if (budget.getAvailableBudget().compareTo(amount) < 0) {
                throw new RuntimeException("预算不足");
            }
            budget.setUsedBudget(budget.getUsedBudget().add(amount));
        }
    }
}
