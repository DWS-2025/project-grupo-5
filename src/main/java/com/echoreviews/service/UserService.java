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

    // Patrón para validar entradas y prevenir inyecciones SQL
    private static final Pattern SAFE_USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._@-]{3,50}$");
    private static final Pattern SAFE_EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$");
    
    /**
     * Método para validar que una entrada de texto sea segura
     * @param input El texto a validar
     * @param pattern El patrón contra el que validar
     * @return true si la entrada es válida, false en caso contrario
     */
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
        // Validar entrada para prevenir inyecciones SQL
        if (!inputSanitizer.isValidUsername(username)) {
            throw new UsernameNotFoundException("Invalid username format");
        }
        
        User userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // Check if the user is banned and prevent authentication if so
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
        // Validar entrada para prevenir inyecciones SQL
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
            
            // Mantener las listas actualizadas
            user.setFollowing(userDTO.following() != null ? userDTO.following() : existingUser.getFollowing());
            user.setFollowers(userDTO.followers() != null ? userDTO.followers() : existingUser.getFollowers());
            
            // Mantener la contraseña si no se proporciona una nueva
            if (userDTO.password() == null || userDTO.password().isBlank()) {
                user.setPassword(existingUser.getPassword());
            } else if (!userDTO.password().startsWith("$2a$") && !userDTO.password().startsWith("$2b$") && !userDTO.password().startsWith("$2y$")) {
                user.setPassword(passwordEncoder.encode(userDTO.password()));
            } else {
                user.setPassword(userDTO.password());
            }
            
            // Mantener el estado de admin
            user.setAdmin(existingUser.isAdmin());
        } else {
            // Validaciones para nuevo usuario
            if (userRepository.existsByUsername(userDTO.username())) {
                throw new RuntimeException("Username '" + userDTO.username() + "' already exists");
            }
            if (userDTO.email() != null && userRepository.existsByEmail(userDTO.email())) {
                throw new RuntimeException("Email '" + userDTO.email() + "' already exists");
            }
            if (userDTO.password() == null || userDTO.password().isBlank()) {
                throw new IllegalArgumentException("Password cannot be blank for a new user.");
            }
            // Encriptar contraseña para nuevo usuario
            user.setPassword(passwordEncoder.encode(userDTO.password()));
        }
        
        User savedUser = userRepository.save(user);
        System.out.println("User saved - Following: " + savedUser.getFollowing());
        System.out.println("User saved - Followers: " + savedUser.getFollowers());
        
        return UserDTO.fromUser(savedUser);
    }

    public Optional<UserDTO> authenticateUser(String username, String password) {
        // Validar entrada para prevenir inyecciones SQL
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
        
        // Validar entradas para prevenir inyecciones SQL
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

        // Trabajar directamente con las entidades
        if (follower.getFollowing().contains(targetUserId)) {
            follower.getFollowing().remove(targetUserId);
            target.getFollowers().remove(followerId);

            System.out.println("Updated following list: " + follower.getFollowing());
            System.out.println("Updated followers list: " + target.getFollowers());

            // Guardar directamente las entidades
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
        
        // Vamos a hacer una validación para comprobar que el archivo subido es realmente un pdf. Es cierto que esta verificación de aquí
        // es facilmente bypaseable interceptando la petición con burbsuite
        if (!pdfFile.getContentType().equals("application/pdf")) {
            System.err.println("Error: File must be a PDF (invalid content type: " + pdfFile.getContentType() + ")");
            return PdfUploadResult.error("File must be a PDF");
        }
        
        // Vamos a validar que sea un PDF verificando los magic bytes del archivo
        try {
            byte[] fileBytes = pdfFile.getBytes();
            
            // 1. Si el archivo tiene menos de 5 bytes, lo tiramos
            if (fileBytes.length < 5) {
                System.err.println("Error: File is too small to be a valid PDF (" + fileBytes.length + " bytes)");
                return PdfUploadResult.error("File is too small to be a valid PDF");
            }
            
            // 2. Si el archivo tiene mas de 10 MB, lo tiramos
            final long MAX_PDF_SIZE = 10 * 1024 * 1024; // 10 MB
            if (fileBytes.length > MAX_PDF_SIZE) {
                System.err.println("Error: PDF file is too large (max 10 MB)");
                return PdfUploadResult.error("PDF file is too large (max 10 MB)");
            }
            
            // 3. Comprobamos los magic numbers check - debe comenzar con %PDF- si realmente es un pdf
            String magicNumber = new String(fileBytes, 0, 5);
            if (!magicNumber.startsWith("%PDF-")) {
                System.err.println("Error: File does not have valid PDF signature (starts with '" + magicNumber + "')");
                return PdfUploadResult.error("File does not have valid PDF signature");
            }
            
            // 4. Verificar marcador de fin de archivo (%%EOF)
            String fileContent = new String(fileBytes);
            if (!fileContent.contains("%%EOF")) {
                System.err.println("Error: File does not have valid PDF structure (missing EOF marker)");
                return PdfUploadResult.error("File does not have valid PDF structure (missing EOF marker)");
            }
            
            // 5. Verificar que el archivo no tenga código ejecutable sospechoso
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
        
        // Primero eliminamos cualquier PDF existente del usuario
        if (userDTO.pdfPath() != null) {
            try {
                deleteUserPdf(userDTO);
            } catch (IOException e) {
                System.err.println("Error: Failed to delete existing PDF: " + e.getMessage());
                return PdfUploadResult.error("Failed to delete existing PDF: " + e.getMessage());
            }
        }
        
        // Limpiar carpetas con estructura incorrecta
        cleanupIncorrectPdfStructure();
        
        try {
            // Crear directorio base para PDFs (asegurarnos que la carpeta 'pdfs' existe)
            Path pdfBaseDir = Paths.get("src/main/resources/static/pdfs");
            if (!Files.exists(pdfBaseDir)) {
                Files.createDirectories(pdfBaseDir);
            }
            
            // Crear el directorio para este usuario usando su ID
            String userFolderName = "user_" + userDTO.id();
            Path userPdfDir = pdfBaseDir.resolve(userFolderName);
            if (!Files.exists(userPdfDir)) {
                Files.createDirectories(userPdfDir);
            }
            
            // Guardar el PDF con su nombre original
            String originalFilename = pdfFile.getOriginalFilename();
            String fileName = originalFilename != null ? originalFilename : "document.pdf";
            Path filePath = userPdfDir.resolve(fileName);
            
            // Copiar el archivo
            Files.copy(pdfFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // La ruta relativa para acceder desde la web usando el ID del usuario
            String relativePath = "/pdfs/" + userFolderName + "/" + fileName;
            UserDTO updatedUser = userDTO.withPdfPath(relativePath);
            
            System.out.println("PDF guardado en: " + filePath.toAbsolutePath());
            System.out.println("Ruta relativa: " + relativePath);
            
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
        
        // Si el usuario no tiene PDF, no hacer nada
        if (userDTO.pdfPath() == null) {
            return userDTO;
        }
        
        try {
            // Eliminar el archivo si existe
            String relativePath = userDTO.pdfPath();
            Path pdfPath = Paths.get("src/main/resources/static" + relativePath);
            
            System.out.println("Intentando eliminar archivo: " + pdfPath.toAbsolutePath());
            
            if (Files.exists(pdfPath) && Files.isRegularFile(pdfPath)) {
                Files.delete(pdfPath);
                System.out.println("Archivo PDF eliminado: " + pdfPath.toAbsolutePath());
                
                // Intentar eliminar la carpeta del usuario si está vacía
                Path userDir = pdfPath.getParent();
                if (Files.exists(userDir) && Files.isDirectory(userDir)) {
                    try (var entries = Files.list(userDir)) {
                        if (entries.findFirst().isEmpty()) {
                            Files.delete(userDir);
                            System.out.println("Carpeta de usuario eliminada: " + userDir.toAbsolutePath());
                        }
                    }
                }
            } else {
                System.out.println("El archivo no existe: " + pdfPath.toAbsolutePath());
            }
            
            // Actualizar el usuario sin la ruta del PDF
            UserDTO updatedUser = userDTO.withPdfPath(null);
            return saveUser(updatedUser);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IOException("Error al eliminar archivo PDF: " + e.getMessage(), e);
        }
    }
    
    /**
     * Limpia cualquier estructura incorrecta de carpetas PDF
     */
    private void cleanupIncorrectPdfStructure() {
        try {
            Path staticDir = Paths.get("src/main/resources/static");
            if (!Files.exists(staticDir)) {
                return;
            }
            
            // Buscar carpetas con formato incorrecto "pdfs.user_X"
            try (var paths = Files.list(staticDir)) {
                paths.filter(path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.startsWith("pdfs.user_") && Files.isDirectory(path);
                }).forEach(incorrectPath -> {
                    try {
                        System.out.println("Encontrada estructura incorrecta: " + incorrectPath);
                        
                        // Crear carpeta correcta si no existe
                        Path pdfBaseDir = Paths.get("src/main/resources/static/pdfs");
                        if (!Files.exists(pdfBaseDir)) {
                            Files.createDirectories(pdfBaseDir);
                        }
                        
                        // Extraer el ID de usuario del nombre pdfs.user_X
                        String incorrectFolderName = incorrectPath.getFileName().toString();
                        String userId = incorrectFolderName.substring("pdfs.user_".length());
                        String correctFolderName = "user_" + userId;
                        
                        Path correctUserDir = pdfBaseDir.resolve(correctFolderName);
                        if (!Files.exists(correctUserDir)) {
                            Files.createDirectories(correctUserDir);
                        }
                        
                        // Mover cualquier PDF de la carpeta incorrecta a la correcta
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
                        
                        // Eliminar carpeta incorrecta
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
}