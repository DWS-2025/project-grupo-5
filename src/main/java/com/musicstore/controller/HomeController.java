package com.musicstore.controller;

import com.musicstore.service.AlbumService;
import com.musicstore.service.UserService;
import com.musicstore.service.ReviewService;
import com.musicstore.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class HomeController {
    @Autowired
    private AlbumService albumService;

    @Autowired
    private UserService userService;

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        model.addAttribute("albums", albumService.getAllAlbums());
        User user = (User) session.getAttribute("user");
        if (user != null) {
            model.addAttribute("user", user);
        } else {
            User anonymousUser = new User();
            anonymousUser.setAnonymous(true);
            model.addAttribute("user", anonymousUser);
        }
        return "album/search";
    }

    @GetMapping("/{id}")
    public String viewAlbum(@PathVariable Long id, Model model) {
        albumService.getAlbumById(id).ifPresent(album -> {
            model.addAttribute("album", album);
            model.addAttribute("reviews", reviewService.getReviewsByAlbumId(id));
        });
        return "album/view";
    }

    @GetMapping("/profile")
    public String profile(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            model.addAttribute("user", user);
            return "user/profile";
        }
        return "redirect:/login";
    }

    @PostMapping("/profile/update")
    public String profileUpdate(@ModelAttribute User updatedUser, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser != null) {
            updatedUser.setId(currentUser.getId());
            updatedUser.setFavoriteAlbumIds(currentUser.getFavoriteAlbumIds());
            User updated = userService.updateUser(updatedUser);
            session.setAttribute("user", updated);
            return "redirect:/profile";
        }
        return "redirect:/login";
    }

}
