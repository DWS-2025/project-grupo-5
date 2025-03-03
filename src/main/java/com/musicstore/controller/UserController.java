package com.musicstore.controller;

import com.musicstore.model.User;
import com.musicstore.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/follow")
    public ResponseEntity<?> followUser(@RequestBody Map<String, String> request, HttpSession session) {
        try {
            User currentUser = (User) session.getAttribute("user");
            if (currentUser == null) {
                return ResponseEntity.status(401).body("User not authenticated");
            }

            String targetUsername = request.get("username");
            User targetUser = userService.getUserByUsername(targetUsername)
                    .orElseThrow(() -> new RuntimeException("Target user not found"));

            userService.followUser(currentUser.getId(), targetUser.getId(), session);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/unfollow")
    public ResponseEntity<?> unfollowUser(@RequestBody Map<String, String> request, HttpSession session) {
        try {
            User currentUser = (User) session.getAttribute("user");
            if (currentUser == null) {
                return ResponseEntity.status(401).body("User not authenticated");
            }

            String targetUsername = request.get("username");
            User targetUser = userService.getUserByUsername(targetUsername)
                    .orElseThrow(() -> new RuntimeException("Target user not found"));

            userService.unfollowUser(currentUser.getId(), targetUser.getId(), session);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}