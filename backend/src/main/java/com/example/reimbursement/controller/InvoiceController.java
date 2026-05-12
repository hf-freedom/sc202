package com.example.reimbursement.controller;

import com.example.reimbursement.entity.InvoiceCache;
import com.example.reimbursement.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @GetMapping("/check/{invoiceNumber}")
    public ResponseEntity<Map<String, Object>> checkInvoice(@PathVariable String invoiceNumber) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean used = invoiceService.isInvoiceUsed(invoiceNumber);
            InvoiceCache cache = invoiceService.getInvoiceCache(invoiceNumber);
            Map<String, Object> data = new HashMap<>();
            data.put("used", used);
            data.put("cache", cache);
            result.put("success", true);
            result.put("data", data);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
}
