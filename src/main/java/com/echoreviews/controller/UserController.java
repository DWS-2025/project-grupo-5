package com.echoreviews.controller;

import com.echoreviews.model.User;
import com.echoreviews.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import com.echoreviews.dto.UserDTO;
import com.echoreviews.dto.ArtistDTO;
import com.echoreviews.dto.AlbumDTO;
import com.echoreviews.dto.ReviewDTO;
import com.echoreviews.mapper.UserMapper;
import com.echoreviews.mapper.AlbumMapper;
import com.echoreviews.mapper.ReviewMapper;
import com.echoreviews.mapper.ArtistMapper;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @PostMapping("/follow")
    public ResponseEntity<?> followUser(@RequestBody Map<String, String> request, HttpSession session) {
        UserDTO currentUser = (UserDTO) session.getAttribute("user");

        if (currentUser == null) {
            return ResponseEntity.status(401).body("User not authenticated");
        }

        try {
            String targetUsername = request.get("username");
            UserDTO targetUser = userService.getUserByUsername(targetUsername)
                    .orElseThrow(() -> new RuntimeException("Target user not found"));

            userService.followUser(currentUser.id(), targetUser.id(), session);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error following user: " + e.getMessage());
        }
    }

    @PostMapping("/unfollow")
    public ResponseEntity<?> unfollowUser(@RequestBody Map<String, String> request, HttpSession session) {
        UserDTO currentUser = (UserDTO) session.getAttribute("user");

        if (currentUser == null) {
            return ResponseEntity.status(401).body("User not authenticated");
        }

        try {
            String targetUsername = request.get("username");
            UserDTO targetUser = userService.getUserByUsername(targetUsername)
                    .orElseThrow(() -> new RuntimeException("Target user not found"));

            userService.unfollowUser(currentUser.id(), targetUser.id(), session);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error unfollowing user: " + e.getMessage());
        }
    }
}
