package com.musicstore.controller;

import com.musicstore.dto.AlbumDTO;
import com.musicstore.dto.ArtistDTO;
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
                              @RequestParam(value = "artistId", required = false) Long artistId,
                              @RequestParam(value = "newArtistName", required = false) String newArtistName,
                              Model model, HttpSession session) throws IOException {

        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null || !user.username().equals("admin")) {
            model.addAttribute("error", "No tienes acceso a este recurso (no nos hackies)");
            return "error";
        }

        if (result.hasErrors()) {
            model.addAttribute("album", album);
            model.addAttribute("artists", artistService.getAllArtists());
            return "album/form";
        }

        try {
            // Asegurarse de que la lista de artistas no sea null
            if (album.getArtists() == null) {
                album.setArtists(new ArrayList<>());
            }

            // Manejar la selección o creación de artista
            if (artistId != null) {
                // Usar artista existente
                artistService.getArtistById(artistId).ifPresent(artistDTO -> {
                    Artist artist = new Artist();
                    artist.setId(artistDTO.id());
                    artist.setName(artistDTO.name());
                    album.getArtists().add(artist);
                });
            } else if (newArtistName != null && !newArtistName.trim().isEmpty()) {
                // Crear nuevo artista
                Artist newArtist = new Artist(newArtistName.trim());
                ArtistDTO savedArtistDTO = artistService.saveArtist(ArtistDTO.fromArtist(newArtist));
                newArtist.setId(savedArtistDTO.id());
                album.getArtists().add(newArtist);
            }

            // Procesar tracklist si existe
            if (album.getTracklist() != null && !album.getTracklist().isEmpty()) {
                String[] tracklistArray = album.getTracklist().split("\\r?\\n");
                String concatenatedTracklist = String.join(" + ", tracklistArray);
                album.setTracklist(concatenatedTracklist);
            }

            // Convertir y guardar el álbum
            AlbumDTO albumDTO = AlbumDTO.fromAlbum(album);
            AlbumDTO savedAlbum = albumService.saveAlbum(albumDTO);

            // Procesar imagen si existe
            if (imageFile != null && !imageFile.isEmpty()) {
                savedAlbum = albumService.saveAlbumWithImage(savedAlbum, imageFile);
            }

            // Procesar audio si existe
            if (audioFile2 != null && !audioFile2.isEmpty()) {
                savedAlbum = albumService.saveAlbumWithAudio(savedAlbum, audioFile2);
            }

            return "redirect:/admin";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", "Error al guardar el álbum: " + e.getMessage());
            model.addAttribute("artists", artistService.getAllArtists());
            return "album/form";
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