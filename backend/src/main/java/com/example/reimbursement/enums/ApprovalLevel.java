package com.example.reimbursement.enums;

public enum ApprovalLevel {
    DIRECT_MANAGER("直属领导", 1),
    DEPARTMENT_HEAD("部门负责人", 2),
    FINANCE("财务", 3);

    private final String description;
    private final int level;

    ApprovalLevel(String description, int level) {
        this.description = description;
        this.level = level;
    }

    public String getDescription() {
        return description;
    }

    public int getLevel() {
        return level;
    }
}
