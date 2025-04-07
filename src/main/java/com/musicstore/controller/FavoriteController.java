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

import java.util.Collections;
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
    public String addFavorite(@RequestParam Long albumId, HttpSession session, Model model) {
        try {
            UserDTO userDTO = (UserDTO) session.getAttribute("user");
            if (userDTO == null || userDTO.id() == null) {
                model.addAttribute("error", "No session started.");
                return "error";
            }

            Long userId = userDTO.id();
            AlbumDTO album = albumService.getAlbumById(albumId)
                    .orElseThrow(() -> new RuntimeException("Album not found"));

            UserDTO user = userService.getUserById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Añadir a favoritos si no está
            if (!album.getFavoriteUsers().contains(user)) {
                album.getFavoriteUsers().add(String.valueOf((user)));
                albumService.saveAlbum(album);
            }

            return "redirect:/" + albumId;
        } catch (Exception e) {
            model.addAttribute("error", "Ocurrió un error al añadir el álbum a favoritos: " + e.getMessage());
            return "error";
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
            User currentUser = userMapper.toEntity(userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found")));
            if (album.getFavoriteUsers().contains(currentUser)) {
                album.getFavoriteUsers().remove(currentUser);
                albumService.saveAlbum(AlbumDTO.fromAlbum(album.toAlbum())); // Save the album
            }

            return "redirect:/" + albumId; // Render the favorites page
        } catch (Exception e) {
            model.addAttribute("error", "Ocurrió un error al eliminar el álbum de favoritos: " + e.getMessage());
            return "error"; // Render the error page
        }
    }


    @GetMapping("/{username}")
    public String showFavorites(@PathVariable String username, HttpSession session, Model model) {

        Optional<UserDTO> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            model.addAttribute("error", "Usuario no encontrado.");
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

