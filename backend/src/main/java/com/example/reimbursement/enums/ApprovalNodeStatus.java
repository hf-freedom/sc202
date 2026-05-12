package com.example.reimbursement.enums;

public enum ApprovalNodeStatus {
    PENDING("待审批"),
    APPROVED("通过"),
    REJECTED("驳回"),
    TRANSFERRED("转交"),
    WITHDRAWN("撤回"),
    SKIPPED("跳过");

    private final String description;

    ApprovalNodeStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
