package com.echoreviews.service;

import com.echoreviews.dto.UserDTO;
import com.echoreviews.dto.ReviewDTO;
import com.echoreviews.mapper.UserMapper;
import com.echoreviews.mapper.AlbumMapper;
import com.echoreviews.model.Album;
import com.echoreviews.model.Review;
import com.echoreviews.model.User;
import com.echoreviews.repository.UserRepository;
import com.echoreviews.repository.AlbumRepository;
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
        User userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        List<GrantedAuthority> authorities = new ArrayList<>();
        if (userEntity.isAdmin()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return new org.springframework.security.core.userdetails.User(userEntity.getUsername(), userEntity.getPassword(), authorities);
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
        Optional<User> userEntityOptional = userRepository.findByUsername(username);
        if (userEntityOptional.isPresent()) {
            User userEntity = userEntityOptional.get();
            UserDTO dto = UserDTO.fromUser(userEntity); // Usando el método estático
            return Optional.of(dto);
        } else {
            return Optional.empty();
        }
    }

    public Optional<UserDTO> getUserById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDTO);
    }

    @Transactional
    public UserDTO saveUser(UserDTO userDTO) {
        User user = userMapper.toEntity(userDTO);
        
        if (userDTO.id() != null) {
            // Si es una actualización, obtener el usuario existente
            User existingUser = userRepository.findById(userDTO.id())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Preservar las listas de seguidores y seguidos si no se proporcionan nuevas
            if (userDTO.followers() == null || userDTO.followers().isEmpty()) {
                user.setFollowers(existingUser.getFollowers());
            }
            if (userDTO.following() == null || userDTO.following().isEmpty()) {
                user.setFollowing(existingUser.getFollowing());
            }
        }
        
        if (userDTO.password() != null && !userDTO.password().isBlank()) {
            if (!userDTO.password().startsWith("$2a$") && !userDTO.password().startsWith("$2b$") && !userDTO.password().startsWith("$2y$")) {
                user.setPassword(passwordEncoder.encode(userDTO.password()));
            } else {
                user.setPassword(userDTO.password());
            }
        } else if (user.getId() != null) {
            User existingUserFromDb = userRepository.findById(user.getId()).orElseThrow(() -> new RuntimeException("User not found for password retention"));
            user.setPassword(existingUserFromDb.getPassword());
        } else {
            throw new IllegalArgumentException("Password cannot be blank for a new user.");
        }

        if (userDTO.id() == null) { 
            if (userRepository.existsByUsername(userDTO.username())) {
                throw new RuntimeException("Username '" + userDTO.username() + "' already exists");
            }
            if (userDTO.email() != null && userRepository.existsByEmail(userDTO.email())) {
                throw new RuntimeException("Email '" + userDTO.email() + "' already exists");
            }
        } else { 
            User existingUser = userRepository.findById(userDTO.id()).orElseThrow(() -> new RuntimeException("User not found with ID: " + userDTO.id()));
            if (!existingUser.getUsername().equals(userDTO.username()) && userRepository.existsByUsername(userDTO.username())) {
                throw new RuntimeException("Username '" + userDTO.username() + "' already exists for another user");
            }
            if (userDTO.email() != null && !existingUser.getEmail().equals(userDTO.email()) && userRepository.existsByEmail(userDTO.email())) {
                 throw new RuntimeException("Email '" + userDTO.email() + "' already exists for another user");
            }
            user.setAdmin(existingUser.isAdmin()); // Asegura que isAdmin se preserve del estado de la BD para actualizaciones
        }
        
        User savedUser = userRepository.save(user);
        UserDTO resultDTO = userMapper.toDTO(savedUser);
        return resultDTO;
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
        UserDTO dtoToSave = userDTO.withIsAdmin(false);
        return saveUser(dtoToSave); 
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

        User userToUpdate = userMapper.toEntity(updatedUserDTO); 
        userToUpdate.setAdmin(existingUser.isAdmin()); // Forzar la preservación del isAdmin de la BD

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
        UserDTO resultDTO = userMapper.toDTO(savedUser);
        return resultDTO;
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

    @Transactional
    public UserDTO createOrUpdateAdmin(UserDTO adminDTO) {
        if (adminDTO == null) {
            throw new IllegalArgumentException("Admin DTO cannot be null");
        }
        if (adminDTO.username() == null || adminDTO.username().trim().isEmpty()) {
            throw new IllegalArgumentException("Admin username cannot be empty");
        }
        if (adminDTO.password() == null || adminDTO.password().trim().isEmpty()) {
            throw new IllegalArgumentException("Admin password cannot be empty");
        }

        Optional<User> existingUserOptional = userRepository.findByUsername(adminDTO.username());
        User userEntity;

        if (existingUserOptional.isPresent()) {
            // Update existing admin
            userEntity = existingUserOptional.get();

            // Update fields from DTO
            userEntity.setEmail(adminDTO.email()); // Assuming email can be updated
            // Update other fields as necessary from adminDTO, e.g., profile picture if applicable

            // Password handling: only update if a new, non-blank password is provided
            // and it's not already the same hashed password.
            if (adminDTO.password() != null && !adminDTO.password().isBlank()) {
                if (!passwordEncoder.matches(adminDTO.password(), userEntity.getPassword()) &&
                    !(adminDTO.password().startsWith("$2a$") || adminDTO.password().startsWith("$2b$") || adminDTO.password().startsWith("$2y$"))) {
                    userEntity.setPassword(passwordEncoder.encode(adminDTO.password()));
                } else if (adminDTO.password().startsWith("$2a$") || adminDTO.password().startsWith("$2b$") || adminDTO.password().startsWith("$2y$")) {
                    // If it's already a hash, set it (e.g. if DTO provides it hashed)
                    userEntity.setPassword(adminDTO.password());
                }
                // If password in DTO is plain text but matches the existing one (after hashing), no change needed to password.
                // If password in DTO is blank, existing password is kept (implicitly handled by not setting).
            }
             // Ensure email uniqueness if it's being changed
            if (adminDTO.email() != null && !userEntity.getEmail().equals(adminDTO.email()) && userRepository.existsByEmail(adminDTO.email())) {
                throw new RuntimeException("Email '" + adminDTO.email() + "' already exists for another user");
            }

        } else {
            // Create new admin
            userEntity = userMapper.toEntity(adminDTO); // Initial mapping
            userEntity.setId(null); // Ensure it's treated as a new entity by JPA

            // Encode password for new user
            userEntity.setPassword(passwordEncoder.encode(adminDTO.password()));

            // Check for username and email conflicts for new user
            if (userRepository.existsByUsername(adminDTO.username())) {
                throw new RuntimeException("Username '" + adminDTO.username() + "' already exists");
            }
            if (adminDTO.email() != null && userRepository.existsByEmail(adminDTO.email())) {
                throw new RuntimeException("Email '" + adminDTO.email() + "' already exists");
            }
        }

        // Crucially, set isAdmin from the DTO
        userEntity.setAdmin(adminDTO.isAdmin());

        // Save and convert back to DTO
        User savedUser = userRepository.save(userEntity);
        return UserDTO.fromUser(savedUser); // Using static method as per previous preference
    }
}