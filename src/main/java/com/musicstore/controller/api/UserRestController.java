package com.musicstore.controller.api;

import com.musicstore.model.User;
import com.musicstore.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import com.musicstore.dto.UserDTO;

import java.util.Optional;
import java.util.stream.Collectors;
import com.musicstore.dto.ArtistDTO;
import com.musicstore.dto.AlbumDTO;
import com.musicstore.dto.ReviewDTO;
import com.musicstore.mapper.UserMapper;
import com.musicstore.mapper.AlbumMapper;
import com.musicstore.mapper.ReviewMapper;
import com.musicstore.mapper.ArtistMapper;

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
                .map(user -> ResponseEntity.ok(UserDTO.fromUser(user)))
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
}
