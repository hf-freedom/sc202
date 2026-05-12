package com.example.reimbursement.entity;

import java.math.BigDecimal;

public class Budget {
    private String departmentId;
    private String departmentName;
    private BigDecimal totalBudget;
    private BigDecimal usedBudget;
    private BigDecimal reservedBudget;

    public Budget() {
    }

    public Budget(String departmentId, String departmentName, BigDecimal totalBudget, BigDecimal usedBudget, BigDecimal reservedBudget) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.totalBudget = totalBudget;
        this.usedBudget = usedBudget;
        this.reservedBudget = reservedBudget;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public BigDecimal getTotalBudget() {
        return totalBudget;
    }

    public void setTotalBudget(BigDecimal totalBudget) {
        this.totalBudget = totalBudget;
    }

    public BigDecimal getUsedBudget() {
        return usedBudget;
    }

    public void setUsedBudget(BigDecimal usedBudget) {
        this.usedBudget = usedBudget;
    }

    public BigDecimal getReservedBudget() {
        return reservedBudget;
    }

    public void setReservedBudget(BigDecimal reservedBudget) {
        this.reservedBudget = reservedBudget;
    }

    public BigDecimal getAvailableBudget() {
        return totalBudget.subtract(usedBudget).subtract(reservedBudget);
    }
}
