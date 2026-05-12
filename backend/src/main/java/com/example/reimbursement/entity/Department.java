package com.example.reimbursement.entity;

public class Department {
    private String id;
    private String name;
    private String headId;

    public Department() {
    }

    public Department(String id, String name, String headId) {
        this.id = id;
        this.name = name;
        this.headId = headId;
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

    public String getHeadId() {
        return headId;
    }

    public void setHeadId(String headId) {
        this.headId = headId;
    }
}
