package com.musicstore.service;

import com.musicstore.dto.UserDTO;
import com.musicstore.dto.ReviewDTO;
import com.musicstore.mapper.UserMapper;
import com.musicstore.mapper.AlbumMapper;
import com.musicstore.model.Album;
import com.musicstore.model.Review;
import com.musicstore.model.User;
import com.musicstore.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    @Autowired
    private AlbumMapper albumMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public void deleteUser(String username) {
        UserDTO userDTO = getUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get all reviews by this user and delete them
        List<ReviewDTO> userReviews = reviewService.getReviewsByUserId(userDTO.id());

        // Collect all affected album IDs before deleting reviews
        List<Long> affectedAlbumIds = userReviews.stream()
            .map(ReviewDTO::albumId)
            .distinct()
            .toList();

        // Delete all reviews first
        for (ReviewDTO review : userReviews) {
            reviewService.deleteReview(review.albumId(), review.id());
        }

        // Update the average ratings of all affected albums
        for (Long albumId : affectedAlbumIds) {
            albumService.getAlbumById(albumId).ifPresent(albumDTO -> {
                List<ReviewDTO> albumReviews = reviewService.getReviewsByAlbumId(albumId);
                double averageRating = albumReviews.stream()
                    .mapToInt(ReviewDTO::rating)
                    .average()
                    .orElse(0.0);
                albumDTO = albumDTO.updateAverageRating(albumReviews);
                albumService.saveAlbum(albumDTO);
            });
        }

        // Remove user from all albums' favorites
        for (Long albumId : userDTO.favoriteAlbumIds()) {
            albumService.getAlbumById(albumId).ifPresent(albumDTO -> {
                albumDTO.getFavoriteUsers().remove(userDTO.id().toString());
                albumService.saveAlbum(albumDTO);
            });
        }

        // Delete the user
        userRepository.delete(userMapper.toEntity(userDTO));
    }
    public List<String> getUsernamesByAlbumId(Long albumId) {
        return userRepository.findUsernamesByFavoriteAlbumId(albumId);
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<UserDTO> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(userMapper::toDTO);
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
        // Encode password if it's not already encoded
        if (!user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userMapper.toDTO(userRepository.save(user));
    }

    public Optional<UserDTO> authenticateUser(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .map(userMapper::toDTO);
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
    public UserDTO addFavoriteAlbum(Long userId, Long albumId, HttpSession session) {
        UserDTO userDTO = getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return albumService.getAlbumById(albumId).map(albumDTO -> {
            if (!userDTO.favoriteAlbumIds().contains(albumId)) {
                List<Long> updatedFavorites = new ArrayList<>(userDTO.favoriteAlbumIds());
                updatedFavorites.add(albumId);
                UserDTO updatedUserDTO = userDTO.withFavoriteAlbumIds(updatedFavorites);
                UserDTO savedUserDTO = saveUser(updatedUserDTO);
                session.setAttribute("user", savedUserDTO);
                return savedUserDTO;
            }
            return userDTO;
        }).orElseThrow(() -> new RuntimeException("Album not found"));
    }

    public List<Long> getFavoriteAlbums(String username) {
        return getUserByUsername(username)
                .map(UserDTO::favoriteAlbumIds)
                .orElse(new ArrayList<>());
    }

    public boolean isAlbumInFavorites(String username, Long albumId) {
        return getUserByUsername(username)
                .map(userDTO -> userDTO.favoriteAlbumIds().contains(albumId))
                .orElse(false);
    }

    @Transactional
    public UserDTO deleteFavoriteAlbum(Long userId, Long albumId, HttpSession session) {
        UserDTO userDTO = getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return albumService.getAlbumById(albumId).map(albumDTO -> {
            if (userDTO.favoriteAlbumIds().contains(albumId)) {
                List<Long> updatedFavorites = new ArrayList<>(userDTO.favoriteAlbumIds());
                updatedFavorites.remove(albumId);
                UserDTO updatedUserDTO = userDTO.withFavoriteAlbumIds(updatedFavorites);
                UserDTO savedUserDTO = saveUser(updatedUserDTO);
                session.setAttribute("user",savedUserDTO);
                return savedUserDTO;
            }
            throw new RuntimeException("Album not found in user's favorites");
        }).orElseThrow(() -> new RuntimeException("Album not found"));
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
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                byte[] imageData = imageFile.getBytes();
                userDTO = userDTO
                    .withImageData(imageData)
                    .withImageUrl("/api/users/" + (userDTO.id() != null ? userDTO.id() : "") + "/image");
            } catch (IOException e) {
                throw new RuntimeException("Failed to process image file: " + e.getMessage(), e);
            }
        }
        
        return saveUser(userDTO);
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
    public UserDTO followUser(Long followerId, Long targetUserId, HttpSession session) {
        if (followerId.equals(targetUserId)) {
            throw new RuntimeException("Users cannot follow themselves");
        }

        UserDTO followerDTO = getUserById(followerId)
                .orElseThrow(() -> new RuntimeException("Follower user not found"));
        UserDTO targetDTO = getUserById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        if (!followerDTO.following().contains(targetUserId)) {
            List<Long> updatedFollowing = new ArrayList<>(followerDTO.following());
            List<Long> updatedTargetFollowers = new ArrayList<>(targetDTO.followers());
            
            updatedFollowing.add(targetUserId);
            updatedTargetFollowers.add(followerId);

            UserDTO updatedFollowerDTO = followerDTO.withFollowing(updatedFollowing);
            UserDTO updatedTargetDTO = targetDTO.withFollowers(updatedTargetFollowers);
            UserDTO savedFollowerDTO = saveUser(updatedFollowerDTO);
            saveUser(updatedTargetDTO);

            session.setAttribute("user", savedFollowerDTO);
            return savedFollowerDTO;
        }
        return followerDTO;
    }

    @Transactional
    public UserDTO unfollowUser(Long followerId, Long targetUserId, HttpSession session) {
        UserDTO followerDTO = getUserById(followerId)
                .orElseThrow(() -> new RuntimeException("Follower user not found"));
        UserDTO targetDTO = getUserById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        if (followerDTO.following().contains(targetUserId)) {
            List<Long> updatedFollowing = new ArrayList<>(followerDTO.following());
            List<Long> updatedTargetFollowers = new ArrayList<>(targetDTO.followers());
            
            updatedFollowing.remove(targetUserId);
            updatedTargetFollowers.remove(followerId);

            UserDTO updatedFollowerDTO = followerDTO.withFollowing(updatedFollowing);
            UserDTO updatedTargetDTO = targetDTO.withFollowers(updatedTargetFollowers);
            UserDTO savedFollowerDTO = saveUser(updatedFollowerDTO);
            saveUser(updatedTargetDTO);

            session.setAttribute("user", savedFollowerDTO);
            return savedFollowerDTO;
        }
        return followerDTO;
    }

    public boolean isFollowing(Long followerId, Long targetUserId) {
        return getUserById(followerId)
                .map(user -> user.following().contains(targetUserId))
                .orElse(false);
    }
}