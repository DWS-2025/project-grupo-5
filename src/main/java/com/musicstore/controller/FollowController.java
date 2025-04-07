package com.musicstore.controller;

import com.musicstore.model.User;
import com.musicstore.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/follow")
public class FollowController {

    @Autowired
    private UserService userService;

    @PostMapping("/add/{targetUserId}")
    public String followUser(@PathVariable Long targetUserId, HttpSession session, Model model) {

        User currentUser = (User) session.getAttribute("user");
        
        if (currentUser == null) {
            model.addAttribute("error", "You must be logged in to follow users");
            return "error";
        }

        try {
            userService.followUser(currentUser.getId(), targetUserId, session);
            // Update the session with the modified user
            session.setAttribute("user", userService.getUserById(currentUser.getId()).orElse(currentUser));
            return "redirect:/profile/" + userService.getUserById(targetUserId)
                    .map(User::getUsername)
                    .orElse("");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    @PostMapping("/remove/{targetUserId}")
    public String unfollowUser(@PathVariable Long targetUserId, HttpSession session, Model model) {

        User currentUser = (User) session.getAttribute("user");
        
        if (currentUser == null) {
            model.addAttribute("error", "You must be logged in to unfollow users");
            return "error";
        }

        try {
            userService.unfollowUser(currentUser.getId(), targetUserId, session);
            // Update the session with the modified user
            session.setAttribute("user", userService.getUserById(currentUser.getId()).orElse(currentUser));
            return "redirect:/profile/" + userService.getUserById(targetUserId)
                    .map(User::getUsername)
                    .orElse("");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }
}