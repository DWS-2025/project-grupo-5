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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
            model.addAttribute("error", "No tienes acceso a este recurso.");
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
            model.addAttribute("error", "No tienes acceso a este recurso.");
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
                              @RequestParam(value = "artistId", required = false) Long artistId,
                              @RequestParam(value = "newArtistName", required = false) String newArtistName,
                              Model model, HttpSession session) throws IOException {

        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "No tienes acceso a este recurso.");
            return "error";
        }

        if (result.hasErrors()) {
            StringBuilder errorMsg = new StringBuilder("Errores de validación: ");
            result.getAllErrors().forEach(error -> errorMsg.append(error.getDefaultMessage()).append(". "));
            model.addAttribute("error", errorMsg.toString());
            return "error";
        }

        try {
            // Asegurarse de que la lista de artistas no sea null
            if (album.getArtists() == null) {
                album.setArtists(new ArrayList<>());
            } else {
                album.getArtists().clear(); // Limpiar artistas existentes para evitar duplicados
            }

            // Manejar la selección o creación de artista
            if (artistId != null && artistId > 0) {
                // Usar artista existente
                var artistOpt = artistService.getArtistById(artistId);
                if (artistOpt.isEmpty()) {
                    model.addAttribute("error", "Error: El artista seleccionado no existe en la base de datos");
                    return "error";
                }
                
                artistOpt.ifPresent(artistDTO -> {
                    Artist artist = new Artist();
                    artist.setId(artistDTO.id());
                    artist.setName(artistDTO.name());
                    album.getArtists().add(artist);
                });
            } else if (newArtistName != null && !newArtistName.trim().isEmpty()) {
                // Crear nuevo artista
                try {
                    // Crear el artista con nombre validado
                    String validatedName = newArtistName.trim();
                    Artist newArtist = new Artist(validatedName);
                    
                    // Verificar explícitamente que el nombre no sea null antes de crear el DTO
                    if (newArtist.getName() == null) {
                        newArtist.setName(validatedName); // Asegurar que el nombre esté establecido
                    }
                    
                    ArtistDTO artistDTO = ArtistDTO.fromArtist(newArtist);
                    ArtistDTO savedArtistDTO = artistService.saveArtist(artistDTO);
                    newArtist.setId(savedArtistDTO.id());
                    album.getArtists().add(newArtist);
                } catch (Exception e) {
                    model.addAttribute("error", "Error al crear el nuevo artista: " + e.getMessage());
                    return "error";
                }
            } else {
                // Si no se seleccionó un artista ni se creó uno nuevo, mostrar error
                model.addAttribute("error", "Error: Debe seleccionar un artista existente o crear uno nuevo");
                return "error";
            }

            // Verificar que se haya añadido al menos un artista
            if (album.getArtists().isEmpty()) {
                model.addAttribute("error", "Error: No se pudo asociar ningún artista al álbum");
                return "error";
            }

            // Procesar tracklist si existe
            if (album.getTracklist() != null && !album.getTracklist().isEmpty()) {
                try {
                    String[] tracklistArray = album.getTracklist().split("\\r?\\n");
                    String concatenatedTracklist = String.join(" + ", tracklistArray);
                    album.setTracklist(concatenatedTracklist);
                } catch (Exception e) {
                    model.addAttribute("error", "Error al procesar la lista de canciones: " + e.getMessage());
                    return "error";
                }
            }

            // Convertir y guardar el álbum
            AlbumDTO albumDTO;
            AlbumDTO savedAlbum;
            try {
                albumDTO = AlbumDTO.fromAlbum(album);
                savedAlbum = albumService.saveAlbum(albumDTO);
            } catch (Exception e) {
                model.addAttribute("error", "Error al guardar la información básica del álbum: " + e.getMessage());
                return "error";
            }

            // Procesar imagen si existe
            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    savedAlbum = albumService.saveAlbumWithImage(savedAlbum, imageFile);
                } catch (Exception e) {
                    model.addAttribute("error", "Error al guardar la imagen del álbum: " + e.getMessage());
                    return "error";
                }
            }

            // Procesar audio si existe
            if (audioPreview != null && !audioPreview.isEmpty()) {
                try {
                    savedAlbum = albumService.saveAlbumWithAudio(savedAlbum, audioPreview);
                } catch (Exception e) {
                    model.addAttribute("error", "Error al guardar el archivo de audio: " + e.getMessage());
                    return "error";
                }
            }

            return "redirect:/admin";
        } catch (Exception e) {
            model.addAttribute("error", "Error inesperado al guardar el álbum: " + e.getMessage());
            return "error";
        }
    }
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, HttpSession session) {
        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "No tienes acceso a este recurso.");
            return "error";
        }
        
        var albumOpt = albumService.getAlbumById(id);
        if (albumOpt.isEmpty()) {
            model.addAttribute("error", "Error: No se encontró el álbum con ID: " + id);
            return "error";
        }
        
        try {
            // Si la lista de artistas es nula o está vacía, inicializamos con un nuevo artista
            Album albumEntity = albumOpt.get().toAlbum();
            if (albumEntity.getArtists() == null || albumEntity.getArtists().isEmpty()) {
                albumEntity.setArtists(new ArrayList<>());
                Artist emptyArtist = new Artist();
                albumEntity.getArtists().add(emptyArtist);
            }
            model.addAttribute("album", AlbumDTO.fromAlbum(albumEntity));
            model.addAttribute("artists", artistService.getAllArtists());
            return "album/form";
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar el formulario de edición: " + e.getMessage());
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
            @RequestParam(value = "artistId", required = false) Long artistId,
            @RequestParam(value = "newArtistName", required = false) String newArtistName,
            Model model, HttpSession session) throws IOException {

        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "No tienes acceso a este recurso.");
            return "error";
        }

        if (result.hasErrors()) {
            StringBuilder errorMsg = new StringBuilder("Errores de validación: ");
            result.getAllErrors().forEach(error -> errorMsg.append(error.getDefaultMessage()).append(". "));
            model.addAttribute("error", errorMsg.toString());
            return "error";
        }

        try {
            // Verificar si el álbum existe
            var albumOpt = albumService.getAlbumById(id);
            if (albumOpt.isEmpty()) {
                model.addAttribute("error", "Error: El álbum que intentas actualizar no existe (ID: " + id + ")");
                return "error";
            }
            
            Album existingAlbum = albumOpt.get().toAlbum();

            existingAlbum.setTitle(album.getTitle());
            // Asegurarse de que la lista de artistas no sea null
            if (existingAlbum.getArtists() == null) {
                existingAlbum.setArtists(new ArrayList<>());
            } else {
                existingAlbum.getArtists().clear(); // Limpiar artistas existentes
            }
            
            // Manejar la selección o creación de artista
            if (artistId != null && artistId > 0) {
                // Usar artista existente
                var artistOpt = artistService.getArtistById(artistId);
                if (artistOpt.isEmpty()) {
                    model.addAttribute("error", "Error: El artista seleccionado no existe en la base de datos");
                    return "error";
                }
                
                artistOpt.ifPresent(artistDTO -> {
                    Artist artist = new Artist();
                    artist.setId(artistDTO.id());
                    artist.setName(artistDTO.name());
                    existingAlbum.getArtists().add(artist);
                });
            } else if (newArtistName != null && !newArtistName.trim().isEmpty()) {
                // Crear nuevo artista
                try {
                    // Crear el artista con nombre validado
                    String validatedName = newArtistName.trim();
                    Artist newArtist = new Artist(validatedName);
                    
                    // Verificar explícitamente que el nombre no sea null antes de crear el DTO
                    if (newArtist.getName() == null) {
                        newArtist.setName(validatedName); // Asegurar que el nombre esté establecido
                    }
                    
                    ArtistDTO artistDTO = ArtistDTO.fromArtist(newArtist);
                    ArtistDTO savedArtistDTO = artistService.saveArtist(artistDTO);
                    newArtist.setId(savedArtistDTO.id());
                    existingAlbum.getArtists().add(newArtist);
                } catch (Exception e) {
                    model.addAttribute("error", "Error al crear el nuevo artista: " + e.getMessage());
                    return "error";
                }
            } else {
                // Si no se seleccionó un artista ni se creó uno nuevo, mostrar error
                model.addAttribute("error", "Error: Debe seleccionar un artista existente o crear uno nuevo");
                return "error";
            }
            
            // Verificar que se haya añadido al menos un artista
            if (existingAlbum.getArtists().isEmpty()) {
                model.addAttribute("error", "Error: No se pudo asociar ningún artista al álbum");
                return "error";
            }
            
            // Actualizar los demás campos del álbum
            existingAlbum.setGenre(album.getGenre());
            existingAlbum.setDescription(album.getDescription());
            existingAlbum.setTracklist(album.getTracklist());
            existingAlbum.setYear(album.getYear());
            existingAlbum.setSpotify_url(album.getSpotify_url());
            existingAlbum.setApplemusic_url(album.getApplemusic_url());
            existingAlbum.setTidal_url(album.getTidal_url());

            // Procesar tracklist si existe
            if (existingAlbum.getTracklist() != null && !existingAlbum.getTracklist().isEmpty()) {
                try {
                    String[] tracklistArray = existingAlbum.getTracklist().split("\\r?\\n");
                    String concatenatedTracklist = String.join(" + ", tracklistArray);
                    existingAlbum.setTracklist(concatenatedTracklist);
                } catch (Exception e) {
                    model.addAttribute("error", "Error al procesar la lista de canciones: " + e.getMessage());
                    return "error";
                }
            }

            // Convertir y guardar el álbum
            AlbumDTO albumDTO;
            AlbumDTO savedAlbum;
            try {
                albumDTO = AlbumDTO.fromAlbum(existingAlbum);
                savedAlbum = albumService.saveAlbum(albumDTO);
            } catch (Exception e) {
                model.addAttribute("error", "Error al guardar la información básica del álbum: " + e.getMessage());
                return "error";
            }

            // Procesar imagen si existe
            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    savedAlbum = albumService.saveAlbumWithImage(savedAlbum, imageFile);
                } catch (Exception e) {
                    model.addAttribute("error", "Error al guardar la imagen del álbum: " + e.getMessage());
                    return "error";
                }
            }

            // Procesar audio si existe
            if (audioPreview != null && !audioPreview.isEmpty()) {
                try {
                    savedAlbum = albumService.saveAlbumWithAudio(savedAlbum, audioPreview);
                } catch (Exception e) {
                    model.addAttribute("error", "Error al guardar el archivo de audio: " + e.getMessage());
                    return "error";
                }
            }

            return "redirect:/admin";
        } catch (Exception e) {
            model.addAttribute("error", "Error inesperado al actualizar el álbum: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteAlbum(@PathVariable Long id,  Model model, HttpSession session) {

        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "No tienes acceso a este recurso.");
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
            model.addAttribute("error", "No tienes acceso a este recurso.");
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
            model.addAttribute("error", "No tienes acceso a este recurso.");
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
            model.addAttribute("error", "No tienes acceso a este recurso.");
            return "error";
        }
        
        Optional<UserDTO> userOpt = userService.getUserById(id);
        if (userOpt.isEmpty()) {
            model.addAttribute("error", "Error: No se encontró el usuario con ID: " + id);
            return "error";
        }
        
        model.addAttribute("userEdit", userOpt.get());
        return "user/form";
    }
    
    @PostMapping("/users/{id}")
    public String updateUser(@PathVariable Long id, @Valid UserDTO userDTO, BindingResult result, Model model, HttpSession session) {
        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "No tienes acceso a este recurso.");
            return "error";
        }
        
        if (result.hasErrors()) {
            model.addAttribute("error", "Error en la validación de datos");
            return "user/form";
        }

        try {
            // Asegurarse de que el ID del path coincida con el del DTO
            UserDTO updatedUserDTO = userDTO.withId(id);
            userService.updateUser(updatedUserDTO);
            return "redirect:/admin/users";
        } catch (RuntimeException e) {
            model.addAttribute("error", "Error al actualizar el usuario: " + e.getMessage());
            model.addAttribute("userEdit", userDTO);
            return "user/form";
        }
    }
    
    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, Model model, HttpSession session) {
        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "No tienes acceso a este recurso.");
            return "error";
        }

        Optional<UserDTO> userToDelete = userService.getUserById(id);
        userService.deleteUser(userToDelete.get().username());

        return "redirect:/admin/users";
    }
    
    @GetMapping("/reviews")
    public String listReviews(Model model, HttpSession session) {
        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "No tienes acceso a este recurso.");
            return "error";
        } else {
            List<ReviewDTO> reviews = reviewService.getAllReviews();
            model.addAttribute("reviews", reviews);
            return "review/admin";
        }
    }
    
    @GetMapping("/reviews/{id}/edit")
    public String showEditReviewForm(@PathVariable Long id, Model model, HttpSession session) {
        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "No tienes acceso a este recurso.");
            return "error";
        }
        
        Optional<ReviewDTO> reviewOpt = reviewService.getReviewById(id);
        if (reviewOpt.isEmpty()) {
            model.addAttribute("error", "Error: No se encontró la reseña con ID: " + id);
            return "error";
        }
        
        model.addAttribute("review", reviewOpt.get());
        return "review/form";
    }
    
    @PostMapping("/reviews/{id}")
    public String updateReview(@PathVariable Long id, @Valid ReviewDTO reviewDTO, BindingResult result, Model model, HttpSession session) {
        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "No tienes acceso a este recurso.");
            return "error";
        }
        
        if (result.hasErrors()) {
            model.addAttribute("error", "Error en la validación de datos");
            return "review/form";
        }
        
        reviewService.updateReview(reviewDTO);
        
        // Actualizar el rating promedio del álbum
        Optional<AlbumDTO> albumOpt = albumService.getAlbumById(reviewDTO.albumId());
        if (albumOpt.isPresent()) {
            AlbumDTO albumDTO = albumOpt.get();
            albumDTO.updateAverageRating(reviewService.getReviewsByAlbumId(reviewDTO.albumId()));
            albumService.saveAlbum(albumDTO);
        }
        
        return "redirect:/admin/reviews";
    }
    
    @PostMapping("/reviews/{id}/delete")
    public String deleteReview(@PathVariable Long id, Model model, HttpSession session) {
        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "No tienes acceso a este recurso.");
            return "error";
        }
        
        Optional<ReviewDTO> reviewOpt = reviewService.getReviewById(id);
        if (reviewOpt.isPresent()) {
            ReviewDTO review = reviewOpt.get();
            Long albumId = review.albumId();
            
            reviewService.deleteReview(id);
            
            // Actualizar el rating promedio del álbum
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