package com.echoreviews.controller;

import com.echoreviews.model.Album;
import com.echoreviews.model.User;
import com.echoreviews.model.Artist;
import com.echoreviews.service.AlbumService;
import com.echoreviews.service.ArtistService;
import com.echoreviews.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import com.echoreviews.dto.UserDTO;
import com.echoreviews.dto.ArtistDTO;
import com.echoreviews.dto.AlbumDTO;
import com.echoreviews.dto.ReviewDTO;
import com.echoreviews.mapper.UserMapper;
import com.echoreviews.mapper.AlbumMapper;
import com.echoreviews.mapper.ReviewMapper;
import com.echoreviews.mapper.ArtistMapper;

@Controller
@RequestMapping("/artists")
public class ArtistController {
    @Autowired
    private ArtistService artistService;

    @Autowired
    private AlbumService albumService;

    @Autowired
    private UserService userService;

    @GetMapping
    public String artistHome(Model model, HttpSession session) {
        try {
            List<ArtistDTO> artists = artistService.getAllArtists();
            List<AlbumDTO> albums = albumService.getAllAlbums();

            // The albums are already associated with artists through JPA relationships
            // No need for manual filtering as the relationship is maintained by the database

            model.addAttribute("artists", artists);
            model.addAttribute("albums", albums);
            UserDTO userDTO = (UserDTO) session.getAttribute("user");

            if (userDTO != null) {
                model.addAttribute("user", userDTO);
            } else {
                UserDTO anonymousUser = new UserDTO(null, null, null, null, false, false, false, null, null, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
                model.addAttribute("user", anonymousUser);
            }
            return "artist/welcome";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading artists: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/{id}")
    public String viewArtist(@PathVariable Long id, Model model, HttpSession session) {
        try {
            Optional<ArtistDTO> artistOpt = artistService.getArtistById(id);
            if (artistOpt.isEmpty()) {
                model.addAttribute("error", "Artist not found");
                return "error";
            }

            Artist artist = artistOpt.get().toArtist();
            // The albums are already associated with the artist through JPA relationships
            List<AlbumDTO> albums = albumService.getAlbumsByIds(artistOpt.get().albumIds());


            model.addAttribute("artist", artist);
            model.addAttribute("albums", albums);
            UserDTO userDTO = (UserDTO) session.getAttribute("user");
            model.addAttribute("user", userDTO);
            return "artist/view";
        } catch (Exception e) {
            model.addAttribute("error", "Error viewing artist: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/new")
    public String showCreateForm(Model model, HttpSession session) {
        UserDTO userDTO = (UserDTO) session.getAttribute("user");
        if (userDTO == null || !userDTO.username().equals("admin")) {
            model.addAttribute("error", "You don't have access to this resource");
            return "error";
        }
        model.addAttribute("artist", new Artist());
        return "artist/form";
    }

    @PostMapping
    public String createArtist(@Valid Artist artist, BindingResult result,
                               @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                               Model model, HttpSession session) {
        UserDTO userDTO = (UserDTO) session.getAttribute("user");
        if (userDTO == null || !userDTO.username().equals("admin")) {
            model.addAttribute("error", "You don't have access to this resource");
            return "error";
        }

        if (result.hasErrors()) {
            model.addAttribute("error", "Please correct the errors in the form");
            return "artist/form";
        }

        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                artistService.saveArtistWithProfileImage(ArtistDTO.fromArtist(artist), imageFile);
            } else {
                artistService.saveArtist(ArtistDTO.fromArtist(artist));
            }
            return "redirect:/artists";
        } catch (Exception e) {
            model.addAttribute("error", "Error creating artist: " + e.getMessage());
            return "artist/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, HttpSession session) {
        UserDTO userDTO = (UserDTO) session.getAttribute("user");
        if (userDTO == null || !userDTO.username().equals("admin")) {
            model.addAttribute("error", "You don't have access to this resource");
            return "error";
        }

        try {
            Optional<ArtistDTO> artistOpt = artistService.getArtistById(id);
            if (artistOpt.isEmpty()) {
                model.addAttribute("error", "Artist not found");
                return "error";
            }

            model.addAttribute("artist", artistOpt.get());
            return "artist/form";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading artist: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/{id}")
    public String updateArtist(@PathVariable Long id,
                               @Valid Artist artist,
                               BindingResult result,
                               @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                               Model model, HttpSession session) {
        UserDTO userDTO = (UserDTO) session.getAttribute("user");
        if (userDTO == null || !userDTO.username().equals("admin")) {
            model.addAttribute("error", "No tienes acceso a este recurso");
            return "error";
        }

        if (result.hasErrors()) {
            model.addAttribute("error", "Please correct the errors in the form");
            return "artist/form";
        }

        try {
            artist.setId(id); // Ensure the ID is set correctly
            if (imageFile != null && !imageFile.isEmpty()) {
                artistService.saveArtistWithProfileImage(ArtistDTO.fromArtist(artist), imageFile);
            } else {
                artistService.updateArtist(ArtistDTO.fromArtist(artist));
            }
            return "redirect:/artists";
        } catch (Exception e) {
            model.addAttribute("error", "Error updating artist: " + e.getMessage());
            return "artist/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteArtist(@PathVariable Long id, Model model, HttpSession session) {
        UserDTO userDTO = (UserDTO) session.getAttribute("user");
        if (userDTO == null || !userDTO.username().equals("admin")) {
            model.addAttribute("error", "No tienes acceso a este recurso");
            return "error";
        }

        try {
            artistService.deleteArtist(id);
            return "redirect:/artists";
        } catch (Exception e) {
            model.addAttribute("error", "Error deleting artist: " + e.getMessage());
            return "error";
        }
    }
}
