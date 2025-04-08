package com.musicstore.controller;

import com.musicstore.model.Album;
import com.musicstore.model.Review;
import com.musicstore.model.User;
import com.musicstore.service.AlbumService;
import com.musicstore.service.ReviewService;
import com.musicstore.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.rowset.serial.SerialException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.HashMap;
import com.musicstore.dto.UserDTO;
import com.musicstore.dto.ArtistDTO;
import com.musicstore.dto.AlbumDTO;
import com.musicstore.dto.ReviewDTO;
import com.musicstore.mapper.UserMapper;
import com.musicstore.mapper.AlbumMapper;
import com.musicstore.mapper.ReviewMapper;
import com.musicstore.mapper.ArtistMapper;

@Controller
public class ProfileController{
    @Autowired
    private AlbumService albumService;

    @Autowired
    private UserService userService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private UserMapper userMapper;

    @GetMapping("/profile")
    public String profile(Model model, HttpSession session) {
        UserDTO user = (UserDTO) session.getAttribute("user");
        if (user != null) {
            model.addAttribute("user", user);
            return "user/profile";
        }
        return "redirect:/login";
    }

    @PostMapping("/profile/update")
    public String profileUpdate(
            @ModelAttribute UserDTO updatedUserDTO,
            String currentPassword,
            String newPassword,
            String confirmPassword,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            HttpSession session,
            Model model
    ) {
        UserDTO currentUserDTO = (UserDTO) session.getAttribute("user");
        if (currentUserDTO != null) {
            // Verify current password
            if (userService.authenticateUser(currentUserDTO.username(), currentPassword).isEmpty()) {
                model.addAttribute("error", "Current Password is incorrect");
                model.addAttribute("user", currentUserDTO);
                return "error";
            }

            // Handle password update if new password is provided
            String finalPassword = currentUserDTO.password();
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                if (!newPassword.equals(confirmPassword)) {
                    model.addAttribute("error", "New passwords do not match");
                    model.addAttribute("user", currentUserDTO);
                    return "user/profile";
                }
                finalPassword = newPassword;
            }

            // Create updated UserDTO
            UserDTO newUserDTO = new UserDTO(
                    currentUserDTO.id(),
                    updatedUserDTO.username(),
                    finalPassword,
                    updatedUserDTO.email(),
                    currentUserDTO.isAdmin(),
                    currentUserDTO.imageUrl(),
                    currentUserDTO.imageData(),
                    currentUserDTO.followers(),
                    currentUserDTO.following(),
                    currentUserDTO.favoriteAlbumIds()
            );

            try {
                // Handle profile image upload if provided
                if (imageFile != null && !imageFile.isEmpty()) {
                    try {
                        userService.saveUserWithProfileImage(newUserDTO, imageFile);
                    } catch (IOException e) {
                        model.addAttribute("error", "Error uploading profile image");
                        model.addAttribute("user", currentUserDTO);
                        return "user/profile";
                    }
                } else {
                    // If no new image, keep the existing one
                    userService.saveUser(newUserDTO);
                }

                session.setAttribute("user", newUserDTO);
                return "redirect:/profile?reload=" + System.currentTimeMillis();
            } catch (RuntimeException e) {
                model.addAttribute("error", e.getMessage());
                model.addAttribute("user", currentUserDTO);
                return "user/profile";
            }
        }
        return "redirect:/login";
    }

    @PostMapping("/profile/delete")
    public String deleteAccount(HttpSession session) {
        UserDTO currentUserDTO = (UserDTO) session.getAttribute("user");
        if (currentUserDTO == null) {
            return "redirect:/login";
        }

        // Get all reviews by this user
        List<ReviewDTO> userReviews = reviewService.getReviewsByUserId(currentUserDTO.id());

        // Collect all affected album IDs before deleting reviews
        List<Long> affectedAlbumIds = userReviews.stream()
                .map(ReviewDTO::albumId)
                .distinct()
                .toList();

        // Delete the user account (this will also delete all reviews and update favorites)
        userService.deleteUser(currentUserDTO.username());

        // Update average ratings for all affected albums
        for (Long albumId : affectedAlbumIds) {
            albumService.getAlbumById(albumId).ifPresent(albumDTO -> {
                albumDTO.updateAverageRating(reviewService.getReviewsByAlbumId(albumId));
                albumService.saveAlbum(albumDTO);
            });
        }

        // Invalidate session
        session.invalidate();

        return "redirect:/login";
    }

    @GetMapping("/profile/{username}")
    public String viewProfile(@PathVariable String username, Model model, HttpSession session) {
        // Usuario logueado
        UserDTO currentUserDTO = (UserDTO) session.getAttribute("user");
        model.addAttribute("currentUser", currentUserDTO);

        // Usuario del perfil
        Optional<UserDTO> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            model.addAttribute("error", "User not found");
            return "error";
        }

        UserDTO profileUserDTO = userOpt.get();
        model.addAttribute("profileUser", profileUserDTO);

        // Seguidores
        Map<String, String> followersUsers = profileUserDTO.followers().stream()
                .map(userService::getUserById) // Optional<UserDTO>
                .filter(Optional::isPresent)
                .map(Optional::get) // UserDTO directamente
                .collect(Collectors.toMap(
                        UserDTO::username,
                        UserDTO::imageUrl,
                        (a, b) -> a,
                        HashMap::new
                ));


        // Siguiendo
        Map<String, String> followingUsers = profileUserDTO.following().stream()
                .map(userService::getUserById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(
                        UserDTO::username,
                        UserDTO::imageUrl,
                        (a, b) -> a,
                        HashMap::new
                ));

        model.addAttribute("followersUsers", followersUsers);
        model.addAttribute("followingUsers", followingUsers);

        // Álbumes favoritos
        List<Long> favoriteAlbumIds = userService.getFavoriteAlbums(username);
        List<AlbumDTO> favoriteAlbums = favoriteAlbumIds.stream()
                .map(albumService::getAlbumById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        model.addAttribute("favoriteAlbums", favoriteAlbums);
        model.addAttribute("totalLikes", favoriteAlbums);

        // Reseñas
        List<ReviewDTO> userReviews = reviewService.getReviewsByUserId(profileUserDTO.id());

        userReviews.forEach(review -> {
            albumService.getAlbumById(review.albumId()).ifPresent(album -> {
                review.setAlbumTitle(album.title());
                review.setAlbumImageUrl(album.imageUrl());
            });
        });

        Collections.reverse(userReviews);
        model.addAttribute("totalReviews", userReviews);

        List<ReviewDTO> userReviews2 = userReviews.stream().limit(5).collect(Collectors.toList());
        model.addAttribute("userReviews", userReviews2);

        return "user/profile-view";
    }

}
