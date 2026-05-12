package com.example.reimbursement.service;

import com.example.reimbursement.entity.*;
import com.example.reimbursement.enums.*;
import com.example.reimbursement.store.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReimbursementService {

    @Autowired
    private DataStore dataStore;

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private ApprovalChainService approvalChainService;

    @Value("${reimbursement.small-amount-threshold:5000}")
    private BigDecimal smallAmountThreshold;

    @Value("${reimbursement.approval-timeout-hours:24}")
    private int approvalTimeoutHours;

    public Reimburs saveDraft(Reimburs request, String applicantId) {
        User applicant = dataStore.users.get(applicantId);
        if (applicant == null) {
            throw new RuntimeException("申请人不存在");
        }

        Reimburs reimburs = new Reimburs();
        reimburs.setId(dataStore.generateId("reimburs"));
        reimburs.setApplicantId(applicantId);
        reimburs.setApplicantName(applicant.getName());
        reimburs.setDepartmentId(applicant.getDepartmentId());
        reimburs.setDepartmentName(dataStore.departments.get(applicant.getDepartmentId()) != null 
            ? dataStore.departments.get(applicant.getDepartmentId()).getName() : null);
        reimburs.setTitle(request.getTitle());
        reimburs.setDescription(request.getDescription());
        reimburs.setAmount(request.getAmount());
        reimburs.setInvoiceNumber(request.getInvoiceNumber());
        reimburs.setInvoiceImage(request.getInvoiceImage());
        reimburs.setStatus(ReimbursementStatus.DRAFT);
        reimburs.setCreatedAt(LocalDateTime.now());
        reimburs.setUpdatedAt(LocalDateTime.now());

        dataStore.reimbursements.put(reimburs.getId(), reimburs);
        return reimburs;
    }

    public Reimburs updateDraft(String reimbursId, Reimburs request) {
        Reimburs reimburs = dataStore.reimbursements.get(reimbursId);
        if (reimburs == null) {
            throw new RuntimeException("报销单不存在");
        }
        if (!ReimbursementStatus.DRAFT.equals(reimburs.getStatus())) {
            throw new RuntimeException("只能更新草稿状态的报销单");
        }

        reimburs.setTitle(request.getTitle());
        reimburs.setDescription(request.getDescription());
        reimburs.setAmount(request.getAmount());
        reimburs.setInvoiceNumber(request.getInvoiceNumber());
        reimburs.setInvoiceImage(request.getInvoiceImage());
        reimburs.setUpdatedAt(LocalDateTime.now());

        return reimburs;
    }

    public Reimburs submitReimbursement(String reimbursId) {
        Reimburs reimburs = dataStore.reimbursements.get(reimbursId);
        if (reimburs == null) {
            throw new RuntimeException("报销单不存在");
        }

        if (!ReimbursementStatus.DRAFT.equals(reimburs.getStatus()) 
            && !ReimbursementStatus.REJECTED_RESUBMIT.equals(reimburs.getStatus())) {
            throw new RuntimeException("只能提交草稿或驳回待重提状态的报销单");
        }

        if (invoiceService.isInvoiceUsed(reimburs.getInvoiceNumber())) {
            throw new RuntimeException("该发票号已被使用，无法重复报销");
        }

        if (!budgetService.checkBudgetAvailable(reimburs.getDepartmentId(), reimburs.getAmount())) {
            throw new RuntimeException("预算不足，无法提交，请先保存为草稿");
        }

        budgetService.reserveBudget(reimburs.getDepartmentId(), reimburs.getAmount());

        invoiceService.markInvoiceUsed(reimburs.getInvoiceNumber(), reimbursId, reimburs.getApplicantId());

        createApprovalNodes(reimburs);

        reimburs.setStatus(ReimbursementStatus.PENDING);
        reimburs.setSubmittedAt(LocalDateTime.now());
        reimburs.setUpdatedAt(LocalDateTime.now());

        setCurrentApprover(reimburs);

        return reimburs;
    }

    private void createApprovalNodes(Reimburs reimburs) {
        List<ApprovalLevel> levels = approvalChainService.getApprovalLevels(reimburs.getAmount());
        User applicant = dataStore.users.get(reimburs.getApplicantId());

        for (int i = 0; i < levels.size(); i++) {
            ApprovalLevel level = levels.get(i);
            User approver = approvalChainService.getApprover(applicant, level);

            if (approver == null) {
                continue;
            }

            ApprovalNode node = new ApprovalNode();
            node.setId(dataStore.generateId("node"));
            node.setReimbursementId(reimburs.getId());
            node.setApprovalLevel(level);
            node.setApproverId(approver.getId());
            node.setApproverName(approver.getName());
            node.setStatus(i == 0 ? ApprovalNodeStatus.PENDING : ApprovalNodeStatus.SKIPPED);
            node.setNodeOrder(i);
            node.setCreatedAt(LocalDateTime.now());
            node.setUpdatedAt(LocalDateTime.now());

            dataStore.approvalNodes.put(node.getId(), node);
            reimburs.getApprovalTrail().add(node);
        }
    }

    private void setCurrentApprover(Reimburs reimburs) {
        ApprovalNode nextNode = getNextPendingNode(reimburs);
        if (nextNode != null) {
            reimburs.setCurrentApproverId(nextNode.getApproverId());
            reimburs.setCurrentApproverName(nextNode.getApproverName());
        }
    }

    private ApprovalNode getNextPendingNode(Reimburs reimburs) {
        for (ApprovalNode node : reimburs.getApprovalTrail()) {
            if (ApprovalNodeStatus.PENDING.equals(node.getStatus())) {
                return node;
            }
        }
        return null;
    }

    private ApprovalNode getCurrentNode(Reimburs reimburs, String approverId) {
        for (ApprovalNode node : reimburs.getApprovalTrail()) {
            if (ApprovalNodeStatus.PENDING.equals(node.getStatus()) 
                && node.getApproverId().equals(approverId)) {
                return node;
            }
        }
        return null;
    }

    public Reimburs approve(String reimbursId, String approverId, String comment) {
        Reimburs reimburs = dataStore.reimbursements.get(reimbursId);
        if (reimburs == null) {
            throw new RuntimeException("报销单不存在");
        }

        if (!ReimbursementStatus.PENDING.equals(reimburs.getStatus())) {
            throw new RuntimeException("该报销单不在待审批状态");
        }

        ApprovalNode currentNode = getCurrentNode(reimburs, approverId);
        if (currentNode == null) {
            throw new RuntimeException("当前审批人无权限审批此报销单");
        }

        currentNode.setStatus(ApprovalNodeStatus.APPROVED);
        currentNode.setComment(comment);
        currentNode.setUpdatedAt(LocalDateTime.now());

        ApprovalNode nextNode = getNextPendingNodeAfter(reimburs, currentNode.getNodeOrder());

        if (nextNode != null) {
            nextNode.setStatus(ApprovalNodeStatus.PENDING);
            nextNode.setUpdatedAt(LocalDateTime.now());
            reimburs.setCurrentApproverId(nextNode.getApproverId());
            reimburs.setCurrentApproverName(nextNode.getApproverName());
        } else {
            budgetService.consumeBudget(reimburs.getDepartmentId(), reimburs.getAmount());
            reimburs.setStatus(ReimbursementStatus.COMPLETED);
            reimburs.setCurrentApproverId(null);
            reimburs.setCurrentApproverName(null);
        }

        reimburs.setUpdatedAt(LocalDateTime.now());
        return reimburs;
    }

    public Reimburs reject(String reimbursId, String approverId, String comment) {
        Reimburs reimburs = dataStore.reimbursements.get(reimbursId);
        if (reimburs == null) {
            throw new RuntimeException("报销单不存在");
        }

        if (!ReimbursementStatus.PENDING.equals(reimburs.getStatus())) {
            throw new RuntimeException("该报销单不在待审批状态");
        }

        ApprovalNode currentNode = getCurrentNode(reimburs, approverId);
        if (currentNode == null) {
            throw new RuntimeException("当前审批人无权限审批此报销单");
        }

        currentNode.setStatus(ApprovalNodeStatus.REJECTED);
        currentNode.setComment(comment);
        currentNode.setUpdatedAt(LocalDateTime.now());

        budgetService.releaseBudget(reimburs.getDepartmentId(), reimburs.getAmount());
        invoiceService.releaseInvoice(reimburs.getInvoiceNumber());

        reimburs.setStatus(ReimbursementStatus.REJECTED_RESUBMIT);
        reimburs.setCurrentApproverId(null);
        reimburs.setCurrentApproverName(null);
        reimburs.setRejectReason(comment);
        reimburs.setUpdatedAt(LocalDateTime.now());

        return reimburs;
    }

    public Reimburs resubmit(String reimbursId, Reimburs updateRequest) {
        Reimburs reimburs = dataStore.reimbursements.get(reimbursId);
        if (reimburs == null) {
            throw new RuntimeException("报销单不存在");
        }

        if (!ReimbursementStatus.REJECTED_RESUBMIT.equals(reimburs.getStatus())) {
            throw new RuntimeException("只能重提驳回状态的报销单");
        }

        String newInvoiceNumber = updateRequest.getInvoiceNumber() != null 
            ? updateRequest.getInvoiceNumber() : reimburs.getInvoiceNumber();
        String oldInvoiceNumber = reimburs.getInvoiceNumber();

        if (newInvoiceNumber != null && !newInvoiceNumber.equals(oldInvoiceNumber)) {
            if (invoiceService.isInvoiceUsed(newInvoiceNumber)) {
                throw new RuntimeException("新的发票号已被使用");
            }
        }

        if (updateRequest.getAmount() != null 
            && !updateRequest.getAmount().equals(reimburs.getAmount())) {
            if (!budgetService.checkBudgetAvailable(reimburs.getDepartmentId(), updateRequest.getAmount())) {
                throw new RuntimeException("预算不足");
            }
        }

        Reimburs newReimburs = new Reimburs();
        newReimburs.setId(dataStore.generateId("reimburs"));
        newReimburs.setParentId(reimburs.getId());
        newReimburs.setVersion(reimburs.getVersion() + 1);

        newReimburs.setApplicantId(reimburs.getApplicantId());
        newReimburs.setApplicantName(reimburs.getApplicantName());
        newReimburs.setDepartmentId(reimburs.getDepartmentId());
        newReimburs.setDepartmentName(reimburs.getDepartmentName());
        newReimburs.setTitle(updateRequest.getTitle() != null ? updateRequest.getTitle() : reimburs.getTitle());
        newReimburs.setDescription(updateRequest.getDescription() != null ? updateRequest.getDescription() : reimburs.getDescription());
        newReimburs.setAmount(updateRequest.getAmount() != null ? updateRequest.getAmount() : reimburs.getAmount());
        newReimburs.setInvoiceNumber(newInvoiceNumber);
        newReimburs.setInvoiceImage(updateRequest.getInvoiceImage() != null ? updateRequest.getInvoiceImage() : reimburs.getInvoiceImage());
        newReimburs.getOriginalInvoiceNumbers().addAll(reimburs.getOriginalInvoiceNumbers());
        if (oldInvoiceNumber != null) {
            newReimburs.getOriginalInvoiceNumbers().add(oldInvoiceNumber);
        }

        newReimburs.getApprovalTrail().addAll(copyNodesForHistory(reimburs.getApprovalTrail()));

        newReimburs.setStatus(ReimbursementStatus.DRAFT);
        newReimburs.setCreatedAt(reimburs.getCreatedAt());
        newReimburs.setUpdatedAt(LocalDateTime.now());

        reimburs.setStatus(ReimbursementStatus.REJECTED);
        reimburs.setUpdatedAt(LocalDateTime.now());

        dataStore.reimbursements.put(newReimburs.getId(), newReimburs);
        dataStore.reimbursements.put(reimburs.getId(), reimburs);

        return submitReimbursement(newReimburs.getId());
    }

    private List<ApprovalNode> copyNodesForHistory(List<ApprovalNode> originalNodes) {
        List<ApprovalNode> copies = new ArrayList<>();
        for (ApprovalNode node : originalNodes) {
            if (!ApprovalNodeStatus.SKIPPED.equals(node.getStatus())) {
                ApprovalNode copy = new ApprovalNode();
                copy.setId(dataStore.generateId("node"));
                copy.setReimbursementId(node.getReimbursementId());
                copy.setApprovalLevel(node.getApprovalLevel());
                copy.setApproverId(node.getApproverId());
                copy.setApproverName(node.getApproverName());
                copy.setStatus(node.getStatus());
                copy.setComment(node.getComment());
                copy.setNodeOrder(node.getNodeOrder());
                copy.setCreatedAt(node.getCreatedAt());
                copy.setUpdatedAt(node.getUpdatedAt());
                copies.add(copy);
            }
        }
        return copies;
    }

    private ApprovalNode getNextPendingNodeAfter(Reimburs reimburs, int currentOrder) {
        for (ApprovalNode node : reimburs.getApprovalTrail()) {
            if (ApprovalNodeStatus.SKIPPED.equals(node.getStatus()) 
                && node.getNodeOrder() > currentOrder) {
                return node;
            }
        }
        return null;
    }

    public Reimburs transfer(String reimbursId, String fromApproverId, String toApproverId, String reason) {
        Reimburs reimburs = dataStore.reimbursements.get(reimbursId);
        if (reimburs == null) {
            throw new RuntimeException("报销单不存在");
        }

        if (!ReimbursementStatus.PENDING.equals(reimburs.getStatus())) {
            throw new RuntimeException("该报销单不在待审批状态");
        }

        ApprovalNode currentNode = getCurrentNode(reimburs, fromApproverId);
        if (currentNode == null) {
            throw new RuntimeException("当前审批人无权限转交此报销单");
        }

        User toApprover = dataStore.users.get(toApproverId);
        if (toApprover == null) {
            throw new RuntimeException("目标审批人不存在");
        }

        currentNode.setStatus(ApprovalNodeStatus.TRANSFERRED);
        currentNode.setTransferredFromId(fromApproverId);
        currentNode.setTransferredToId(toApproverId);
        currentNode.setTransferredReason(reason);
        currentNode.setUpdatedAt(LocalDateTime.now());

        ApprovalNode newNode = new ApprovalNode();
        newNode.setId(dataStore.generateId("node"));
        newNode.setReimbursementId(reimburs.getId());
        newNode.setApprovalLevel(currentNode.getApprovalLevel());
        newNode.setApproverId(toApproverId);
        newNode.setApproverName(toApprover.getName());
        newNode.setStatus(ApprovalNodeStatus.PENDING);
        newNode.setNodeOrder(currentNode.getNodeOrder());
        newNode.setCreatedAt(LocalDateTime.now());
        newNode.setUpdatedAt(LocalDateTime.now());

        dataStore.approvalNodes.put(newNode.getId(), newNode);
        reimburs.getApprovalTrail().add(newNode);

        reimburs.setCurrentApproverId(toApproverId);
        reimburs.setCurrentApproverName(toApprover.getName());
        reimburs.setUpdatedAt(LocalDateTime.now());

        return reimburs;
    }

    public Reimburs withdraw(String reimbursId, String applicantId, String comment) {
        Reimburs reimburs = dataStore.reimbursements.get(reimbursId);
        if (reimburs == null) {
            throw new RuntimeException("报销单不存在");
        }

        if (!ReimbursementStatus.PENDING.equals(reimburs.getStatus())) {
            throw new RuntimeException("只能撤回待审批状态的报销单");
        }

        if (!reimburs.getApplicantId().equals(applicantId)) {
            throw new RuntimeException("只有申请人可以撤回报销单");
        }

        for (ApprovalNode node : reimburs.getApprovalTrail()) {
            if (ApprovalNodeStatus.PENDING.equals(node.getStatus())) {
                node.setStatus(ApprovalNodeStatus.WITHDRAWN);
                node.setComment(comment);
                node.setUpdatedAt(LocalDateTime.now());
            }
        }

        budgetService.releaseBudget(reimburs.getDepartmentId(), reimburs.getAmount());
        invoiceService.releaseInvoice(reimburs.getInvoiceNumber());

        reimburs.setStatus(ReimbursementStatus.WITHDRAWN);
        reimburs.setCurrentApproverId(null);
        reimburs.setCurrentApproverName(null);
        reimburs.setUpdatedAt(LocalDateTime.now());

        return reimburs;
    }

    public Reimburs getById(String reimbursId) {
        return dataStore.reimbursements.get(reimbursId);
    }

    public List<Reimburs> getByApplicant(String applicantId) {
        return dataStore.reimbursements.values().stream()
            .filter(r -> r.getApplicantId().equals(applicantId))
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .collect(Collectors.toList());
    }

    public List<Reimburs> getByApprover(String approverId) {
        return dataStore.reimbursements.values().stream()
            .filter(r -> approverId.equals(r.getCurrentApproverId()) 
                && ReimbursementStatus.PENDING.equals(r.getStatus()))
            .sorted((a, b) -> a.getSubmittedAt().compareTo(b.getSubmittedAt()))
            .collect(Collectors.toList());
    }

    public List<Reimburs> getAll() {
        return dataStore.reimbursements.values().stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .collect(Collectors.toList());
    }

    public List<ApprovalNode> getApprovalNodes(String reimbursId) {
        return dataStore.approvalNodes.values().stream()
            .filter(n -> n.getReimbursementId().equals(reimbursId))
            .sorted((a, b) -> a.getNodeOrder().compareTo(b.getNodeOrder()))
            .collect(Collectors.toList());
    }

    public List<Reimburs> getTimeoutReimbursements() {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusHours(approvalTimeoutHours);
        return dataStore.reimbursements.values().stream()
            .filter(r -> ReimbursementStatus.PENDING.equals(r.getStatus()))
            .filter(r -> {
                for (ApprovalNode node : r.getApprovalTrail()) {
                    if (ApprovalNodeStatus.PENDING.equals(node.getStatus())) {
                        return node.getCreatedAt().isBefore(timeoutThreshold);
                    }
                }
                return false;
            })
            .collect(Collectors.toList());
    }
}
