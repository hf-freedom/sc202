package com.example.reimbursement.entity;

import java.time.LocalDateTime;

public class InvoiceCache {
    private String invoiceNumber;
    private String reimbursementId;
    private String applicantId;
    private LocalDateTime cachedAt;
    private Boolean used;

    public InvoiceCache() {
    }

    public InvoiceCache(String invoiceNumber, String reimbursementId, String applicantId, LocalDateTime cachedAt, Boolean used) {
        this.invoiceNumber = invoiceNumber;
        this.reimbursementId = reimbursementId;
        this.applicantId = applicantId;
        this.cachedAt = cachedAt;
        this.used = used;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getReimbursementId() {
        return reimbursementId;
    }

    public void setReimbursementId(String reimbursementId) {
        this.reimbursementId = reimbursementId;
    }

    public String getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(String applicantId) {
        this.applicantId = applicantId;
    }

    public LocalDateTime getCachedAt() {
        return cachedAt;
    }

    public void setCachedAt(LocalDateTime cachedAt) {
        this.cachedAt = cachedAt;
    }

    public Boolean getUsed() {
        return used;
    }

    public void setUsed(Boolean used) {
        this.used = used;
    }
}
