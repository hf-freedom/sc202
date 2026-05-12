package com.example.reimbursement.store;

import com.example.reimbursement.entity.*;
import com.example.reimbursement.enums.ApprovalLevel;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DataStore {

    public final Map<String, User> users = new ConcurrentHashMap<>();
    public final Map<String, Department> departments = new ConcurrentHashMap<>();
    public final Map<String, Budget> budgets = new ConcurrentHashMap<>();
    public final Map<String, Reimburs> reimbursements = new ConcurrentHashMap<>();
    public final Map<String, ApprovalNode> approvalNodes = new ConcurrentHashMap<>();
    public final Map<String, InvoiceCache> invoiceCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        initDepartments();
        initUsers();
        initBudgets();
    }

    private void initDepartments() {
        departments.put("dept_tech", new Department("dept_tech", "技术部", "user_dept_head_tech"));
        departments.put("dept_sales", new Department("dept_sales", "销售部", "user_dept_head_sales"));
        departments.put("dept_finance", new Department("dept_finance", "财务部", "user_finance_head"));
    }

    private void initUsers() {
        users.put("user_emp1", new User("user_emp1", "员工张三", "dept_tech", null, "user_mgr1", new ArrayList<>()));
        users.put("user_emp2", new User("user_emp2", "员工李四", "dept_tech", null, "user_mgr1", new ArrayList<>()));
        users.put("user_emp3", new User("user_emp3", "员工王五", "dept_sales", null, "user_mgr2", new ArrayList<>()));
        users.put("user_mgr1", new User("user_mgr1", "直属经理赵六", "dept_tech", ApprovalLevel.DIRECT_MANAGER, "user_dept_head_tech", Arrays.asList("user_emp1", "user_emp2")));
        users.put("user_mgr2", new User("user_mgr2", "直属经理钱七", "dept_sales", ApprovalLevel.DIRECT_MANAGER, "user_dept_head_sales", Arrays.asList("user_emp3")));
        users.put("user_dept_head_tech", new User("user_dept_head_tech", "技术部总监孙八", "dept_tech", ApprovalLevel.DEPARTMENT_HEAD, null, Arrays.asList("user_mgr1")));
        users.put("user_dept_head_sales", new User("user_dept_head_sales", "销售部总监周九", "dept_sales", ApprovalLevel.DEPARTMENT_HEAD, null, Arrays.asList("user_mgr2")));
        users.put("user_finance_1", new User("user_finance_1", "财务人员吴十", "dept_finance", ApprovalLevel.FINANCE, "user_finance_head", new ArrayList<>()));
        users.put("user_finance_head", new User("user_finance_head", "财务总监郑十一", "dept_finance", ApprovalLevel.FINANCE, null, Arrays.asList("user_finance_1")));
    }

    private void initBudgets() {
        budgets.put("dept_tech", new Budget("dept_tech", "技术部", new BigDecimal("100000.00"), new BigDecimal("0.00"), new BigDecimal("0.00")));
        budgets.put("dept_sales", new Budget("dept_sales", "销售部", new BigDecimal("200000.00"), new BigDecimal("0.00"), new BigDecimal("0.00")));
        budgets.put("dept_finance", new Budget("dept_finance", "财务部", new BigDecimal("50000.00"), new BigDecimal("0.00"), new BigDecimal("0.00")));
    }

    public synchronized String generateId(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 6);
    }
}
