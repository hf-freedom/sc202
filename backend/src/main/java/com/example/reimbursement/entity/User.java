package com.example.reimbursement.entity;

import com.example.reimbursement.enums.ApprovalLevel;

import java.util.List;

public class User {
    private String id;
    private String name;
    private String departmentId;
    private ApprovalLevel approvalLevel;
    private String superiorId;
    private List<String> subordinateIds;

    public User() {
    }

    public User(String id, String name, String departmentId, ApprovalLevel approvalLevel, String superiorId, List<String> subordinateIds) {
        this.id = id;
        this.name = name;
        this.departmentId = departmentId;
        this.approvalLevel = approvalLevel;
        this.superiorId = superiorId;
        this.subordinateIds = subordinateIds;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public ApprovalLevel getApprovalLevel() {
        return approvalLevel;
    }

    public void setApprovalLevel(ApprovalLevel approvalLevel) {
        this.approvalLevel = approvalLevel;
    }

    public String getSuperiorId() {
        return superiorId;
    }

    public void setSuperiorId(String superiorId) {
        this.superiorId = superiorId;
    }

    public List<String> getSubordinateIds() {
        return subordinateIds;
    }

    public void setSubordinateIds(List<String> subordinateIds) {
        this.subordinateIds = subordinateIds;
    }
}
