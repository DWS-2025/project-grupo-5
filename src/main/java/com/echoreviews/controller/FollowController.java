package com.echoreviews.controller;

import com.echoreviews.model.User;
import com.echoreviews.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.echoreviews.dto.UserDTO;
import com.echoreviews.dto.ArtistDTO;
import com.echoreviews.dto.AlbumDTO;
import com.echoreviews.dto.ReviewDTO;
import com.echoreviews.mapper.UserMapper;
import com.echoreviews.mapper.AlbumMapper;
import com.echoreviews.mapper.ReviewMapper;
import com.echoreviews.mapper.ArtistMapper;

@Controller
@RequestMapping("/follow")
public class FollowController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @PostMapping("/add/{targetUserId}")
    public String followUser(@PathVariable Long targetUserId, HttpSession session, Model model) {

        UserDTO currentUser = (UserDTO) session.getAttribute("user");
        
        if (currentUser == null) {
            model.addAttribute("error", "You must be logged in to follow users");
            return "error";
        }

        try {
            userService.followUser(currentUser.id(), targetUserId, session);
            // Update the session with the modified user
            session.setAttribute("user", userService.getUserById(currentUser.id())
                    .map(user -> userMapper.toDTO(user.toUser()))
                    .orElse(currentUser));
            return "redirect:/profile/" + userService.getUserById(targetUserId)
                    .map(UserDTO::username)
                    .orElse("");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    @PostMapping("/remove/{targetUserId}")
    public String unfollowUser(@PathVariable Long targetUserId, HttpSession session, Model model) {

        UserDTO currentUser = (UserDTO) session.getAttribute("user");
        
        if (currentUser == null) {
            model.addAttribute("error", "You must be logged in to unfollow users");
            return "error";
        }

        try {
            userService.unfollowUser(currentUser.id(), targetUserId, session);
            // Update the session with the modified user
            session.setAttribute("user", userService.getUserById(currentUser.id())
                    .map(user -> userMapper.toDTO(user.toUser()))
                    .orElse(currentUser));
            return "redirect:/profile/" + userService.getUserById(targetUserId)
                    .map(UserDTO::username)
                    .orElse("");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }
}