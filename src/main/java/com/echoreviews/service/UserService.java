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
import com.echoreviews.util.InputSanitizer;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final ReviewService reviewService;
    private final AlbumService albumService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final InputSanitizer inputSanitizer;


    private static final Pattern SAFE_USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._@-]{3,50}$");
    private static final Pattern SAFE_EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$");
    

    private boolean isValidInput(String input, Pattern pattern) {
        return input != null && pattern.matcher(input).matches();
    }

    @Autowired
    public UserService(UserRepository userRepository, @Lazy ReviewService reviewService, @Lazy AlbumService albumService, UserMapper userMapper, @Lazy PasswordEncoder passwordEncoder, InputSanitizer inputSanitizer) {
        this.userRepository = userRepository;
        this.reviewService = reviewService;
        this.albumService = albumService;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.inputSanitizer = inputSanitizer;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        if (!inputSanitizer.isValidUsername(username)) {
            throw new UsernameNotFoundException("Invalid username format");
        }
        
        User userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));


        if (userEntity.isBanned()) {
            throw new UsernameNotFoundException("This account has been banned. Please contact customer support for more information.");
        }

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

        if (!inputSanitizer.isValidUsername(username)) {
            return Optional.empty();
        }
        
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
        System.out.println("Saving user - ID: " + userDTO.id());
        System.out.println("Following list before save: " + userDTO.following());
        System.out.println("Followers list before save: " + userDTO.followers());
        
        User user = userMapper.toEntity(userDTO);
        
        if (userDTO.id() != null) {
            User existingUser = userRepository.findById(userDTO.id())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            System.out.println("Existing user following: " + existingUser.getFollowing());
            System.out.println("Existing user followers: " + existingUser.getFollowers());
            

            user.setFollowing(userDTO.following() != null ? userDTO.following() : existingUser.getFollowing());
            user.setFollowers(userDTO.followers() != null ? userDTO.followers() : existingUser.getFollowers());
            

            if (userDTO.password() == null || userDTO.password().isBlank()) {
                user.setPassword(existingUser.getPassword());
            } else if (!userDTO.password().startsWith("$2a$") && !userDTO.password().startsWith("$2b$") && !userDTO.password().startsWith("$2y$")) {
                user.setPassword(passwordEncoder.encode(userDTO.password()));
            } else {
                user.setPassword(userDTO.password());
            }
            

            user.setAdmin(existingUser.isAdmin());
        } else {

            if (userRepository.existsByUsername(userDTO.username())) {
                throw new RuntimeException("Username '" + userDTO.username() + "' already exists");
            }
            if (userDTO.email() != null && userRepository.existsByEmail(userDTO.email())) {
                throw new RuntimeException("Email '" + userDTO.email() + "' already exists");
            }
            if (userDTO.password() == null || userDTO.password().isBlank()) {
                throw new IllegalArgumentException("Password cannot be blank for a new user.");
            }

            user.setPassword(passwordEncoder.encode(userDTO.password()));
        }
        
        User savedUser = userRepository.save(user);
        System.out.println("User saved - Following: " + savedUser.getFollowing());
        System.out.println("User saved - Followers: " + savedUser.getFollowers());
        
        return UserDTO.fromUser(savedUser);
    }

    public Optional<UserDTO> authenticateUser(String username, String password) {

        if (!inputSanitizer.isValidUsername(username)) {
            return Optional.empty();
        }
        
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
        

        if (!inputSanitizer.isValidUsername(userDTO.username())) {
            throw new RuntimeException("Invalid username format. Only alphanumeric characters, dots, underscores, @ and hyphens are allowed.");
        }
        
        if (!inputSanitizer.isValidEmail(userDTO.email())) {
            throw new RuntimeException("Invalid email format");
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
                if (session != null) {
                    session.setAttribute("user", savedUserDTO);
                }
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
                if (session != null) {
                    session.setAttribute("user", savedUserDTO);
                }
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
        // The states of admin, potentiallyDangerous, and banned are taken from the DTO

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
        System.out.println("Attempting to unfollow - Follower ID: " + followerId + ", Target ID: " + targetUserId);
        
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new RuntimeException("Follower user not found"));
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        System.out.println("Current following list: " + follower.getFollowing());
        System.out.println("Current followers list: " + target.getFollowers());


        if (follower.getFollowing().contains(targetUserId)) {
            follower.getFollowing().remove(targetUserId);
            target.getFollowers().remove(followerId);

            System.out.println("Updated following list: " + follower.getFollowing());
            System.out.println("Updated followers list: " + target.getFollowers());


            follower = userRepository.save(follower);
            target = userRepository.save(target);

            UserDTO updatedFollowerDTO = UserDTO.fromUser(follower);
            session.setAttribute("user", updatedFollowerDTO);
            return updatedFollowerDTO;
        }
        return UserDTO.fromUser(follower);
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

    @Transactional
    public PdfUploadResult uploadUserPdf(UserDTO userDTO, MultipartFile pdfFile) {
        if (userDTO == null) {
            System.err.println("Error: User cannot be null");
            return PdfUploadResult.error("User cannot be null");
        }
        if (pdfFile == null || pdfFile.isEmpty()) {
            System.err.println("Error: PDF file cannot be null or empty");
            return PdfUploadResult.error("PDF file cannot be null or empty");
        }
        

        if (!pdfFile.getContentType().equals("application/pdf")) {
            System.err.println("Error: File must be a PDF (invalid content type: " + pdfFile.getContentType() + ")");
            return PdfUploadResult.error("File must be a PDF");
        }
        

        try {
            byte[] fileBytes = pdfFile.getBytes();
            

            if (fileBytes.length < 5) {
                System.err.println("Error: File is too small to be a valid PDF (" + fileBytes.length + " bytes)");
                return PdfUploadResult.error("File is too small to be a valid PDF");
            }
            

            final long MAX_PDF_SIZE = 10 * 1024 * 1024; // 10 MB
            if (fileBytes.length > MAX_PDF_SIZE) {
                System.err.println("Error: PDF file is too large (max 10 MB)");
                return PdfUploadResult.error("PDF file is too large (max 10 MB)");
            }
            

            String magicNumber = new String(fileBytes, 0, 5);
            if (!magicNumber.startsWith("%PDF-")) {
                System.err.println("Error: File does not have valid PDF signature (starts with '" + magicNumber + "')");
                return PdfUploadResult.error("File does not have valid PDF signature");
            }
            

            String fileContent = new String(fileBytes);
            if (!fileContent.contains("%%EOF")) {
                System.err.println("Error: File does not have valid PDF structure (missing EOF marker)");
                return PdfUploadResult.error("File does not have valid PDF structure (missing EOF marker)");
            }
            

            if (fileContent.toLowerCase().contains("/js") || 
                fileContent.toLowerCase().contains("/javascript") ||
                fileContent.toLowerCase().contains("/action") ||
                fileContent.toLowerCase().contains("/launch")) {
                System.err.println("Error: PDF contains potentially unsafe elements");
                return PdfUploadResult.error("PDF contains potentially unsafe elements");
            }
        } catch (IOException e) {
            System.err.println("Error: Failed to read file content: " + e.getMessage());
            return PdfUploadResult.error("Failed to read file content: " + e.getMessage());
        }
        

        if (userDTO.pdfPath() != null) {
            try {
                deleteUserPdf(userDTO);
            } catch (IOException e) {
                System.err.println("Error: Failed to delete existing PDF: " + e.getMessage());
                return PdfUploadResult.error("Failed to delete existing PDF: " + e.getMessage());
            }
        }
        

        String pdfBaseDir = System.getProperty("app.pdf.storage.directory", "./user-pdfs");
        
        try {

            Path pdfBasePath = Paths.get(pdfBaseDir);
            if (!Files.exists(pdfBasePath)) {
                Files.createDirectories(pdfBasePath);
                System.out.println("Directorio base para PDFs creado: " + pdfBasePath.toAbsolutePath());
            }
            

            String userFolderName = "user_" + userDTO.id();
            Path userPdfDir = pdfBasePath.resolve(userFolderName);
            if (!Files.exists(userPdfDir)) {
                Files.createDirectories(userPdfDir);
                System.out.println("Directorio de usuario para PDFs creado: " + userPdfDir.toAbsolutePath());
            }
            

            String originalFilename = pdfFile.getOriginalFilename();
            String fileName = originalFilename != null ? 
                    originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_") : 
                    "document.pdf";
            Path filePath = userPdfDir.resolve(fileName);
            

            Files.copy(pdfFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            

            String relativePath = "user-pdfs/" + userFolderName + "/" + fileName;
            UserDTO updatedUser = userDTO.withPdfPath(relativePath);
            
            System.out.println("PDF guardado en: " + filePath.toAbsolutePath());
            System.out.println("Ruta relativa guardada: " + relativePath);
            
            UserDTO savedUser = saveUser(updatedUser);
            return PdfUploadResult.success(savedUser);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error: Failed to save PDF: " + e.getMessage());
            return PdfUploadResult.error("Error al guardar PDF: " + e.getMessage());
        }
    }

    @Transactional
    public UserDTO deleteUserPdf(UserDTO userDTO) throws IOException {
        if (userDTO == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        

        if (userDTO.pdfPath() == null || userDTO.pdfPath().isEmpty()) {
            return userDTO;
        }
        
        try {

            String relativePath = userDTO.pdfPath();
            Path filePath = Paths.get(relativePath);
            

            if (!Files.exists(filePath)) {
                filePath = Paths.get(".", relativePath).normalize();
            }
            
            System.out.println("Intentando eliminar archivo: " + filePath.toString());
            

            if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
                Files.delete(filePath);
                System.out.println("Archivo PDF eliminado: " + filePath.toString());
                

                Path userDir = filePath.getParent();
                if (Files.exists(userDir) && Files.isDirectory(userDir)) {
                    try (var entries = Files.list(userDir)) {
                        if (entries.findFirst().isEmpty()) {
                            Files.delete(userDir);
                            System.out.println("Carpeta de usuario eliminada: " + userDir.toString());
                        }
                    }
                }
            } else {
                System.out.println("El archivo no existe: " + filePath.toString());
            }
            

            UserDTO updatedUser = userDTO.withPdfPath(null);
            return saveUser(updatedUser);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IOException("Error al eliminar archivo PDF: " + e.getMessage(), e);
        }
    }
    

    private void cleanupIncorrectPdfStructure() {
        try {
            Path staticDir = Paths.get("src/main/resources/static");
            if (!Files.exists(staticDir)) {
                return;
            }
            

            try (var paths = Files.list(staticDir)) {
                paths.filter(path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.startsWith("pdfs.user_") && Files.isDirectory(path);
                }).forEach(incorrectPath -> {
                    try {
                        System.out.println("Encontrada estructura incorrecta: " + incorrectPath);
                        

                        Path pdfBaseDir = Paths.get("src/main/resources/static/pdfs");
                        if (!Files.exists(pdfBaseDir)) {
                            Files.createDirectories(pdfBaseDir);
                        }
                        

                        String incorrectFolderName = incorrectPath.getFileName().toString();
                        String userId = incorrectFolderName.substring("pdfs.user_".length());
                        String correctFolderName = "user_" + userId;
                        
                        Path correctUserDir = pdfBaseDir.resolve(correctFolderName);
                        if (!Files.exists(correctUserDir)) {
                            Files.createDirectories(correctUserDir);
                        }
                        

                        try (var files = Files.list(incorrectPath)) {
                            files.forEach(pdfFile -> {
                                try {
                                    Path targetFile = correctUserDir.resolve(pdfFile.getFileName());
                                    Files.move(pdfFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                                    System.out.println("Movido archivo " + pdfFile + " a " + targetFile);
                                } catch (IOException e) {
                                    System.err.println("Error al mover archivo: " + e.getMessage());
                                }
                            });
                        }
                        

                        Files.delete(incorrectPath);
                        System.out.println("Eliminada carpeta incorrecta: " + incorrectPath);
                        
                    } catch (IOException e) {
                        System.err.println("Error al limpiar estructura incorrecta: " + e.getMessage());
                    }
                });
            }
            
        } catch (IOException e) {
            System.err.println("Error al limpiar estructura incorrecta: " + e.getMessage());
        }
    }

    @Transactional
    public String getUserPdfPath(String username) {
        Optional<UserDTO> userOpt = getUserByUsername(username);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + username);
        }
        
        UserDTO user = userOpt.get();
        return user.pdfPath();
    }

    public static class PdfUploadResult {
        private final boolean success;
        private final String errorMessage;
        private final UserDTO user;

        private PdfUploadResult(boolean success, String errorMessage, UserDTO user) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.user = user;
        }

        public static PdfUploadResult success(UserDTO user) {
            return new PdfUploadResult(true, null, user);
        }

        public static PdfUploadResult error(String errorMessage) {
            return new PdfUploadResult(false, errorMessage, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public UserDTO getUser() {
            return user;
        }
    }

    public boolean verifyPassword(String username, String password) {
        return authenticateUser(username, password).isPresent();
    }

    @Transactional
    public UserDTO updatePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate password format
        if (!newPassword.matches("^(?=.*[0-9])(?=.*[A-Z])(?=.*[!@#$%^&*(),.?\":{}|<>])(?=\\S+$).{8,25}$")) {
            throw new IllegalArgumentException(
                "Password must be between 8 and 25 characters and contain at least one number, " +
                "one uppercase letter, and one special character"
            );
        }

        // Encode and set the new password
        user.setPassword(passwordEncoder.encode(newPassword));
        
        // Save the user
        User savedUser = userRepository.save(user);
        return UserDTO.fromUser(savedUser);
    }
}