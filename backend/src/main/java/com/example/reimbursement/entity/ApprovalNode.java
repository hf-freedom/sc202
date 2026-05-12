package com.example.reimbursement.entity;

import com.example.reimbursement.enums.ApprovalLevel;
import com.example.reimbursement.enums.ApprovalNodeStatus;

import java.time.LocalDateTime;

public class ApprovalNode {
    private String id;
    private String reimbursementId;
    private ApprovalLevel approvalLevel;
    private String approverId;
    private String approverName;
    private ApprovalNodeStatus status;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer nodeOrder;
    private String transferredFromId;
    private String transferredToId;
    private String transferredReason;

    public ApprovalNode() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReimbursementId() {
        return reimbursementId;
    }

    public void setReimbursementId(String reimbursementId) {
        this.reimbursementId = reimbursementId;
    }

    public ApprovalLevel getApprovalLevel() {
        return approvalLevel;
    }

    public void setApprovalLevel(ApprovalLevel approvalLevel) {
        this.approvalLevel = approvalLevel;
    }

    public String getApproverId() {
        return approverId;
    }

    public void setApproverId(String approverId) {
        this.approverId = approverId;
    }

    public String getApproverName() {
        return approverName;
    }

    public void setApproverName(String approverName) {
        this.approverName = approverName;
    }

    public ApprovalNodeStatus getStatus() {
        return status;
    }

    public void setStatus(ApprovalNodeStatus status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getNodeOrder() {
        return nodeOrder;
    }

    public void setNodeOrder(Integer nodeOrder) {
        this.nodeOrder = nodeOrder;
    }

    public String getTransferredFromId() {
        return transferredFromId;
    }

    public void setTransferredFromId(String transferredFromId) {
        this.transferredFromId = transferredFromId;
    }

    public String getTransferredToId() {
        return transferredToId;
    }

    public void setTransferredToId(String transferredToId) {
        this.transferredToId = transferredToId;
    }

    public String getTransferredReason() {
        return transferredReason;
    }

    public void setTransferredReason(String transferredReason) {
        this.transferredReason = transferredReason;
    }
}
