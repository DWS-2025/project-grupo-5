package com.echoreviews.controller;

import com.echoreviews.model.Album;
import com.echoreviews.model.Review;
import com.echoreviews.model.User;
import com.echoreviews.service.AlbumService;
import com.echoreviews.service.ReviewService;
import com.echoreviews.service.UserService;
import com.echoreviews.service.ArtistService;
import com.echoreviews.model.Artist;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
import com.echoreviews.dto.UserDTO;
import com.echoreviews.dto.ArtistDTO;
import com.echoreviews.dto.AlbumDTO;
import com.echoreviews.dto.ReviewDTO;
import com.echoreviews.dto.ProfileUpdateDTO;
import com.echoreviews.mapper.UserMapper;
import com.echoreviews.mapper.AlbumMapper;
import com.echoreviews.mapper.ReviewMapper;
import com.echoreviews.mapper.ArtistMapper;

@Controller
public class ProfileController{
    @Autowired
    private AlbumService albumService;

    @Autowired
    private UserService userService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ArtistService artistService;

    @Autowired
    private UserMapper userMapper;

    @GetMapping("/profile")
    public String profile(
            @RequestParam(name = "userIdToEdit", required = false) Long userIdToEdit,
            Model model,
            HttpSession session) {

        UserDTO sessionUser = (UserDTO) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/login";
        }

        UserDTO userToDisplay;

        if (userIdToEdit != null && sessionUser.isAdmin()) {
            Optional<UserDTO> targetUserOpt = userService.getUserById(userIdToEdit);
            if (targetUserOpt.isEmpty()) {
                model.addAttribute("error", "User to edit not found.");
                userToDisplay = sessionUser; 
            } else {
                userToDisplay = targetUserOpt.get();
                model.addAttribute("editingUserAsAdmin", true);
            }
        } else {
            userToDisplay = sessionUser;
        }

        model.addAttribute("user", userToDisplay);
        model.addAttribute("profileUser", userToDisplay);
        return "user/profile";
    }

    @PostMapping("/profile/update")
    public String profileUpdate(
            @ModelAttribute ProfileUpdateDTO profileUpdateDTO,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "userIdBeingEdited", required = false) Long userIdBeingEdited,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {

        UserDTO sessionUser = (UserDTO) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/login";
        }

        UserDTO userToUpdate;
        boolean isAdminEditingOther = false;

        if (userIdBeingEdited != null && sessionUser.isAdmin()) {
            Optional<UserDTO> targetUserOpt = userService.getUserById(userIdBeingEdited);
            if (targetUserOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "User to update not found.");
                return "redirect:/admin/users";
            }
            userToUpdate = targetUserOpt.get();
            isAdminEditingOther = true;
        } else if (userIdBeingEdited == null || userIdBeingEdited.equals(sessionUser.id())) {
            userToUpdate = sessionUser;
        } else {
            redirectAttributes.addFlashAttribute("error", "Unauthorized to update this profile.");
            return "redirect:/profile";
        }

        String newPlainPassword = null;
        if (profileUpdateDTO.isPasswordChangeRequested()) {
            if (!profileUpdateDTO.isPasswordChangeValid()) {
                model.addAttribute("error", "New password and confirmation do not match or are invalid.");
                model.addAttribute("user", userToUpdate);
                model.addAttribute("editingUserAsAdmin", isAdminEditingOther);
                model.addAttribute("profileUser", userToUpdate);
                return "user/profile";
            }
            if (!isAdminEditingOther) {
                if (profileUpdateDTO.currentPassword() == null || userService.authenticateUser(userToUpdate.username(), profileUpdateDTO.currentPassword()).isEmpty()) {
                    model.addAttribute("error", "La contraseña actual es incorrecta");
                    model.addAttribute("user", userToUpdate);
                    model.addAttribute("editingUserAsAdmin", isAdminEditingOther);
                    model.addAttribute("profileUser", userToUpdate);
                    return "user/profile";
                }
            }
            newPlainPassword = profileUpdateDTO.newPassword();
        }

        String passwordForUpdate = userToUpdate.password(); // Existing hashed password
        if (newPlainPassword != null) {
            passwordForUpdate = newPlainPassword; // New plain password for service to hash
        }

        UserDTO updatedUserDTO = new UserDTO(
            userToUpdate.id(),
            (profileUpdateDTO.username() != null && !profileUpdateDTO.username().isBlank()) ? profileUpdateDTO.username() : userToUpdate.username(),
            passwordForUpdate, // This will be new plain or old hashed. Service must handle.
            (profileUpdateDTO.email() != null && !profileUpdateDTO.email().isBlank()) ? profileUpdateDTO.email() : userToUpdate.email(),
            userToUpdate.isAdmin(), // Admin status not changed here
            userToUpdate.imageUrl(), // Default to old, might be overwritten by imageFile
            userToUpdate.imageData(), // Default to old
            userToUpdate.followers(),
            userToUpdate.following(),
            userToUpdate.favoriteAlbumIds()
        );

        try {
            UserDTO savedUser;
            if (imageFile != null && !imageFile.isEmpty()) {
                 UserDTO userDtoForImageSave = new UserDTO(
                    updatedUserDTO.id(), updatedUserDTO.username(), updatedUserDTO.password(), updatedUserDTO.email(),
                    updatedUserDTO.isAdmin(), null, null, // Null out image fields for new image
                    updatedUserDTO.followers(), updatedUserDTO.following(), updatedUserDTO.favoriteAlbumIds()
                );
                savedUser = userService.saveUserWithProfileImage(userDtoForImageSave, imageFile);
            } else {
                savedUser = userService.saveUser(updatedUserDTO);
            }

            if (!isAdminEditingOther) {
                session.setAttribute("user", savedUser);
                redirectAttributes.addFlashAttribute("success", "Perfil actualizado correctamente.");
                return "redirect:/profile";
            } else {
                redirectAttributes.addFlashAttribute("success", "Perfil de '" + savedUser.username() + "' actualizado correctamente.");
                return "redirect:/admin/users";
            }

        } catch (RuntimeException | IOException e) {
            model.addAttribute("error", "Error al actualizar el perfil: " + e.getMessage());
            model.addAttribute("user", userToUpdate);
            model.addAttribute("editingUserAsAdmin", isAdminEditingOther);
            model.addAttribute("profileUser", userToUpdate);
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

    @GetMapping("/profile/change-password")
    public String showChangePasswordForm(Model model, HttpSession session) {
        UserDTO currentUser = (UserDTO) session.getAttribute("user");
        if (currentUser == null) {
            return "redirect:/login";
        }
        return "user/change-password";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@RequestParam String currentPassword,
                               @RequestParam String newPassword,
                               @RequestParam String confirmPassword,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        UserDTO currentUser = (UserDTO) session.getAttribute("user");
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Verificar que la contraseña actual sea correcta
        if (userService.authenticateUser(currentUser.username(), currentPassword).isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "La contraseña actual es incorrecta");
            return "redirect:/profile/change-password";
        }

        // Verificar que las nuevas contraseñas coincidan
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Las contraseñas nuevas no coinciden");
            return "redirect:/profile/change-password";
        }

        // Validar el formato de la nueva contraseña
        if (!newPassword.matches("^(?=.*[0-9])(?=.*[A-Z])(?=.*[!@#$%^&*(),.?\":{}|<>])(?=\\S+$).{8,25}$")) {
            redirectAttributes.addFlashAttribute("error", 
                "La nueva contraseña debe tener entre 8 y 25 caracteres y contener al menos un número, " +
                "una mayúscula y un carácter especial");
            return "redirect:/profile/change-password";
        }

        // Actualizar la contraseña
        UserDTO updatedUser = new UserDTO(
            currentUser.id(),
            currentUser.username(),
            newPassword,
            currentUser.email(),
            currentUser.isAdmin(),
            currentUser.imageUrl(),
            currentUser.imageData(),
            currentUser.followers(),
            currentUser.following(),
            currentUser.favoriteAlbumIds()
        );

        try {
            userService.updateUser(updatedUser);
            session.setAttribute("user", updatedUser);
            redirectAttributes.addFlashAttribute("success", "Contraseña actualizada correctamente");
            return "redirect:/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar la contraseña: " + e.getMessage());
            return "redirect:/profile/change-password";
        }
    }
}
