package com.echoreviews.controller.api;

import com.echoreviews.model.User;
import com.echoreviews.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import com.echoreviews.dto.UserDTO;

import java.util.Optional;
import java.util.stream.Collectors;
import com.echoreviews.dto.ArtistDTO;
import com.echoreviews.dto.AlbumDTO;
import com.echoreviews.dto.ReviewDTO;
import com.echoreviews.mapper.UserMapper;
import com.echoreviews.mapper.AlbumMapper;
import com.echoreviews.mapper.ReviewMapper;
import com.echoreviews.mapper.ArtistMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Map;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/users")
public class UserRestController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        List<UserDTO> userDTOs = new ArrayList<>(users);
        return ResponseEntity.ok(userDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(UserDTO.fromUser(user.toUser())))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<UserDTO> getUserByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username)
                .map(user -> ResponseEntity.ok(UserDTO.fromUser(user.toUser())))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO userDTO) {
        if (userService.getUserByUsername(userDTO.username()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        UserDTO savedUser = userService.saveUser(userDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long id,
            @RequestBody UserDTO userDTO) {
        return userService.getUserById(id)
                .map(existingUser -> {
                    UserDTO updatedUserDTO = userDTO.withId(id);
                    UserDTO savedUser = userService.saveUser(updatedUserDTO);
                    return ResponseEntity.ok(savedUser);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteUser(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> {
                    userService.deleteUser(user.username());
                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<UserDTO> uploadUserImage(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile image) {
        return userService.getUserById(id)
                .map(user -> {
                    try {
                        UserDTO updatedUser = user.withImageData(image.getBytes());
                        UserDTO savedUser = userService.saveUser(updatedUser);
                        return ResponseEntity.ok(savedUser);
                    } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<UserDTO>build();
                    }
                })
                .orElseGet(() -> ResponseEntity.of(Optional.<UserDTO>empty()));
    }

    @GetMapping("/{username}/followers")
    public ResponseEntity<List<UserDTO>> getUserFollowers(@PathVariable String username) {
        try {
            Optional<UserDTO> userOpt = userService.getUserByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            List<UserDTO> followers = userOpt.get().followers().stream()
                    .map(userId -> userService.getUserById(userId))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(followers);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{username}/following")
    public ResponseEntity<List<UserDTO>> getUserFollowing(@PathVariable String username) {
        try {
            Optional<UserDTO> userOpt = userService.getUserByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            List<UserDTO> following = userOpt.get().following().stream()
                    .map(userId -> userService.getUserById(userId))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(following);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Endpoint para seguir/dejar de seguir a un usuario.
     * Si el usuario ya está siendo seguido, lo deja de seguir.
     * Si no lo está siguiendo, comienza a seguirlo.
     * 
     * @param targetUserId ID del usuario al que se quiere seguir/dejar de seguir
     * @param session Sesión HTTP con la información del usuario autenticado
     * @return El usuario actualizado después de la operación
     */
    @PostMapping("/follow/{targetUserId}")
    public ResponseEntity<?> toggleFollowUser(@PathVariable Long targetUserId, HttpSession session) {
        // Verificar si hay un usuario autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
        
        try {
            // Obtener el nombre de usuario del usuario autenticado
            String username = "";
            if (auth.getPrincipal() instanceof UserDetails) {
                username = ((UserDetails) auth.getPrincipal()).getUsername();
            } else {
                username = auth.getName();
            }
            
            // Buscar el usuario por su nombre de usuario
            UserDTO currentUser = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Current user not found"));
            
            // Obtener el usuario objetivo
            UserDTO targetUser = userService.getUserById(targetUserId)
                    .orElseThrow(() -> new RuntimeException("Target user not found"));
            
            // Verificar si el usuario ya está siguiendo al objetivo
            boolean isFollowing = userService.isFollowing(currentUser.id(), targetUserId);
            
            UserDTO updatedUser;
            if (isFollowing) {
                // Dejar de seguir al usuario
                updatedUser = userService.unfollowUser(currentUser.id(), targetUserId, session);
                return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "action", "unfollow",
                    "message", "You have unfollowed " + targetUser.username()
                ));
            } else {
                // Comenzar a seguir al usuario
                updatedUser = userService.followUser(currentUser.id(), targetUserId, session);
                return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "action", "follow",
                    "message", "You are now following " + targetUser.username()
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}
