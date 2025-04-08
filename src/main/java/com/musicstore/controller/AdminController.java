package com.musicstore.controller;

import com.musicstore.dto.AlbumDTO;
import com.musicstore.dto.UserDTO;
import com.musicstore.model.Album;
import com.musicstore.model.Artist;
import com.musicstore.model.User;
import com.musicstore.service.AlbumService;
import com.musicstore.service.ArtistService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.ArrayList;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private AlbumService albumService;

    @Autowired
    private ArtistService artistService;

    @GetMapping
    public String listAlbums(Model model, HttpSession session) {

        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user.username() == null || !user.username().equals("admin")) {
            model.addAttribute("error", "No tienes acceso a este recurso (no nos hackies)");
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

        if (user == null || !user.username().equals("admin")) {
            model.addAttribute("error", "No tienes acceso a este recurso (no nos hackies)");
            return "error";
        } else{
            model.addAttribute("album", new Album());
            return "album/form";
        }
    }

    @PostMapping
    public String createAlbum(@Valid Album album, BindingResult result,
                              @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                              @RequestParam(value = "audioFile2", required = false) MultipartFile audioFile2,
                              Model model, HttpSession session) throws IOException, javax.sql.rowset.serial.SerialException, java.sql.SQLException {

        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.username().equals("admin")) {
            model.addAttribute("error", "No tienes acceso a este recurso (no nos hackies)");
            return "error";
        } else {

            if (result.hasErrors()) {
                model.addAttribute("album", album);
                return "form";
            }

            AlbumDTO albumDTO = AlbumDTO.fromAlbum(album);

// Guarda el álbum inicial
            AlbumDTO savedAlbum = albumService.saveAlbum(albumDTO);

// Si hay imagen, la sube
            try {
                if (imageFile != null && !imageFile.isEmpty()) {
                    savedAlbum = albumService.saveAlbumWithImage(savedAlbum, imageFile);
                }
            } catch (IOException e) {
                e.printStackTrace();
                model.addAttribute("error", "Error al subir la imagen: " + e.getMessage());
                return "album/form";
            }

// Si hay audio, lo sube
            if (audioFile2 != null && !audioFile2.isEmpty()) {
                albumService.saveAlbumWithAudio(savedAlbum, audioFile2);
            }


            if (album.getTracklist() != null && !album.getTracklist().isEmpty()) {
                // Convert the tracklist. When introduce with enters, will separate the diferents tracks with a "+".
                String[] tracklistArray = album.getTracklist().split("\\r?\\n");
                String concatenatedTracklist = String.join(" + ", tracklistArray);
                album.setTracklist(concatenatedTracklist);
            } albumService.saveAlbum(AlbumDTO.fromAlbum(album));

            if (audioFile2 != null && !audioFile2.isEmpty()) {
                albumService.saveAlbumWithAudio(AlbumDTO.fromAlbum(album), audioFile2);
            } else {
                // If there is no audio, it will save the album without audio.
                albumService.saveAlbum(AlbumDTO.fromAlbum(album));
            }
            return "redirect:/admin";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, HttpSession session) {
        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.username().equals("admin")) {
            model.addAttribute("error", "No tienes acceso a este recurso (no nos hackies)");
            return "error";
        } else {
            albumService.getAlbumById(id).ifPresent(album -> {
                // Si la lista de artistas es nula o está vacía, inicializamos con un nuevo artista
                Album albumEntity = album.toAlbum();
                if (albumEntity.getArtists() == null || albumEntity.getArtists().isEmpty()) {
                    albumEntity.setArtists(new ArrayList<>());
                    Artist emptyArtist = new Artist();
                    albumEntity.getArtists().add(emptyArtist);
                }
                model.addAttribute("album", AlbumDTO.fromAlbum(albumEntity));
            });
            return "album/form";
        }
    }


    @PostMapping("/{id}")
    public String updateAlbum(
            @PathVariable Long id,
            @Valid Album album,
            BindingResult result,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "audioFile2", required = false) MultipartFile audioFile2,
            Model model, HttpSession session) throws IOException, javax.sql.rowset.serial.SerialException, java.sql.SQLException {

        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.username().equals("admin")) {
            model.addAttribute("error", "No tienes acceso a este recurso (no nos hackies)");
            return "error";
        } else {

            if (result.hasErrors()) {
                model.addAttribute("album", album);
                return "form";
            }

            Album existingAlbum = albumService.getAlbumById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Album not found: " + id)).toAlbum();

            existingAlbum.setTitle(album.getTitle());
            existingAlbum.setArtists(album.getArtists());
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
            }

            albumService.saveAlbum(AlbumDTO.fromAlbum(existingAlbum));

            try {
                if (imageFile != null && !imageFile.isEmpty()) {
                    albumService.saveAlbumWithImage(AlbumDTO.fromAlbum(existingAlbum), imageFile);
                } else {
                    albumService.saveAlbum(AlbumDTO.fromAlbum(existingAlbum));
                }
            } catch (IOException e) {
                // Handle the error appropriately
                return "album" +
                        "x/form";
            }

            if (audioFile2 != null && !audioFile2.isEmpty()) {
                albumService.saveAlbumWithAudio(AlbumDTO.fromAlbum(existingAlbum), audioFile2);
            } else {
                albumService.saveAlbum(AlbumDTO.fromAlbum(existingAlbum));
            }
            return "redirect:/admin";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteAlbum(@PathVariable Long id,  Model model, HttpSession session) {

        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.username().equals("admin")) {
            model.addAttribute("error", "No tienes acceso a este recurso (no nos hackies)");
            return "error";
        } else {

            albumService.deleteAlbum(id);
            return "redirect:/admin";
        }
    }

    @GetMapping("/artists")
    public String listArtists(Model model, HttpSession session) {
        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.username().equals("admin")) {
            model.addAttribute("error", "No tienes acceso a este recurso (no nos hackies)");
            return "error";
        } else {
            model.addAttribute("artists", artistService.getAllArtists());
            return "artist/admin";
        }
    }
}