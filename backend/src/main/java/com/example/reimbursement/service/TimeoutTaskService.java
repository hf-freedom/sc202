package com.example.reimbursement.service;

import com.example.reimbursement.entity.ApprovalNode;
import com.example.reimbursement.entity.Reimburs;
import com.example.reimbursement.entity.User;
import com.example.reimbursement.enums.ApprovalNodeStatus;
import com.example.reimbursement.store.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TimeoutTaskService {

    @Autowired
    private ReimbursementService reimbursementService;

    @Autowired
    private ApprovalChainService approvalChainService;

    @Autowired
    private DataStore dataStore;

    @Value("${reimbursement.approval-timeout-minutes:1}")
    private int approvalTimeoutMinutes;

    private final List<Map<String, Object>> timeoutHistory = new ArrayList<>();

    @Scheduled(fixedRate = 60000)
    public void checkTimeoutApprovals() {
        processTimeoutCheck(true);
    }

    public Map<String, Object> triggerTimeoutCheck() {
        return processTimeoutCheck(false);
    }

    private Map<String, Object> processTimeoutCheck(boolean isScheduled) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> processed = new ArrayList<>();
        int processedCount = 0;

        List<Reimburs> allPending = dataStore.reimbursements.values().stream()
            .filter(r -> "PENDING".equals(r.getStatus()))
            .collect(java.util.stream.Collectors.toList());

        for (Reimburs reimburs : allPending) {
            for (ApprovalNode node : reimburs.getApprovalTrail()) {
                if (ApprovalNodeStatus.PENDING.equals(node.getStatus())) {
                    long minutesElapsed = ChronoUnit.MINUTES.between(node.getCreatedAt(), LocalDateTime.now());
                    
                    if (minutesElapsed >= approvalTimeoutMinutes) {
                        User currentApprover = dataStore.users.get(node.getApproverId());
                        if (currentApprover == null) continue;

                        User superior = approvalChainService.getSuperiorApprover(currentApprover);
                        if (superior == null) continue;

                        node.setStatus(ApprovalNodeStatus.TRANSFERRED);
                        node.setTransferredFromId(node.getApproverId());
                        node.setTransferredToId(superior.getId());
                        node.setTransferredReason("审批超时自动转交（已等待" + minutesElapsed + "分钟）");
                        node.setUpdatedAt(LocalDateTime.now());

                        ApprovalNode newNode = new ApprovalNode();
                        newNode.setId(dataStore.generateId("node"));
                        newNode.setReimbursementId(reimburs.getId());
                        newNode.setApprovalLevel(node.getApprovalLevel());
                        newNode.setApproverId(superior.getId());
                        newNode.setApproverName(superior.getName());
                        newNode.setStatus(ApprovalNodeStatus.PENDING);
                        newNode.setNodeOrder(node.getNodeOrder());
                        newNode.setCreatedAt(LocalDateTime.now());
                        newNode.setUpdatedAt(LocalDateTime.now());

                        dataStore.approvalNodes.put(newNode.getId(), newNode);
                        reimburs.getApprovalTrail().add(newNode);

                        reimburs.setCurrentApproverId(superior.getId());
                        reimburs.setCurrentApproverName(superior.getName());
                        reimburs.setUpdatedAt(LocalDateTime.now());

                        dataStore.reimbursements.put(reimburs.getId(), reimburs);

                        Map<String, Object> record = new HashMap<>();
                        record.put("reimbursementId", reimburs.getId());
                        record.put("reimbursementTitle", reimburs.getTitle());
                        record.put("fromApprover", currentApprover.getName());
                        record.put("toApprover", superior.getName());
                        record.put("timeoutMinutes", minutesElapsed);
                        record.put("processedAt", LocalDateTime.now());
                        record.put("isScheduled", isScheduled);
                        timeoutHistory.add(0, record);
                        processed.add(record);
                        processedCount++;
                        break;
                    }
                }
            }
        }

        result.put("processedCount", processedCount);
        result.put("processed", processed);
        result.put("totalHistory", timeoutHistory.size());
        result.put("timeoutThreshold", approvalTimeoutMinutes);
        return result;
    }

    public List<Map<String, Object>> getTimeoutHistory() {
        return new ArrayList<>(timeoutHistory);
    }

    public Map<String, Object> getApprovalTimeoutStatus(String approverId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> pendingWithTimeout = new ArrayList<>();

        List<Reimburs> pendingList = dataStore.reimbursements.values().stream()
            .filter(r -> "PENDING".equals(r.getStatus()))
            .filter(r -> approverId.equals(r.getCurrentApproverId()))
            .collect(java.util.stream.Collectors.toList());

        for (Reimburs reimburs : pendingList) {
            for (ApprovalNode node : reimburs.getApprovalTrail()) {
                if (ApprovalNodeStatus.PENDING.equals(node.getStatus()) 
                    && node.getApproverId().equals(approverId)) {
                    long minutesElapsed = ChronoUnit.MINUTES.between(node.getCreatedAt(), LocalDateTime.now());
                    long minutesRemaining = approvalTimeoutMinutes - minutesElapsed;
                    
                    Map<String, Object> status = new HashMap<>();
                    status.put("reimbursementId", reimburs.getId());
                    status.put("reimbursementTitle", reimburs.getTitle());
                    status.put("amount", reimburs.getAmount());
                    status.put("applicantName", reimburs.getApplicantName());
                    status.put("minutesElapsed", minutesElapsed);
                    status.put("minutesRemaining", Math.max(0, minutesRemaining));
                    status.put("isTimeout", minutesElapsed >= approvalTimeoutMinutes);
                    status.put("progressPercent", Math.min(100, (int)((minutesElapsed * 100) / approvalTimeoutMinutes)));
                    pendingWithTimeout.add(status);
                    break;
                }
            }
        }

        result.put("timeoutThreshold", approvalTimeoutMinutes);
        result.put("pendingCount", pendingWithTimeout.size());
        result.put("pendingList", pendingWithTimeout);
        return result;
    }
}
