package com.musicstore.controller;

import com.musicstore.model.Album;
import com.musicstore.service.AlbumService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

import java.util.List;

@Controller
@RequestMapping("/albums")
public class AlbumController {
    @Autowired
    private AlbumService albumService;

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
    public String createAlbum(@Valid Album album, BindingResult result) {
        if (result.hasErrors()) {
            return "album/form";
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
        return "redirect:/albums";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        albumService.getAlbumById(id).ifPresent(album -> model.addAttribute("album", album));
        return "album/form";
    }

    @GetMapping("/{id}")
    public String viewAlbum(@PathVariable Long id, Model model) {
        albumService.getAlbumById(id).ifPresent(album -> model.addAttribute("album", album));
        return "album/view";
    }

    @PostMapping("/{id}")
    public String updateAlbum(@PathVariable Long id, @Valid Album album, BindingResult result) {
        if (result.hasErrors()) {
            return "album/form";
        }
        try {
            album.setId(id);
            if (album.getImageFile() != null && !album.getImageFile().isEmpty()) {
                albumService.saveAlbumWithImage(album, album.getImageFile());
            } else {
                albumService.saveAlbum(album);
            }
        } catch (IOException e) {
            // Handle the error appropriately
            return "album/form";
        }
        return "redirect:/albums";
    }

    @PostMapping("/{id}/delete")
    public String deleteAlbum(@PathVariable Long id) {
        albumService.deleteAlbum(id);  // Llamamos al servicio para eliminar el álbum
        return "redirect:/albums";  // Redirigimos a la lista de álbumes después de eliminar
    }


}