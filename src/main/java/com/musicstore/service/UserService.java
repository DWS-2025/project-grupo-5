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
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final ReviewService reviewService;
    private final AlbumService albumService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, @Lazy ReviewService reviewService, @Lazy AlbumService albumService, UserMapper userMapper, @Lazy PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.reviewService = reviewService;
        this.albumService = albumService;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        List<GrantedAuthority> authorities = new ArrayList<>();
        if (user.isAdmin()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), authorities);
    }

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
        return userMapper.toDTOList(userRepository.findAll());
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
        User user = userMapper.toEntity(userDTO);
        
        if (userDTO.password() != null && !userDTO.password().isBlank()) {
            // Only encode if password is provided and not already looking like a BCrypt hash
            if (!userDTO.password().startsWith("$2a$") && !userDTO.password().startsWith("$2b$") && !userDTO.password().startsWith("$2y$")) {
                 user.setPassword(passwordEncoder.encode(userDTO.password()));
            } else {
                // Password seems already encoded, or user wants to keep existing one (passed as already encoded)
                user.setPassword(userDTO.password());
            }
        } else if (user.getId() != null) {
            // Existing user and password in DTO is null/blank, retain old password from DB
            User existingUserFromDb = userRepository.findById(user.getId()).orElseThrow(() -> new RuntimeException("User not found for password retention"));
            user.setPassword(existingUserFromDb.getPassword());
        } else {
            // New user and password is blank - this should ideally be caught by validation earlier
            throw new IllegalArgumentException("Password cannot be blank for a new user.");
        }

        if (userDTO.id() == null) { // New user
            if (userRepository.existsByUsername(userDTO.username())) {
                throw new RuntimeException("Username '" + userDTO.username() + "' already exists");
            }
            if (userDTO.email() != null && userRepository.existsByEmail(userDTO.email())) {
                throw new RuntimeException("Email '" + userDTO.email() + "' already exists");
            }
        } else { // Existing user
            User existingUser = userRepository.findById(userDTO.id()).orElseThrow(() -> new RuntimeException("User not found with ID: " + userDTO.id()));
            if (!existingUser.getUsername().equals(userDTO.username()) && userRepository.existsByUsername(userDTO.username())) {
                throw new RuntimeException("Username '" + userDTO.username() + "' already exists for another user");
            }
            if (userDTO.email() != null && !existingUser.getEmail().equals(userDTO.email()) && userRepository.existsByEmail(userDTO.email())) {
                 throw new RuntimeException("Email '" + userDTO.email() + "' already exists for another user");
            }
            // Preserve isAdmin status from DB for existing user unless specifically managed elsewhere
            user.setAdmin(existingUser.isAdmin());
        }
        // For a new user, DTO's isAdmin will be used (which is false by default from registration)
        // or true if an admin creates another admin (though this method might not be directly used for that)

        return userMapper.toDTO(userRepository.save(user));
    }

    public Optional<UserDTO> authenticateUser(String username, String password) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                return Optional.of(userMapper.toDTO(user));
            }
        }
        return Optional.empty();
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
        if (userDTO.password() == null || userDTO.password().trim().isEmpty()) {
             throw new RuntimeException("Password cannot be empty");
        }
        // Password complexity should be validated before calling this service method (e.g. in controller or DTO validation)
        return saveUser(userDTO.withIsAdmin(false)); 
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
            throw new RuntimeException("User or user ID cannot be null for update");
        }

        User existingUser = userRepository.findById(updatedUserDTO.id())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + updatedUserDTO.id() + " for update"));

        User userToUpdate = userMapper.toEntity(updatedUserDTO); // Converts DTO to an entity instance
        userToUpdate.setAdmin(existingUser.isAdmin()); // IMPORTANT: Preserve isAdmin status from DB

        // Handle password update: 
        // If password field in DTO is not empty, it means an attempt to change.
        if (updatedUserDTO.password() != null && !updatedUserDTO.password().trim().isEmpty()) {
            // Only encode if the new password is not the same as the old one (already encoded) 
            // and it doesn't look like a BCrypt hash already.
            if (!passwordEncoder.matches(updatedUserDTO.password(), existingUser.getPassword()) && 
                !(updatedUserDTO.password().startsWith("$2a$") || updatedUserDTO.password().startsWith("$2b$") || updatedUserDTO.password().startsWith("$2y$"))) {
                userToUpdate.setPassword(passwordEncoder.encode(updatedUserDTO.password()));
            } else if (updatedUserDTO.password().startsWith("$2a$") || updatedUserDTO.password().startsWith("$2b$") || updatedUserDTO.password().startsWith("$2y$")) {
                // If it looks like a hash, assume it's intentional to set an already hashed password (e.g. migration)
                userToUpdate.setPassword(updatedUserDTO.password());
            } else {
                 // Password in DTO is plain text but matches the existing one, so no change needed, keep existing hash.
                userToUpdate.setPassword(existingUser.getPassword());
            }
        } else {
            // Password in DTO is null or empty, so keep the existing password from DB.
            userToUpdate.setPassword(existingUser.getPassword());
        }
        
        // Check for username and email conflicts before saving
        if (!existingUser.getUsername().equals(updatedUserDTO.username()) && userRepository.existsByUsername(updatedUserDTO.username())) {
            throw new RuntimeException("Username '" + updatedUserDTO.username() + "' already exists for another user");
        }
        if (updatedUserDTO.email() != null && !existingUser.getEmail().equals(updatedUserDTO.email()) && userRepository.existsByEmail(updatedUserDTO.email())) {
            throw new RuntimeException("Email '" + updatedUserDTO.email() + "' already exists for another user");
        }

        User savedUser = userRepository.save(userToUpdate);
        return userMapper.toDTO(savedUser);
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