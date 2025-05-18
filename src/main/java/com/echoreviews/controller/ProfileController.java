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
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

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

import java.nio.file.Path;
import java.nio.file.Paths;

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
        model.addAttribute("editingUserAsAdmin", false);

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
        model.addAttribute("username", userToDisplay.username());
        return "user/profile";
    }

    @PostMapping("/profile/update")
    public String profileUpdate(
            @ModelAttribute ProfileUpdateDTO profileUpdateDTO,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "pdfFile", required = false) MultipartFile pdfFile,
            @RequestParam(value = "userIdBeingEdited", required = false) Long userIdBeingEdited,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {

        UserDTO sessionUser = (UserDTO) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/login";
        }

        // Log para depuración
        System.out.println("Profile Update - Username: " + profileUpdateDTO.username());
        System.out.println("Profile Update - Email: " + profileUpdateDTO.email());

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
                    model.addAttribute("error", "Current password is incorrect");
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

        // Ensure that username and email are not empty
        String username = (profileUpdateDTO.username() != null && !profileUpdateDTO.username().isBlank()) 
                        ? profileUpdateDTO.username() 
                        : userToUpdate.username();
        
        String email = (profileUpdateDTO.email() != null && !profileUpdateDTO.email().isBlank()) 
                     ? profileUpdateDTO.email() 
                     : userToUpdate.email();

        UserDTO updatedUserDTO = new UserDTO(
            userToUpdate.id(),
            username,
            passwordForUpdate, // This will be new plain or old hashed. Service must handle.
            email,
            userToUpdate.isAdmin(), // Admin status not changed here
            userToUpdate.potentiallyDangerous(), // Preserve existing flag
            userToUpdate.banned(), // Preserve existing flag
            userToUpdate.imageUrl(), // Default to old, might be overwritten by imageFile
            userToUpdate.imageData(), // Default to old
            userToUpdate.followers() != null ? new ArrayList<>(userToUpdate.followers()) : new ArrayList<>(),
            userToUpdate.following() != null ? new ArrayList<>(userToUpdate.following()) : new ArrayList<>(),
            userToUpdate.favoriteAlbumIds(),
            userToUpdate.pdfPath() // Mantener el PDF path existente
        );

        try {
            UserDTO savedUser = updatedUserDTO;
            
            // Process image file if uploaded
            if (imageFile != null && !imageFile.isEmpty()) {
                UserDTO userDtoForImageSave = new UserDTO(
                    updatedUserDTO.id(), updatedUserDTO.username(), updatedUserDTO.password(), updatedUserDTO.email(),
                    updatedUserDTO.isAdmin(), updatedUserDTO.potentiallyDangerous(), updatedUserDTO.banned(), 
                    null, null, // Null out image fields for new image
                    updatedUserDTO.followers(), updatedUserDTO.following(), updatedUserDTO.favoriteAlbumIds(),
                    updatedUserDTO.pdfPath() // Mantener el PDF path
                );
                savedUser = userService.saveUserWithProfileImage(userDtoForImageSave, imageFile);
            } else {
                savedUser = userService.saveUser(updatedUserDTO);
            }
            
            // Process PDF file if uploaded
            if (pdfFile != null && !pdfFile.isEmpty()) {
                UserService.PdfUploadResult pdfResult = userService.uploadUserPdf(savedUser, pdfFile);
                if (pdfResult.isSuccess()) {
                    savedUser = pdfResult.getUser();
                } else {
                    throw new IOException("Error uploading PDF: " + pdfResult.getErrorMessage());
                }
            }

            if (!isAdminEditingOther) {
                session.setAttribute("user", savedUser);
                redirectAttributes.addFlashAttribute("success", "Profile updated successfully.");
                return "redirect:/profile";
            } else {
                redirectAttributes.addFlashAttribute("success", "Profile of '" + savedUser.username() + "' updated successfully.");
                return "redirect:/admin/users";
            }

        } catch (RuntimeException | IOException e) {
            model.addAttribute("error", "Error updating profile: " + e.getMessage());
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
        // Logged in user
        UserDTO currentUserDTO = (UserDTO) session.getAttribute("user");
        model.addAttribute("currentUser", currentUserDTO);

        // Profile user
        Optional<UserDTO> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            model.addAttribute("error", "User not found");
            return "error";
        }

        UserDTO profileUserDTO = userOpt.get();
        model.addAttribute("profileUser", profileUserDTO);
        model.addAttribute("username", username);

        // Followers
        Map<String, String> followersUsers = profileUserDTO.followers().stream()
                .map(userService::getUserById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(
                        UserDTO::username,
                        UserDTO::imageUrl,
                        (a, b) -> a,
                        HashMap::new
                ));

        // Following
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
            // Update session with new image
            UserDTO updatedUser = userService.getUserByUsername(currentUserDTO.username()).orElse(currentUserDTO);
            session.setAttribute("user", updatedUser);
        } catch (IOException e) {
            model.addAttribute("error", "Error uploading profile image");
            model.addAttribute("user", currentUserDTO);
            return "user/profile";
        }
        return "redirect:/profile?reload=" + System.currentTimeMillis();
    }

    @GetMapping("/profile/change-password")
    public String showChangePasswordForm(
            @RequestParam(required = false) Long userIdToEdit,
            Model model, 
            HttpSession session) {
        UserDTO sessionUser = (UserDTO) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/login";
        }

        if (userIdToEdit != null && sessionUser.isAdmin()) {
            Optional<UserDTO> targetUserOpt = userService.getUserById(userIdToEdit);
            if (targetUserOpt.isEmpty()) {
                model.addAttribute("error", "User not found");
                return "redirect:/admin/users";
            }
            model.addAttribute("user", targetUserOpt.get());
            model.addAttribute("editingUserAsAdmin", true);
        } else {
            model.addAttribute("user", sessionUser);
        }

        return "user/change-password";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            @RequestParam(required = false) Long userIdToEdit,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        UserDTO sessionUser = (UserDTO) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/login";
        }

        // Determine which user is being modified
        UserDTO userToUpdate;
        if (userIdToEdit != null && sessionUser.isAdmin()) {
            Optional<UserDTO> targetUserOpt = userService.getUserById(userIdToEdit);
            if (targetUserOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/admin/users";
            }
            userToUpdate = targetUserOpt.get();
        } else {
            userToUpdate = sessionUser;
        }

        // Verify that the current password is correct (only for non-admin users or admin changing their own password)
        boolean isAdminChangingOtherUser = sessionUser.isAdmin() && !userToUpdate.id().equals(sessionUser.id());
        
        // Skip password verification if admin is changing another user's password
        if (!isAdminChangingOtherUser) {
            if (userService.authenticateUser(userToUpdate.username(), currentPassword).isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Current password is incorrect");
                return "redirect:/profile/change-password" + (userIdToEdit != null ? "?userIdToEdit=" + userIdToEdit : "");
            }
        }

        // Verify that the new passwords match
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "New passwords do not match");
            return "redirect:/profile/change-password" + (userIdToEdit != null ? "?userIdToEdit=" + userIdToEdit : "");
        }

        // Validate the format of the new password
        if (!newPassword.matches("^(?=.*[0-9])(?=.*[A-Z])(?=.*[!@#$%^&*(),.?\":{}|<>])(?=\\S+$).{8,25}$")) {
            redirectAttributes.addFlashAttribute("error", 
                "The new password must be between 8 and 25 characters and contain at least one number, " +
                "one uppercase letter, and one special character");
            return "redirect:/profile/change-password" + (userIdToEdit != null ? "?userIdToEdit=" + userIdToEdit : "");
        }

        // Update the password
        UserDTO updatedUser = new UserDTO(
            userToUpdate.id(),
            userToUpdate.username(),
            newPassword,
            userToUpdate.email(),
            userToUpdate.isAdmin(),
            userToUpdate.potentiallyDangerous(),
            userToUpdate.banned(),
            userToUpdate.imageUrl(),
            userToUpdate.imageData(),
            userToUpdate.followers(),
            userToUpdate.following(),
            userToUpdate.favoriteAlbumIds(),
            userToUpdate.pdfPath() // Mantener el PDF path existente
        );

        try {
            userService.updateUser(updatedUser);
            if (!sessionUser.isAdmin() || userToUpdate.id().equals(sessionUser.id())) {
                session.setAttribute("user", updatedUser);
            }
            redirectAttributes.addFlashAttribute("success", "Password updated successfully");
            return sessionUser.isAdmin() && !userToUpdate.id().equals(sessionUser.id()) 
                   ? "redirect:/admin/users" 
                   : "redirect:/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating password: " + e.getMessage());
            return "redirect:/profile/change-password" + (userIdToEdit != null ? "?userIdToEdit=" + userIdToEdit : "");
        }
    }

    @PostMapping("/profile/upload-pdf")
    public String uploadUserPdf(@RequestParam("pdfFile") MultipartFile pdfFile, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        UserDTO currentUserDTO = (UserDTO) session.getAttribute("user");
        if (currentUserDTO == null) {
            return "redirect:/login";
        }
        
        UserService.PdfUploadResult result = userService.uploadUserPdf(currentUserDTO, pdfFile);
        
        if (result.isSuccess()) {
            session.setAttribute("user", result.getUser());
            redirectAttributes.addFlashAttribute("success", "PDF uploaded successfully");
        } else {
            redirectAttributes.addFlashAttribute("error", result.getErrorMessage());
        }
        
        return "redirect:/profile/" + currentUserDTO.username();
    }

    @PostMapping("/profile/delete-pdf")
    public String deleteUserPdf(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        UserDTO currentUserDTO = (UserDTO) session.getAttribute("user");
        if (currentUserDTO == null) {
            return "redirect:/login";
        }
        
        try {
            UserDTO updatedUser = userService.deleteUserPdf(currentUserDTO);
            session.setAttribute("user", updatedUser);
            redirectAttributes.addFlashAttribute("success", "PDF deleted successfully");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting PDF: " + e.getMessage());
        }
        
        return "redirect:/profile";
    }

    @GetMapping("/profile/{userId}/pdf")
    public ResponseEntity<Resource> viewUserPdf(@PathVariable Long userId, HttpSession session) {
        try {
            // Verify that there is a user in session
            UserDTO currentUser = (UserDTO) session.getAttribute("user");
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Verify that the user is trying to access their own PDF
            if (!currentUser.id().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Optional<UserDTO> userOpt = userService.getUserById(userId);
            if (userOpt.isEmpty() || userOpt.get().pdfPath() == null) {
                return ResponseEntity.notFound().build();
            }
            
            String pdfPath = userOpt.get().pdfPath();
            // Construir la ruta absoluta desde la raíz del proyecto
            Path filePath = Paths.get(".", pdfPath).normalize().toAbsolutePath();
            
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
}
