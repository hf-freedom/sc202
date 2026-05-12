package com.example.reimbursement.controller;

import com.example.reimbursement.entity.User;
import com.example.reimbursement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<User> list = userService.getAll();
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
            User user = userService.getById(id);
            if (user == null) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return ResponseEntity.ok(result);
            }
            result.put("success", true);
            result.put("data", user);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @GetMapping("/{id}/superiors")
    public ResponseEntity<Map<String, Object>> getSuperiors(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<User> list = userService.getSuperiors(id);
            result.put("success", true);
            result.put("data", list);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
}
