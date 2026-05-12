package com.example.reimbursement.enums;

public enum ReimbursementStatus {
    DRAFT("草稿"),
    PENDING("待审批"),
    APPROVED("已通过"),
    REJECTED("已驳回"),
    REJECTED_RESUBMIT("驳回待重提"),
    TRANSFERRED("已转交"),
    WITHDRAWN("已撤回"),
    COMPLETED("已完成");

    private final String description;

    ReimbursementStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
