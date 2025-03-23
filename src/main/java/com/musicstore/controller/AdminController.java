package com.musicstore.controller;

import com.musicstore.model.Album;
import com.musicstore.model.User;
import com.musicstore.service.AlbumService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private AlbumService albumService;


    @GetMapping
    public String listAlbums(Model model, HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "No tienes acceso a este recurso (no nos hackies)");
            return "error";
        } else {
            model.addAttribute("albums", albumService.getAllAlbums());
            return "album/admin";
        }
    }

    @GetMapping("/new")
    public String showCreateForm(Model model, HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null || !user.getUsername().equals("admin")) {
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
                              Model model, HttpSession session) throws IOException {

        User user = (User) session.getAttribute("user");

        if (user == null || !user.getUsername().equals("admin")) {
            model.addAttribute("error", "No tienes acceso a este recurso (no nos hackies)");
            return "error";
        } else {

            if (result.hasErrors()) {
                model.addAttribute("album", album);
                return "form";
            }

            try {
                if (imageFile != null && !imageFile.isEmpty()) {
                    albumService.saveAlbumWithImage(album, imageFile);
                } else {
                    albumService.saveAlbum(album);
                }
            } catch (IOException e) {
                // Handle the error appropriately
                return "album/form";
            }

            if (album.getTracklist() != null && !album.getTracklist().isEmpty()) {
                // Convert the tracklist. When introduce with enters, will separate the diferents tracks with a "+".
                String[] tracklistArray = album.getTracklist().split("\\r?\\n");
                String concatenatedTracklist = String.join(" + ", tracklistArray);
                album.setTracklist(concatenatedTracklist);
            } albumService.saveAlbum(album);

            if (audioFile2 != null && !audioFile2.isEmpty()) {
                albumService.saveAlbumWithAudio(album, audioFile2);
            } else {
                // If there is no audio, it will save the album without audio.
                albumService.saveAlbum(album);
            }
            return "redirect:/admin";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "No tienes acceso a este recurso (no nos hackies)");
            return "error";
        } else {
            // handle all the exceptions
            albumService.getAlbumById(id).ifPresent(album -> model.addAttribute("album", album));
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
            Model model, HttpSession session) throws IOException {

        User user = (User) session.getAttribute("user");

        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "No tienes acceso a este recurso (no nos hackies)");
            return "error";
        } else {

            if (result.hasErrors()) {
                model.addAttribute("album", album);
                return "form";
            }

            Album existingAlbum = albumService.getAlbumById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Álbum no encontrado: " + id));

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

            albumService.saveAlbum(existingAlbum);

            try {
                if (imageFile != null && !imageFile.isEmpty()) {
                    albumService.saveAlbumWithImage(existingAlbum, imageFile);
                } else {
                    albumService.saveAlbum(existingAlbum);
                }
            } catch (IOException e) {
                // Handle the error appropriately
                return "album" +
                        "x/form";
            }

            if (audioFile2 != null && !audioFile2.isEmpty()) {
                albumService.saveAlbumWithAudio(existingAlbum, audioFile2);
            } else {
                albumService.saveAlbum(existingAlbum);
            }
            return "redirect:/admin";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteAlbum(@PathVariable Long id,  Model model, HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null || !user.isAdmin()) {
            model.addAttribute("error", "No tienes acceso a este recurso (no nos hackies)");
            return "error";
        } else {

        albumService.deleteAlbum(id);
        return "redirect:/admin";
        }
    }
}