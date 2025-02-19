package com.musicstore.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicstore.model.User;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final String FILE_PATH = "data/users.json";
    private final ObjectMapper objectMapper;

    public UserService() {
        this.objectMapper = new ObjectMapper();
        createFileIfNotExists();
    }

    private void createFileIfNotExists() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            try {
                objectMapper.writeValue(file, new ArrayList<User>());
            } catch (IOException e) {
                throw new RuntimeException("Could not initialize storage file", e);
            }
        }
    }

    public List<User> getAllUsers() {
        try {
            return objectMapper.readValue(new File(FILE_PATH), new TypeReference<List<User>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Could not read users", e);
        }
    }

    public Optional<User> getUserByUsername(String username) {
        return getAllUsers().stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst();
    }

    public User saveUser(User user) {
        List<User> users = getAllUsers();
        if (user.getId() == null) {
            if (getUserByUsername(user.getUsername()).isPresent()) {
                throw new RuntimeException("Username already exists");
            }
            user.setId(generateId(users));
            users.add(user);
        } else {
            int index = findUserIndex(users, user.getId());
            if (index != -1) {
                users.set(index, user);
            } else {
                users.add(user);
            }
        }
        saveAllUsers(users);
        return user;
    }

    public Optional<User> authenticateUser(String username, String password) {
        return getAllUsers().stream()
                .filter(user -> user.getUsername().equals(username) 
                        && user.getPassword().equals(password))
                .findFirst();
    }

    public User registerUser(User user) {
        if (user == null) {
            throw new RuntimeException("User cannot be null");
        }
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new RuntimeException("Username cannot be empty");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new RuntimeException("Password cannot be empty");
        }
        return saveUser(user);
    }

    private Long generateId(List<User> users) {
        return users.stream()
                .mapToLong(User::getId)
                .max()
                .orElse(0L) + 1;
    }

    private int findUserIndex(List<User> users, Long id) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private void saveAllUsers(List<User> users) {
        try {
            objectMapper.writeValue(new File(FILE_PATH), users);
        } catch (IOException e) {
            throw new RuntimeException("Could not save users", e);
        }
    }
}