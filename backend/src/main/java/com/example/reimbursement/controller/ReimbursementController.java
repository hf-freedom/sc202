package com.example.reimbursement.controller;

import com.example.reimbursement.entity.ApprovalNode;
import com.example.reimbursement.entity.Reimburs;
import com.example.reimbursement.service.ReimbursementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reimbursements")
public class ReimbursementController {

    @Autowired
    private ReimbursementService reimbursementService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Reimburs> list = reimbursementService.getAll();
            result.put("success", true);
            result.put("data", list);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Reimburs reimburs = reimbursementService.getById(id);
            if (reimburs == null) {
                result.put("success", false);
                result.put("message", "报销单不存在");
                return ResponseEntity.ok(result);
            }
            result.put("success", true);
            result.put("data", reimburs);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @GetMapping("/applicant/{applicantId}")
    public ResponseEntity<Map<String, Object>> getByApplicant(@PathVariable String applicantId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Reimburs> list = reimbursementService.getByApplicant(applicantId);
            result.put("success", true);
            result.put("data", list);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @GetMapping("/approver/{approverId}")
    public ResponseEntity<Map<String, Object>> getByApprover(@PathVariable String approverId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Reimburs> list = reimbursementService.getByApprover(approverId);
            result.put("success", true);
            result.put("data", list);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @PostMapping("/draft")
    public ResponseEntity<Map<String, Object>> saveDraft(@RequestBody Reimburs request, 
                                                         @RequestParam String applicantId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Reimburs reimburs = reimbursementService.saveDraft(request, applicantId);
            result.put("success", true);
            result.put("data", reimburs);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @PutMapping("/draft/{id}")
    public ResponseEntity<Map<String, Object>> updateDraft(@PathVariable String id, 
                                                          @RequestBody Reimburs request) {
        Map<String, Object> result = new HashMap<>();
        try {
            Reimburs reimburs = reimbursementService.updateDraft(id, request);
            result.put("success", true);
            result.put("data", reimburs);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<Map<String, Object>> submit(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Reimburs reimburs = reimbursementService.submitReimbursement(id);
            result.put("success", true);
            result.put("data", reimburs);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approve(@PathVariable String id,
                                                      @RequestParam String approverId,
                                                      @RequestParam(required = false) String comment) {
        Map<String, Object> result = new HashMap<>();
        try {
            Reimburs reimburs = reimbursementService.approve(id, approverId, comment);
            result.put("success", true);
            result.put("data", reimburs);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> reject(@PathVariable String id,
                                                     @RequestParam String approverId,
                                                     @RequestParam(required = false) String comment) {
        Map<String, Object> result = new HashMap<>();
        try {
            Reimburs reimburs = reimbursementService.reject(id, approverId, comment);
            result.put("success", true);
            result.put("data", reimburs);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @PostMapping("/{id}/resubmit")
    public ResponseEntity<Map<String, Object>> resubmit(@PathVariable String id,
                                                       @RequestBody Reimburs request) {
        Map<String, Object> result = new HashMap<>();
        try {
            Reimburs reimburs = reimbursementService.resubmit(id, request);
            result.put("success", true);
            result.put("data", reimburs);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @PostMapping("/{id}/transfer")
    public ResponseEntity<Map<String, Object>> transfer(@PathVariable String id,
                                                       @RequestParam String fromApproverId,
                                                       @RequestParam String toApproverId,
                                                       @RequestParam(required = false) String reason) {
        Map<String, Object> result = new HashMap<>();
        try {
            Reimburs reimburs = reimbursementService.transfer(id, fromApproverId, toApproverId, reason);
            result.put("success", true);
            result.put("data", reimburs);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<Map<String, Object>> withdraw(@PathVariable String id,
                                                       @RequestParam String applicantId,
                                                       @RequestParam(required = false) String comment) {
        Map<String, Object> result = new HashMap<>();
        try {
            Reimburs reimburs = reimbursementService.withdraw(id, applicantId, comment);
            result.put("success", true);
            result.put("data", reimburs);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @GetMapping("/{id}/nodes")
    public ResponseEntity<Map<String, Object>> getApprovalNodes(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<ApprovalNode> nodes = reimbursementService.getApprovalNodes(id);
            result.put("success", true);
            result.put("data", nodes);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
}
