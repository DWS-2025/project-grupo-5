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
import com.musicstore.dto.ProfileUpdateDTO;
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
            @ModelAttribute ProfileUpdateDTO profileUpdateDTO,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            HttpSession session,
            Model model
    ) {
        UserDTO currentUserDTO = (UserDTO) session.getAttribute("user");
        if (currentUserDTO == null) {
            return "redirect:/login";
        }
        
        // Validar los datos de actualización
        if (profileUpdateDTO.isPasswordChangeRequested()) {
            // Validar cambio de contraseña
            if (!profileUpdateDTO.isPasswordChangeValid()) {
                model.addAttribute("error", "Datos de cambio de contraseña inválidos");
                model.addAttribute("user", currentUserDTO);
                return "user/profile";
            }
            
            // Verificar la contraseña actual
            if (userService.authenticateUser(currentUserDTO.username(), profileUpdateDTO.currentPassword()).isEmpty()) {
                model.addAttribute("error", "La contraseña actual es incorrecta");
                model.addAttribute("user", currentUserDTO);
                return "user/profile";
            }
        }

        // Determinar la contraseña final
        String finalPassword = profileUpdateDTO.isPasswordChangeRequested() 
                ? profileUpdateDTO.newPassword() 
                : currentUserDTO.password();

        // Crear un nuevo UserDTO preservando los datos que no se actualizan
        UserDTO newUserDTO = new UserDTO(
                currentUserDTO.id(),
                profileUpdateDTO.username(),
                finalPassword,
                profileUpdateDTO.email(),
                currentUserDTO.isAdmin(),
                currentUserDTO.imageUrl(),
                currentUserDTO.imageData(),
                currentUserDTO.followers(),
                currentUserDTO.following(),
                currentUserDTO.favoriteAlbumIds()
        );

        try {
            // Manejar la carga de imagen de perfil si se proporciona
            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    // Actualizar con la nueva imagen
                    UserDTO savedUserDTO = userService.saveUserWithProfileImage(newUserDTO, imageFile);
                    // Actualizar la sesión con el usuario guardado que incluye la nueva imagen
                    session.setAttribute("user", savedUserDTO);
                } catch (IOException e) {
                    model.addAttribute("error", "Error al subir la imagen de perfil");
                    model.addAttribute("user", currentUserDTO);
                    return "user/profile";
                }
            } else {
                // Si no hay nueva imagen, guardar los cambios y actualizar la sesión
                UserDTO savedUserDTO = userService.saveUser(newUserDTO);
                session.setAttribute("user", savedUserDTO);
            }

            return "redirect:/profile?reload=" + System.currentTimeMillis();
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("user", currentUserDTO);
            return "user/profile";
        }
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

        // Favorite albums
        List<Long> favoriteAlbumIds = userService.getFavoriteAlbums(username);
        List<AlbumDTO> favoriteAlbums = favoriteAlbumIds.stream()
                .map(albumService::getAlbumById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        model.addAttribute("favoriteAlbums", favoriteAlbums);
        model.addAttribute("totalLikes", favoriteAlbums);

        // Reviews
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

    @PostMapping("/profile/upload-image")
    public String uploadProfileImage(@RequestParam("imageFile") MultipartFile imageFile, HttpSession session, Model model) {
        UserDTO currentUserDTO = (UserDTO) session.getAttribute("user");
        if (currentUserDTO == null) {
            return "redirect:/login";
        }
        try {
            userService.saveUserWithProfileImage(currentUserDTO, imageFile);
            // Actualizar la sesión con la nueva imagen
            UserDTO updatedUser = userService.getUserByUsername(currentUserDTO.username()).orElse(currentUserDTO);
            session.setAttribute("user", updatedUser);
        } catch (IOException e) {
            model.addAttribute("error", "Error al subir la imagen de perfil");
            model.addAttribute("user", currentUserDTO);
            return "user/profile";
        }
        return "redirect:/profile?reload=" + System.currentTimeMillis();
    }
}
