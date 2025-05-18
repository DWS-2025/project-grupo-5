package com.echoreviews.controller;

import com.echoreviews.model.User;
import com.echoreviews.service.UserService;
import com.echoreviews.service.ArtistService;
import com.echoreviews.service.AlbumService;
import com.echoreviews.service.ReviewService;
import com.echoreviews.model.Artist;
import com.echoreviews.model.Album;
import com.echoreviews.model.Review;
import com.echoreviews.dto.UserDTO;
import com.echoreviews.dto.ArtistDTO;
import com.echoreviews.dto.AlbumDTO;
import com.echoreviews.dto.ReviewDTO;
import com.echoreviews.mapper.UserMapper;
import com.echoreviews.mapper.AlbumMapper;
import com.echoreviews.mapper.ReviewMapper;
import com.echoreviews.mapper.ArtistMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private AlbumService albumService;

    @Autowired
    private ArtistService artistService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ReviewService reviewService;

    @GetMapping
    public String listAlbums(Model model, HttpSession session) {
        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "You don't have access to this resource.");
            return "error";
        } else {
            model.addAttribute("albums", albumService.getAllAlbums());
            return "album/admin";
        }
    }

    @GetMapping("/new")
    public String showCreateForm(Model model, HttpSession session) {

        UserDTO user = (UserDTO) session.getAttribute("user");
        model.addAttribute("artists", artistService.getAllArtists());

        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "You don't have access to this resource.");
            return "error";
        } else{
            model.addAttribute("album", new Album());
            return "album/form";
        }
    }

    @PostMapping
    public String createAlbum(@Valid Album album, BindingResult result,
                              @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                              @RequestParam(value = "audioFile2", required = false) MultipartFile audioPreview,
                              @RequestParam(value = "artistId[]", required = false) List<Long> artistIds,
                              @RequestParam(value = "newArtistName", required = false) String newArtistName,
                              Model model, HttpSession session) throws IOException {

        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "You don't have access to this resource.");
            return "error";
        }

        if (result.hasErrors()) {
            StringBuilder errorMsg = new StringBuilder("Validation errors: ");
            result.getAllErrors().forEach(error -> errorMsg.append(error.getDefaultMessage()).append(". "));
            model.addAttribute("error", errorMsg.toString());
            return "error";
        }

        try {
            // Make sure the artists list is initialized
            if (album.getArtists() == null) {
                album.setArtists(new ArrayList<>());
            } else {
                album.getArtists().clear(); // Clear existing artists to avoid duplicates
            }

            // Process existing artists if they exist
            if (artistIds != null && !artistIds.isEmpty()) {
                for (Long artistId : artistIds) {
                    if (artistId != null && artistId > 0) {
                        var artistOpt = artistService.getArtistById(artistId);
                        if (artistOpt.isEmpty()) {
                            model.addAttribute("error", "Error: One of the selected artists does not exist in the database");
                            return "error";
                        }
                        
                        artistOpt.ifPresent(artistDTO -> {
                            Artist artist = new Artist();
                            artist.setId(artistDTO.id());
                            artist.setName(artistDTO.name());
                            album.getArtists().add(artist);
                        });
                    }
                }
            }

            // Process new artist if it exists
            if (newArtistName != null && !newArtistName.trim().isEmpty()) {
                try {
                    String validatedName = newArtistName.trim();
                    Artist newArtist = new Artist(validatedName);
                    
                    if (newArtist.getName() == null) {
                        newArtist.setName(validatedName);
                    }
                    
                    ArtistDTO artistDTO = ArtistDTO.fromArtist(newArtist);
                    ArtistDTO savedArtistDTO = artistService.saveArtist(artistDTO);
                    newArtist.setId(savedArtistDTO.id());
                    album.getArtists().add(newArtist);
                } catch (Exception e) {
                    model.addAttribute("error", "Error creating new artist: " + e.getMessage());
                    return "error";
                }
            }

            // Verify that at least one artist has been added
            if (album.getArtists().isEmpty()) {
                model.addAttribute("error", "Error: You must select at least one artist or create a new one");
                return "error";
            }

            // Process tracklist if it exists
            if (album.getTracklist() != null && !album.getTracklist().isEmpty()) {
                try {
                    String[] tracklistArray = album.getTracklist().split("\\r?\\n");
                    String concatenatedTracklist = String.join(" + ", tracklistArray);
                    album.setTracklist(concatenatedTracklist);
                } catch (Exception e) {
                    model.addAttribute("error", "Error processing the song list: " + e.getMessage());
                    return "error";
                }
            }

            // Convert and save the album
            AlbumDTO albumDTO;
            AlbumDTO savedAlbum;
            try {
                albumDTO = AlbumDTO.fromAlbum(album);
                savedAlbum = albumService.saveAlbum(albumDTO);
            } catch (Exception e) {
                model.addAttribute("error", "Error saving the basic album information: " + e.getMessage());
                return "error";
            }

            // Process image if it exists
            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    savedAlbum = albumService.saveAlbumWithImage(savedAlbum, imageFile);
                } catch (Exception e) {
                    model.addAttribute("error", "Error saving the album image: " + e.getMessage());
                    return "error";
                }
            }

            // Process audio if it exists
            if (audioPreview != null && !audioPreview.isEmpty()) {
                try {
                    savedAlbum = albumService.saveAlbumWithAudio(savedAlbum, audioPreview);
                } catch (Exception e) {
                    model.addAttribute("error", "Error saving the audio file: " + e.getMessage());
                    return "error";
                }
            }

            return "redirect:/admin";
        } catch (Exception e) {
            model.addAttribute("error", "Unexpected error while saving the album: " + e.getMessage());
            return "error";
        }
    }
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, HttpSession session) {
        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "You don't have access to this resource.");
            return "error";
        }
        
        var albumOpt = albumService.getAlbumById(id);
        if (albumOpt.isEmpty()) {
            model.addAttribute("error", "Error: Album not found with ID: " + id);
            return "error";
        }
        
        try {
            Album albumEntity = albumOpt.get().toAlbum();
            // Make sure the artists list is initialized
            if (albumEntity.getArtists() == null) {
                albumEntity.setArtists(new ArrayList<>());
            }
            
            // Convert the album to DTO while maintaining its current artists
            AlbumDTO albumDTO = AlbumDTO.fromAlbum(albumEntity);
            model.addAttribute("album", albumDTO);
            model.addAttribute("artists", artistService.getAllArtists());
            
            // Add the list of current artist IDs
            List<Long> currentArtistIds = albumEntity.getArtists().stream()
                .map(Artist::getId)
                .collect(Collectors.toList());
            model.addAttribute("currentArtistIds", currentArtistIds);
            
            return "album/form";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading the edit form: " + e.getMessage());
            return "error";
        }
    }


    @PostMapping("/{id}")
    public String updateAlbum(
            @PathVariable Long id,
            @Valid Album album,
            BindingResult result,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "audioFile2", required = false) MultipartFile audioPreview,
            @RequestParam(value = "artistId[]", required = false) List<Long> artistIds,
            @RequestParam(value = "newArtistName", required = false) String newArtistName,
            Model model, HttpSession session) throws IOException {

        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "You don't have access to this resource.");
            return "error";
        }

        if (result.hasErrors()) {
            StringBuilder errorMsg = new StringBuilder("Validation errors: ");
            result.getAllErrors().forEach(error -> errorMsg.append(error.getDefaultMessage()).append(". "));
            model.addAttribute("error", errorMsg.toString());
            return "error";
        }

        try {
            var albumOpt = albumService.getAlbumById(id);
            if (albumOpt.isEmpty()) {
                model.addAttribute("error", "Error: The album you are trying to update does not exist (ID: " + id + ")");
                return "error";
            }
            
            Album existingAlbum = albumOpt.get().toAlbum();
            existingAlbum.setTitle(album.getTitle());
            existingAlbum.getArtists().clear();
            
            // Process existing artists if they exist
            if (artistIds != null && !artistIds.isEmpty()) {
                for (Long artistId : artistIds) {
                    if (artistId != null && artistId > 0) {
                        var artistOpt = artistService.getArtistById(artistId);
                        if (artistOpt.isEmpty()) {
                            model.addAttribute("error", "Error: One of the selected artists does not exist in the database");
                            return "error";
                        }
                        
                        artistOpt.ifPresent(artistDTO -> {
                            Artist artist = new Artist();
                            artist.setId(artistDTO.id());
                            artist.setName(artistDTO.name());
                            existingAlbum.getArtists().add(artist);
                        });
                    }
                }
            }

            // Process new artist if it exists
            if (newArtistName != null && !newArtistName.trim().isEmpty()) {
                try {
                    String validatedName = newArtistName.trim();
                    Artist newArtist = new Artist(validatedName);
                    
                    if (newArtist.getName() == null) {
                        newArtist.setName(validatedName);
                    }
                    
                    ArtistDTO artistDTO = ArtistDTO.fromArtist(newArtist);
                    ArtistDTO savedArtistDTO = artistService.saveArtist(artistDTO);
                    newArtist.setId(savedArtistDTO.id());
                    existingAlbum.getArtists().add(newArtist);
                } catch (Exception e) {
                    model.addAttribute("error", "Error creating new artist: " + e.getMessage());
                    return "error";
                }
            }
            
            // Verify that at least one artist has been added
            if (existingAlbum.getArtists().isEmpty()) {
                model.addAttribute("error", "Error: You must select at least one artist or create a new one");
                return "error";
            }

            // Update the remaining album fields
            existingAlbum.setGenre(album.getGenre());
            existingAlbum.setDescription(album.getDescription());
            existingAlbum.setTracklist(album.getTracklist());
            existingAlbum.setYear(album.getYear());
            existingAlbum.setSpotify_url(album.getSpotify_url());
            existingAlbum.setApplemusic_url(album.getApplemusic_url());
            existingAlbum.setTidal_url(album.getTidal_url());

            // Process tracklist if it exists
            if (existingAlbum.getTracklist() != null && !existingAlbum.getTracklist().isEmpty()) {
                try {
                    String[] tracklistArray = existingAlbum.getTracklist().split("\\r?\\n");
                    String concatenatedTracklist = String.join(" + ", tracklistArray);
                    existingAlbum.setTracklist(concatenatedTracklist);
                } catch (Exception e) {
                    model.addAttribute("error", "Error processing the song list: " + e.getMessage());
                    return "error";
                }
            }

            // Convert and save the album
            AlbumDTO albumDTO;
            AlbumDTO savedAlbum;
            try {
                albumDTO = AlbumDTO.fromAlbum(existingAlbum);
                savedAlbum = albumService.saveAlbum(albumDTO);
            } catch (Exception e) {
                model.addAttribute("error", "Error saving the basic album information: " + e.getMessage());
                return "error";
            }

            // Process image if it exists
            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    savedAlbum = albumService.saveAlbumWithImage(savedAlbum, imageFile);
                } catch (Exception e) {
                    model.addAttribute("error", "Error saving the album image: " + e.getMessage());
                    return "error";
                }
            }

            // Process audio if it exists
            if (audioPreview != null && !audioPreview.isEmpty()) {
                try {
                    savedAlbum = albumService.saveAlbumWithAudio(savedAlbum, audioPreview);
                } catch (Exception e) {
                    model.addAttribute("error", "Error saving the audio file: " + e.getMessage());
                    return "error";
                }
            }

            return "redirect:/admin";
        } catch (Exception e) {
            model.addAttribute("error", "Unexpected error while updating the album: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteAlbum(@PathVariable Long id,  Model model, HttpSession session) {

        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "You don't have access to this resource.");
            return "error";
        } else {

            albumService.deleteAlbum(id);
            return "redirect:/admin";
        }
    }

    @GetMapping("/artists")
    public String listArtists(Model model, HttpSession session) {
        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "You don't have access to this resource.");
            return "error";
        } else {
            model.addAttribute("artists", artistService.getAllArtists());
            return "artist/admin";
        }
    }
    
    @GetMapping("/users")
    public String listUsers(Model model, HttpSession session) {
        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "You don't have access to this resource.");
            return "error";
        } else {
            model.addAttribute("users", userService.getAllUsers());
            return "user/admin";
        }
    }
    
    @GetMapping("/users/{id}/edit")
    public String showEditUserForm(@PathVariable Long id, Model model, HttpSession session) {
        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "You don't have access to this resource.");
            return "error";
        }
        
        Optional<UserDTO> userOpt = userService.getUserById(id);
        if (userOpt.isEmpty()) {
            model.addAttribute("error", "Error: User not found with ID: " + id);
            return "error";
        }
        
        model.addAttribute("userEdit", userOpt.get());
        return "user/form";
    }
    
    @PostMapping("/users/{id}")
    public String updateUser(@PathVariable Long id, @Valid UserDTO userDTO, BindingResult result, Model model, HttpSession session) {
        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "You don't have access to this resource.");
            return "error";
        }
        
        if (result.hasErrors()) {
            model.addAttribute("error", "Data validation error");
            return "user/form";
        }

        try {
            // Make sure that the path ID matches the DTO ID
            UserDTO updatedUserDTO = userDTO.withId(id);
            userService.updateUser(updatedUserDTO);
            return "redirect:/admin/users";
        } catch (RuntimeException e) {
            model.addAttribute("error", "Error updating user: " + e.getMessage());
            model.addAttribute("userEdit", userDTO);
            return "user/form";
        }
    }
    
    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, Model model, HttpSession session) {
        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "You don't have access to this resource.");
            return "error";
        }

        Optional<UserDTO> userToDelete = userService.getUserById(id);
        if (userToDelete.isPresent()) {
            userService.deleteUser(userToDelete.get().username());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/reviews")
    public String listReviews(Model model, HttpSession session) {
        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "You don't have access to this resource.");
            return "error";
        } else {
            List<ReviewDTO> reviews = reviewService.getAllReviewsWithMarkdown();
            model.addAttribute("reviews", reviews);
            return "review/admin";
        }
    }
    
    @GetMapping("/reviews/{id}/edit")
    public String showEditReviewForm(@PathVariable Long id, Model model, HttpSession session) {
        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "You don't have access to this resource.");
            return "error";
        }
        
        Optional<ReviewDTO> reviewOpt = reviewService.getReviewByIdWithMarkdown(id);
        if (reviewOpt.isEmpty()) {
            model.addAttribute("error", "Error: Review not found with ID: " + id);
            return "error";
        }
        
        model.addAttribute("review", reviewOpt.get());
        return "review/form";
    }
    
    @PostMapping("/reviews/{id}")
    public String updateReview(@PathVariable Long id, @Valid ReviewDTO reviewDTO, BindingResult result, Model model, HttpSession session) {
        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "You don't have access to this resource.");
            return "error";
        }

        if (reviewDTO.content().length() > 255) {
            model.addAttribute("error", "Se ha superado el límite de caracteres");
            return "error";
        }
        
        if (result.hasErrors()) {
            model.addAttribute("error", "Data validation error");
            return "review/form";
        }
        
        reviewService.updateReview(reviewDTO);
        
        return "redirect:/admin/reviews";
    }
    
    @PostMapping("/reviews/{id}/delete")
    public String deleteReview(@PathVariable Long id, Model model, HttpSession session) {
        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "You don't have access to this resource.");
            return "error";
        }
        
        Optional<ReviewDTO> reviewOpt = reviewService.getReviewById(id);
        if (reviewOpt.isPresent()) {
            ReviewDTO review = reviewOpt.get();
            Long albumId = review.albumId();
            
            reviewService.deleteReview(id);
            
            // Update album's average rating
            Optional<AlbumDTO> albumOpt = albumService.getAlbumById(albumId);
            if (albumOpt.isPresent()) {
                AlbumDTO albumDTO = albumOpt.get();
                albumDTO.updateAverageRating(reviewService.getReviewsByAlbumId(albumId));
                albumService.saveAlbum(albumDTO);
            }
        }
        
        return "redirect:/admin/reviews";
    }
}