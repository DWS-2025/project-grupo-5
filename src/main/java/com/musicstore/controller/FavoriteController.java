package com.musicstore.controller;

import com.musicstore.dto.AlbumDTO;
import com.musicstore.model.Album;
import com.musicstore.model.User;
import com.musicstore.service.AlbumService;
import com.musicstore.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import com.musicstore.dto.UserDTO;
import com.musicstore.mapper.UserMapper;

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
    public String addFavorite(@RequestParam Long albumId,
                              HttpSession session,
                              Model model) {
        try {
            // Obtain the user in the actual session
            UserDTO user = (UserDTO) session.getAttribute("user");
            if (user == null || user.id() == null) {
                model.addAttribute("error", "No session started.");
                return "error";
            }

            Long auxUserId = user.id();

            // Search album
            Optional<AlbumDTO> albumOptional = albumService.getAlbumById(albumId);
            if (albumOptional.isEmpty()) {
                model.addAttribute("error", "Album not found.");
                return "error";
            }

            Album album = albumOptional.get().toAlbum();

            // Add album to the favorites section of the user
            userService.addFavoriteAlbum(auxUserId, albumId, session);

            UserDTO currentUser = userService.getUserById(auxUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            User userEntity = currentUser.toUser();
            if (!album.getFavoriteUsers().contains(userEntity)) {
                album.getFavoriteUsers().add(userEntity);
                albumService.saveAlbum(AlbumDTO.fromAlbum(album)); // Save changes
            }

            return "redirect:/" + albumId; // Redirect to the favorites page
        } catch (Exception e) {
            model.addAttribute("error", "Ocurrió un error al añadir el álbum a favoritos: " + e.getMessage());
            return "error"; // Render the error page
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

            Album album = albumOptional.get().toAlbum();

            // Delete the album of the user favorites
            userService.deleteFavoriteAlbum(userId, albumId, session);

            // Delete the user from the album's favorite users list
            UserDTO currentUser = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            User userEntity = currentUser.toUser();
            if (album.getFavoriteUsers().contains(userEntity)) {
                album.getFavoriteUsers().remove(userEntity);
                albumService.saveAlbum(AlbumDTO.fromAlbum(album)); // Save the album
            }

            return "redirect:/" + albumId; // Render the favorites page
        } catch (Exception e) {
            model.addAttribute("error", "Ocurrió un error al eliminar el álbum de favoritos: " + e.getMessage());
            return "error"; // Render the error page
        }
    }

    @GetMapping("/{username}")
    public String showFavorites(@PathVariable String username, HttpSession session, Model model) {

        // Get the requested user's favorite albums
        List<Long> favoriteAlbumIds = userService.getFavoriteAlbums(username);

        if (favoriteAlbumIds == null) {
            model.addAttribute("error", "Usuario no encontrado.");
            return "error";
        }

        List<AlbumDTO> favoriteAlbums = favoriteAlbumIds.stream()
                .map(albumId -> albumService.getAlbumById(albumId).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Get the user information (including the profile image)
        Optional<UserDTO> userOpt = userService.getUserByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get().toUser();
            model.addAttribute("userProfileImage", user.getImageUrl()); // Add user profile image URL
        }

        // Get the current logged-in user (if any)
        UserDTO currentUser = (UserDTO) session.getAttribute("user");
        boolean isOwnProfile = currentUser != null && currentUser.username().equals(username);

        // Add data to the model
        model.addAttribute("username", username);
        model.addAttribute("favoriteAlbums", favoriteAlbums);
        model.addAttribute("isOwnProfile", isOwnProfile);
        return "album/favorites";
    }
}

