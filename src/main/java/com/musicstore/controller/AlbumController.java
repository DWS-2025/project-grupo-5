package com.musicstore.controller;

import com.musicstore.model.Album;
import com.musicstore.model.Review;
import com.musicstore.model.User;
import com.musicstore.service.AlbumService;
import com.musicstore.service.ReviewService;
import com.musicstore.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AlbumController {
    @Autowired
    private AlbumService albumService;
    @Autowired
    private UserService userService;
    @Autowired
    private ReviewService reviewService;

    @GetMapping
    public String listAlbums(Model model) {
        model.addAttribute("albums", albumService.getAllAlbums());
        return "album/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("album", new Album());
        return "album/form";
    }

    @PostMapping
    public String createAlbum(@Valid Album album, BindingResult result,
                              @RequestParam(value = "audioFile2", required = false) MultipartFile audioFile2,
                              Model model) throws IOException {
        if (result.hasErrors()) {
            model.addAttribute("album", album);
            return "form";
        }
        try {
            if (album.getImageFile() != null && !album.getImageFile().isEmpty()) {
                albumService.saveAlbumWithImage(album, album.getImageFile());
            } else {
                albumService.saveAlbum(album);
            }
        } catch (IOException e) {
            // Handle the error appropriately
            return "album/form";
        }

        if (album.getTracklist() != null && !album.getTracklist().isEmpty()) {
            String[] tracklistArray = album.getTracklist().split("\\r?\\n");
            String concatenatedTracklist = String.join(" + ", tracklistArray);
            album.setTracklist(concatenatedTracklist);
        } albumService.saveAlbum(album);

        if (audioFile2 != null && !audioFile2.isEmpty()) {
            albumService.saveAlbumWithAudio(album, audioFile2);
        } else {
            albumService.saveAlbum(album); // Si no hay archivo de audio, solo se guarda el álbum sin el audio
        }

        return "redirect:/admin";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        albumService.getAlbumById(id).ifPresent(album -> model.addAttribute("album", album));
        return "album/form";
    }

    @GetMapping("/{id}")
    public String viewAlbum(@PathVariable Long id, Model model, HttpSession session) {
        Optional<Album> albumOpt = albumService.getAlbumById(id);
        if (albumOpt.isEmpty()) {
            model.addAttribute("error", "El álbum no existe.");
            return "redirect:/";
        }

        // Obtener el usuario en sesión
        User user = (User) session.getAttribute("user");

        boolean isFavorite = false; // Por defecto, no es favorito
        // Si hay usuario en sesión y no es anónimo, revisamos si el álbum está en favoritos
        if (user != null && !user.isAnonymous()) {
            isFavorite = userService.isAlbumInFavorites(user.getUsername(), id);
        }

        // Pasar datos al modelo
        model.addAttribute("album", albumOpt.get());
        model.addAttribute("isFavorite", isFavorite); // Pasamos el estado de favorito
        // Obtener reseñas asociadas al álbum

        albumService.getAlbumById(id).ifPresent(album -> model.addAttribute("album", album));

        List<Review> reviews = (List<Review>) reviewService;

        Optional<Album> album = albumService.getAlbumById(id);
        if (album.isPresent()) {
            model.addAttribute("album", album.get());
            model.addAttribute("reviews", reviews);
        }


        return "album/view"; // Renderizar la vista del álbum


    }


    @PostMapping("/{id}")
    public String updateAlbum(
            @PathVariable Long id,
            @Valid Album album,
            BindingResult result,
            @RequestParam(value = "audioFile2", required = false) MultipartFile audioFile2,
            Model model) throws IOException {

        if (result.hasErrors()) {
            model.addAttribute("album", album);
            return "form";
        }

        Album existingAlbum = albumService.getAlbumById(id)
                .orElseThrow(() -> new IllegalArgumentException("Álbum no encontrado: " + id));

        existingAlbum.setTitle(album.getTitle());
        existingAlbum.setArtist(album.getArtist());
        existingAlbum.setGenre(album.getGenre());
        existingAlbum.setDescription(album.getDescription());
        existingAlbum.setTracklist(album.getTracklist());
        existingAlbum.setYear(album.getYear());
        existingAlbum.setSpotify_url(album.getSpotify_url());
        existingAlbum.setApplemusic_url(album.getApplemusic_url());
        existingAlbum.setTidal_url(album.getTidal_url());

        if (existingAlbum.getTracklist() != null && !existingAlbum.getTracklist().isEmpty()) {
            String[] tracklistArray = existingAlbum.getTracklist().split("\\r?\\n");
            String concatenatedTracklist = String.join(" + ", tracklistArray);
            existingAlbum.setTracklist(concatenatedTracklist);
        } albumService.saveAlbum(existingAlbum);

        try {
            if (album.getImageFile() != null && !album.getImageFile().isEmpty()) {
                albumService.saveAlbumWithImage(existingAlbum, album.getImageFile());
            } else {
                albumService.saveAlbum(existingAlbum);
            }
        } catch (IOException e) {
            // Handle the error appropriately
            return "album/form";
        }


        if (audioFile2 != null && !audioFile2.isEmpty()) {
            albumService.saveAlbumWithAudio(existingAlbum, audioFile2);
        } else {
            albumService.saveAlbum(existingAlbum);
        }



        return "redirect:/admin";
    }



    @PostMapping("/{id}/delete")
    public String deleteAlbum(@PathVariable Long id) {
        albumService.deleteAlbum(id);
        return "redirect:/";
    }


}