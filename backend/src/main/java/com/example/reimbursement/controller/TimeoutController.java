package com.example.reimbursement.controller;

import com.example.reimbursement.service.TimeoutTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/timeout")
public class TimeoutController {

    @Autowired
    private TimeoutTaskService timeoutTaskService;

    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerTimeoutCheck() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = timeoutTaskService.triggerTimeoutCheck();
            result.put("success", true);
            result.put("data", data);
            result.put("message", "超时检查完成，共处理 " + data.get("processedCount") + " 条记录");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getTimeoutHistory() {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("success", true);
            result.put("data", timeoutTaskService.getTimeoutHistory());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status/{approverId}")
    public ResponseEntity<Map<String, Object>> getApprovalTimeoutStatus(@PathVariable String approverId) {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("success", true);
            result.put("data", timeoutTaskService.getApprovalTimeoutStatus(approverId));
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
}
