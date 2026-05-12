package com.example.reimbursement.service;

import com.example.reimbursement.entity.*;
import com.example.reimbursement.enums.ApprovalLevel;
import com.example.reimbursement.store.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class ApprovalChainService {

    @Autowired
    private DataStore dataStore;

    @Value("${reimbursement.small-amount-threshold:5000}")
    private BigDecimal smallAmountThreshold;

    public List<ApprovalLevel> getApprovalLevels(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Collections.singletonList(ApprovalLevel.DIRECT_MANAGER);
        }
        
        if (amount.compareTo(smallAmountThreshold) < 0) {
            return Collections.singletonList(ApprovalLevel.DIRECT_MANAGER);
        } else {
            return Arrays.asList(
                ApprovalLevel.DIRECT_MANAGER,
                ApprovalLevel.DEPARTMENT_HEAD,
                ApprovalLevel.FINANCE
            );
        }
    }

    public User getApprover(User applicant, ApprovalLevel level) {
        if (applicant == null) {
            return null;
        }

        switch (level) {
            case DIRECT_MANAGER:
                return getDirectManager(applicant);
            case DEPARTMENT_HEAD:
                return getDepartmentHead(applicant.getDepartmentId());
            case FINANCE:
                return getFinanceApprover();
            default:
                return null;
        }
    }

    public User getDirectManager(User user) {
        if (user == null || user.getSuperiorId() == null) {
            return null;
        }
        return dataStore.users.get(user.getSuperiorId());
    }

    public User getDepartmentHead(String departmentId) {
        if (departmentId == null) {
            return null;
        }
        Department dept = dataStore.departments.get(departmentId);
        if (dept == null || dept.getHeadId() == null) {
            return null;
        }
        return dataStore.users.get(dept.getHeadId());
    }

    public User getFinanceApprover() {
        for (User user : dataStore.users.values()) {
            if (ApprovalLevel.FINANCE.equals(user.getApprovalLevel())) {
                return user;
            }
        }
        return null;
    }

    public User getSuperiorApprover(User currentApprover) {
        if (currentApprover == null || currentApprover.getSuperiorId() == null) {
            return null;
        }
        return dataStore.users.get(currentApprover.getSuperiorId());
    }
}
