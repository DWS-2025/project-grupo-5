package com.musicstore.controller;

import com.musicstore.model.User;
import com.musicstore.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import com.musicstore.dto.UserDTO;
import com.musicstore.dto.ArtistDTO;
import com.musicstore.dto.AlbumDTO;
import com.musicstore.dto.ReviewDTO;
import com.musicstore.mapper.UserMapper;
import com.musicstore.mapper.AlbumMapper;
import com.musicstore.mapper.ReviewMapper;
import com.musicstore.mapper.ArtistMapper;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @PostMapping("/follow")
    public ResponseEntity<?> followUser(@RequestBody Map<String, String> request, HttpSession session) {
        try {
            UserDTO currentUser = userMapper.toDTO((User) session.getAttribute("user"));
            if (currentUser == null) {
                return ResponseEntity.status(401).body("User not authenticated");
            }

            String targetUsername = request.get("username");
            UserDTO targetUser = userService.getUserByUsername(targetUsername)
                    .orElseThrow(() -> new RuntimeException("Target user not found"));

            userService.followUser(currentUser.id(), targetUser.id(), session);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/unfollow")
    public ResponseEntity<?> unfollowUser(@RequestBody Map<String, String> request, HttpSession session) {
        try {
            UserDTO currentUser = userMapper.toDTO((User) session.getAttribute("user"));
            if (currentUser == null) {
                return ResponseEntity.status(401).body("User not authenticated");
            }

            String targetUsername = request.get("username");
            UserDTO targetUser = userService.getUserByUsername(targetUsername)
                    .orElseThrow(() -> new RuntimeException("Target user not found"));

            userService.unfollowUser(currentUser.id(), targetUser.id(), session);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}