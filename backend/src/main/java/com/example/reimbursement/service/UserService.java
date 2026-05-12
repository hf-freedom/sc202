package com.example.reimbursement.service;

import com.example.reimbursement.entity.User;
import com.example.reimbursement.store.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private DataStore dataStore;

    public User getById(String userId) {
        return dataStore.users.get(userId);
    }

    public List<User> getAll() {
        return new ArrayList<>(dataStore.users.values());
    }

    public List<User> getSuperiors(String userId) {
        List<User> superiors = new ArrayList<>();
        User current = dataStore.users.get(userId);
        while (current != null && current.getSuperiorId() != null) {
            User superior = dataStore.users.get(current.getSuperiorId());
            if (superior != null) {
                superiors.add(superior);
                current = superior;
            } else {
                current = null;
            }
        }
        return superiors;
    }
}
