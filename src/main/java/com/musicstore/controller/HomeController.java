package com.musicstore.controller;

import com.musicstore.model.Album;
import com.musicstore.model.Review;
import com.musicstore.service.AlbumService;
import com.musicstore.service.UserService;
import com.musicstore.service.ReviewService;
import com.musicstore.model.User;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


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
        return "album/welcome";
    }

    @GetMapping("/{id}")
    public String viewAlbum(@PathVariable Long id, Model model) {
        if (albumService.getAlbumById(id).isEmpty()) {
            model.addAttribute("error", "Album not found");
            return "error";
        }

        albumService.getAlbumById(id).ifPresent(album -> {
            model.addAttribute("album", album);
            List<String> usernames = album.getFavoriteUsers().stream()
                    .map(userId -> userService.getUserById(Long.parseLong(userId))
                            .map(User::getUsername)
                            .orElse("Unknown User"))
                    .collect(Collectors.toList());
            model.addAttribute("favoriteUsernames", usernames);

            // Get reviews and map user IDs to usernames and profile images
            List<Review> reviews = reviewService.getReviewsByAlbumId(id);
            reviews.forEach(review -> {
                userService.getUserById(review.getUserId())
                        .ifPresent(user -> {
                            review.setUsername(user.getUsername());
                            review.setUserImageUrl(user.getImageUrl());
                        });
            });
            model.addAttribute("reviews", reviews);
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
    public String profileUpdate(
            @ModelAttribute User updatedUser,
            String currentPassword,
            String newPassword,
            String confirmPassword,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            HttpSession session,
            Model model
    ) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser != null) {
            // Verify current password
            if (!userService.authenticateUser(currentUser.getUsername(), currentPassword).isPresent()) {
                model.addAttribute("error", "Current Password is incorrect");
                model.addAttribute("user", currentUser);
                return "error";
            }

            // Handle password update if new password is provided
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                if (!newPassword.equals(confirmPassword)) {
                    model.addAttribute("error", "New passwords do not match");
                    model.addAttribute("user", currentUser);
                    return "user/profile";
                }
                updatedUser.setPassword(newPassword);
            } else {
                updatedUser.setPassword(currentUser.getPassword());
            }

            updatedUser.setId(currentUser.getId());
            updatedUser.setFavoriteAlbumIds(currentUser.getFavoriteAlbumIds());
            updatedUser.setFollowers(currentUser.getFollowers());
            updatedUser.setFollowing(currentUser.getFollowing());

            try {
                // Handle profile image upload if provided
                if (imageFile != null && !imageFile.isEmpty()) {
                    try {
                        userService.saveUserWithProfileImage(updatedUser, imageFile);
                    } catch (IOException e) {
                        model.addAttribute("error", "Error uploading profile image");
                        model.addAttribute("user", currentUser);
                        return "user/profile";
                    }
                } else {
                    // If no new image, keep the existing one
                    updatedUser.setImageUrl(currentUser.getImageUrl());
                    userService.updateUser(updatedUser);
                }

                session.setAttribute("user", updatedUser);
                return "redirect:/profile";
            } catch (RuntimeException e) {
                model.addAttribute("error", e.getMessage());
                model.addAttribute("user", currentUser);
                return "user/profile";
            }
        }
        return "redirect:/login";
    }


    @PostMapping("/profile/delete")
    public String deleteAccount(HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Get all reviews by this user
        List<Review> userReviews = reviewService.getReviewsByUserId(currentUser.getId());

        // Collect all affected album IDs before deleting reviews
        List<Long> affectedAlbumIds = userReviews.stream()
                .map(Review::getAlbumId)
                .distinct()
                .toList();

        // Delete the user account (this will also delete all reviews and update favorites)
        userService.deleteUser(currentUser.getUsername());

        // Update average ratings for all affected albums
        for (Long albumId : affectedAlbumIds) {
            albumService.getAlbumById(albumId).ifPresent(album -> {
                album.updateAverageRating(reviewService.getReviewsByAlbumId(albumId));
                albumService.saveAlbum(album);
            });
        }



        // Invalidate session
        session.invalidate();

        return "redirect:/login";
    }

    @GetMapping("/profile/{username}")
    public String viewProfile(@PathVariable String username, Model model, HttpSession session) {
        // Get the current logged-in user if any
        User currentUser = (User) session.getAttribute("user");
        model.addAttribute("currentUser", currentUser);

        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            model.addAttribute("error", "User not found");
            return "error";
        }

        User profileUser = userOpt.get();
        model.addAttribute("profileUser", profileUser);

        // Get followers and following usernames with their profile images
        Map<String, String> followersUsers = profileUser.getFollowers().stream()
                .map(id -> userService.getUserById(id))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(User::getUsername, User::getImageUrl, (a, b) -> a));

        Map<String, String> followingUsers = profileUser.getFollowing().stream()
                .map(id -> userService.getUserById(id))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(User::getUsername, User::getImageUrl, (a, b) -> a));

        model.addAttribute("followersUsers", followersUsers);
        model.addAttribute("followingUsers", followingUsers);

        // Get favorite albums
        List<Album> favoriteAlbums = profileUser.getFavoriteAlbumIds().stream()
                .map(albumId -> albumService.getAlbumById(albumId))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        Collections.reverse(favoriteAlbums);
        favoriteAlbums = favoriteAlbums.stream().limit(5).collect(Collectors.toList());
        model.addAttribute("favoriteAlbums", favoriteAlbums);

        // Get reviews and associate them with albums
        List<Review> userReviews = reviewService.getReviewsByUserId(profileUser.getId());
        userReviews.forEach(review -> {
            albumService.getAlbumById(review.getAlbumId()).ifPresent(album -> {
                review.setAlbumTitle(album.getTitle());
                review.setAlbumImageUrl(album.getImageUrl());
            });
        });

        Collections.reverse(userReviews);
        userReviews = userReviews.stream().limit(5).collect(Collectors.toList());
        model.addAttribute("userReviews", userReviews);

        return "user/profile-view";
    }
    }

