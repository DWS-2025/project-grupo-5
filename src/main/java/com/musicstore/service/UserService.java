package com.musicstore.service;

import com.musicstore.dto.UserDTO;
import com.musicstore.mapper.UserMapper;
import com.musicstore.model.Album;
import com.musicstore.model.Review;
import com.musicstore.model.User;
import com.musicstore.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReviewService reviewService;
    @Autowired
    private AlbumService albumService;
    @Autowired
    private UserMapper userMapper;

    @Transactional
    public void deleteUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get all reviews by this user and delete them
        List<Review> userReviews = reviewService.getReviewsByUserId(user.getId());

        // Collect all affected album IDs before deleting reviews
        List<Long> affectedAlbumIds = userReviews.stream()
            .map(Review::getAlbumId)
            .distinct()
            .toList();

        // Delete all reviews first
        for (Review review : userReviews) {
            reviewService.deleteReview(review.getAlbumId(), review.getId());
        }

        // Update the average ratings of all affected albums
        for (Long albumId : affectedAlbumIds) {
            albumService.getAlbumById(albumId).ifPresent(album -> {
                album.updateAverageRating(reviewService.getReviewsByAlbumId(albumId));
                albumService.saveAlbum(album);
            });
        }

        // Remove user from all albums' favorites
        for (Album album : user.getFavoriteAlbums()) {
            album.getFavoriteUsers().remove(user.getId().toString());
            albumService.saveAlbum(album);
        }

        // Delete the user
        userRepository.delete(user);
    }
    public List<String> getUsernamesByAlbumId(Long albumId) {
        return userRepository.findUsernamesByFavoriteAlbumId(albumId);
    }

    public List<UserDTO> getAllUsers() {
        return userMapper.toDTOList(userRepository.findAll());
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<UserDTO> getUserById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDTO);
    }

    @Transactional
    public UserDTO saveUser(UserDTO userDTO) {
        if (userDTO.id() == null) {
            if (userRepository.existsByUsername(userDTO.username())) {
                throw new RuntimeException("Username already exists");
            }
        }
        User user = userMapper.toEntity(userDTO);
        return userMapper.toDTO(userRepository.save(user));
    }

    public Optional<User> authenticateUser(String username, String password) {
        return userRepository.findByUsernameAndPassword(username, password);
    }

    @Transactional
    public UserDTO registerUser(UserDTO userDTO) {
        if (userDTO == null) {
            throw new RuntimeException("User cannot be null");
        }
        if (userDTO.username() == null || userDTO.username().trim().isEmpty()) {
            throw new RuntimeException("Username cannot be empty");
        }
        if (userDTO.email() == null || userDTO.email().trim().isEmpty()) {
            throw new RuntimeException("Email cannot be empty");
        }
        if (userRepository.existsByUsername(userDTO.username())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(userDTO.email())) {
            throw new RuntimeException("Email already exists");
        }
        return saveUser(userDTO);
    }

    @Transactional
    public void addFavoriteAlbum(Long userId, Long albumId, HttpSession session) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        albumService.getAlbumById(albumId).ifPresent(album -> {
            if (!user.getFavoriteAlbums().contains(album)) {
                user.getFavoriteAlbums().add(album);
                userRepository.save(user);
                session.setAttribute("user", user);
            }
        });
    }

    public List<Long> getFavoriteAlbums(String username) {
        return getUserByUsername(username)
                .<List<Long>>map(user -> user.getFavoriteAlbums().stream()
                        .map(Album::getId)
                        .toList())
                .orElse(new ArrayList<>());
    }

    public boolean isAlbumInFavorites(String username, Long albumId) {
        return getUserByUsername(username)
                .<Boolean>map(user -> user.getFavoriteAlbums().stream()
                        .anyMatch(album -> album.getId().equals(albumId)))
                .orElse(false);
    }

    @Transactional
    public void deleteFavoriteAlbum(Long userId, Long albumId, HttpSession session) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        albumService.getAlbumById(albumId).ifPresent(album -> {
            if (user.getFavoriteAlbums().remove(album)) {
                userRepository.save(user);
                session.setAttribute("user", user);
            } else {
                throw new RuntimeException("Album not found in user's favorites");
            }
        });
    }

    /*
    @Transactional
    public void saveUserWithProfileImage(User user, MultipartFile profileImage) throws IOException {
        if (profileImage == null || profileImage.isEmpty()) {
            throw new RuntimeException("Profile image cannot be null or empty");
        }

        String contentType = profileImage.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Only image files are allowed");
        }

        String originalFilename = profileImage.getOriginalFilename();
        String fileExtension = originalFilename != null ? 
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        String filename = "user_" + user.getId() + "_" + System.currentTimeMillis() + fileExtension;

        File uploadDir = new File("src/main/resources/static/images");
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        File destFile = new File(uploadDir.getAbsolutePath() + File.separator + filename);
        profileImage.transferTo(destFile);

        user.setImageUrl("/images/" + filename);
        userRepository.save(user);
    }*/

    public UserDTO saveUserWithProfileImage(UserDTO userDTO, MultipartFile imageFile) throws IOException {
        User user = userMapper.toEntity(userDTO);
        User savedUser = userRepository.save(user);

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                byte[] imageData = imageFile.getBytes();
                savedUser.setImageData(imageData);
                savedUser.setImageUrl("/api/users/" + savedUser.getId() + "/image");
                return userMapper.toDTO(userRepository.save(savedUser));
            } catch (IOException e) {
                throw new RuntimeException("Failed to process image file: " + e.getMessage(), e);
            }
        }
        return userMapper.toDTO(savedUser);
    }


    @Transactional
    public UserDTO updateUser(UserDTO updatedUserDTO) {
        if (updatedUserDTO == null || updatedUserDTO.id() == null) {
            throw new RuntimeException("User or user ID cannot be null");
        }

        User existingUser = userRepository.findById(updatedUserDTO.id())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if the new username is already taken by another user
        Optional<User> userWithUsername = userRepository.findByUsername(updatedUserDTO.username());
        if (userWithUsername.isPresent() && !userWithUsername.get().getId().equals(updatedUserDTO.id())) {
            throw new RuntimeException("Username already exists");
        }

        User updatedUser = userMapper.toEntity(updatedUserDTO);
        // Preserve the password if not provided in the update
        if (updatedUser.getPassword() == null || updatedUser.getPassword().trim().isEmpty()) {
            updatedUser.setPassword(existingUser.getPassword());
        }

        return userMapper.toDTO(userRepository.save(updatedUser));
    }

    @Transactional
    public void followUser(Long followerId, Long targetUserId, HttpSession session) {
        if (followerId.equals(targetUserId)) {
            throw new RuntimeException("Users cannot follow themselves");
        }

        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new RuntimeException("Follower user not found"));
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        if (!follower.getFollowing().contains(targetUserId)) {
            follower.getFollowing().add(targetUserId);
            target.getFollowers().add(followerId);

            userRepository.save(follower);
            userRepository.save(target);

            session.setAttribute("user", follower);
        }
    }

    @Transactional
    public void unfollowUser(Long followerId, Long targetUserId, HttpSession session) {
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new RuntimeException("Follower user not found"));
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        if (follower.getFollowing().contains(targetUserId)) {
            follower.getFollowing().remove(targetUserId);
            target.getFollowers().remove(followerId);

            userRepository.save(follower);
            userRepository.save(target);

            session.setAttribute("user", follower);
        }
    }

    public boolean isFollowing(Long followerId, Long targetUserId) {
        return getUserById(followerId)
                .map(user -> user.following().contains(targetUserId))
                .orElse(false);
    }
}