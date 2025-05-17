package com.echoreviews.controller;

import com.echoreviews.dto.AlbumDTO;
import com.echoreviews.model.Album;
import com.echoreviews.model.User;
import com.echoreviews.service.AlbumService;
import com.echoreviews.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import com.echoreviews.dto.UserDTO;
import com.echoreviews.dto.ArtistDTO;
import com.echoreviews.dto.ReviewDTO;
import com.echoreviews.mapper.UserMapper;
import com.echoreviews.mapper.AlbumMapper;
import com.echoreviews.mapper.ReviewMapper;
import com.echoreviews.mapper.ArtistMapper;

@Controller
@RequestMapping("/favorites")
public class FavoriteController {

    @Autowired
    private UserService userService;

    @Autowired
    private AlbumService albumService;

    @Autowired
    private UserMapper userMapper;

    // Add album to favorites

    @PostMapping("/add")
    public String addFavorite(@RequestParam Long albumId, HttpSession session, Model model) {
        try {
            // Get the user from the session
            UserDTO userDTO = (UserDTO) session.getAttribute("user");
            if (userDTO == null || userDTO.id() == null) {
                model.addAttribute("error", "No session started.");
                return "error";
            }

            Long auxUserId = userDTO.id();

            // Find the album
            Optional<AlbumDTO> albumOptional = albumService.getAlbumById(albumId);
            if (albumOptional.isEmpty()) {
                model.addAttribute("error", "Album not found.");
                return "error";
            }

            AlbumDTO albumDTO = albumOptional.get();

            // Add the album to the user's favorites list
            userService.addFavoriteAlbum(auxUserId, albumId, session);

            // Mapear UserDTO a User
            Long userId = userDTO.id();
            UserDTO currentUser = UserDTO.fromUser(userMapper.toEntity(userService.getUserById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"))));
            if (albumDTO.getFavoriteUsers().contains(currentUser)) {
                albumDTO.getFavoriteUsers().add(currentUser.username());
                albumService.saveAlbum(AlbumDTO.fromAlbum(albumDTO.toAlbum())); // Save the album
            }

            return "redirect:/album/" + albumId; // Redirect to the album page
        } catch (Exception e) {
            model.addAttribute("error", "An error occurred while adding the album to favorites: " + e.getMessage());
            return "error"; // Show error page
        }
    }



    @PostMapping("/delete")
    public String deleteFavorite(@RequestParam Long albumId,
                                 HttpSession session,
                                 Model model) {
        try {
            // Obtain the user in the actual session
            UserDTO user = (UserDTO) session.getAttribute("user");
            if (user == null || user.id() == null) {
                model.addAttribute("error", "No session started.");
                return "error";
            }

            Long userId = user.id();

            // Search album
            Optional<AlbumDTO> albumOptional = albumService.getAlbumById(albumId);
            if (albumOptional.isEmpty()) {
                model.addAttribute("error", "Album not found.");
                return "error";
            }

            AlbumDTO album = AlbumDTO.fromAlbum(albumOptional.get().toAlbum());

            // Delete the album of the user favorites
            userService.deleteFavoriteAlbum(userId, albumId, session);

            // Delete the user from the album's favorite users list
            UserDTO currentUser = UserDTO.fromUser(userMapper.toEntity(userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"))));
            if (album.getFavoriteUsers().contains(currentUser)) {
                album.getFavoriteUsers().remove(currentUser);
                albumService.saveAlbum(AlbumDTO.fromAlbum(album.toAlbum())); // Save the album
            }

            return "redirect:/album/" + albumId; // Render the favorites page
        } catch (Exception e) {
            model.addAttribute("error", "An error occurred while removing the album from favorites: " + e.getMessage());
            return "error"; // Render the error page
        }
    }


    @GetMapping("/{username}")
    public String showFavorites(@PathVariable String username, HttpSession session, Model model) {

        Optional<UserDTO> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            model.addAttribute("error", "User not found.");
            return "error";
        }

        List<Long> favoriteAlbumIds = userService.getFavoriteAlbums(username);
        List<AlbumDTO> favoriteAlbums = !favoriteAlbumIds.isEmpty() ?
                favoriteAlbumIds.stream()
                        .map(albumService::getAlbumById)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList()) :
                Collections.emptyList();

        UserDTO user = userOpt.get();
        model.addAttribute("userProfileImage", user.imageUrl());

        UserDTO currentUser = (UserDTO) session.getAttribute("user");
        boolean isOwnProfile = currentUser != null && currentUser.username().equals(username);

        model.addAttribute("username", username);
        model.addAttribute("favoriteAlbums", favoriteAlbums);
        model.addAttribute("isOwnProfile", isOwnProfile);
        return "album/favorites";
    }

}

