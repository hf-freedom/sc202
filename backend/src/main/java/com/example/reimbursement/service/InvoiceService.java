package com.example.reimbursement.service;

import com.example.reimbursement.entity.InvoiceCache;
import com.example.reimbursement.store.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class InvoiceService {

    @Autowired
    private DataStore dataStore;

    public boolean isInvoiceUsed(String invoiceNumber) {
        if (invoiceNumber == null || invoiceNumber.isEmpty()) {
            return false;
        }
        InvoiceCache cache = dataStore.invoiceCache.get(invoiceNumber);
        return cache != null && Boolean.TRUE.equals(cache.getUsed());
    }

    public InvoiceCache getInvoiceCache(String invoiceNumber) {
        return dataStore.invoiceCache.get(invoiceNumber);
    }

    public synchronized void markInvoiceUsed(String invoiceNumber, String reimbursementId, String applicantId) {
        if (invoiceNumber == null || invoiceNumber.isEmpty()) {
            return;
        }
        InvoiceCache cache = new InvoiceCache();
        cache.setInvoiceNumber(invoiceNumber);
        cache.setReimbursementId(reimbursementId);
        cache.setApplicantId(applicantId);
        cache.setCachedAt(LocalDateTime.now());
        cache.setUsed(true);
        dataStore.invoiceCache.put(invoiceNumber, cache);
    }

    public synchronized void releaseInvoice(String invoiceNumber) {
        if (invoiceNumber == null || invoiceNumber.isEmpty()) {
            return;
        }
        InvoiceCache cache = dataStore.invoiceCache.get(invoiceNumber);
        if (cache != null) {
            cache.setUsed(false);
        }
    }
}
