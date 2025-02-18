package com.musicstore.controller;

import com.musicstore.model.Album;
import com.musicstore.service.AlbumService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class AlbumController {
    // Root mapping to show album list
    @GetMapping("/")
    public String showHome(Model model) {
        return listAlbums(model);
    }

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
        albumService.saveAlbum(album);
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
        album.setId(id);
        albumService.saveAlbum(album);
        return "redirect:/albums";
    }

    @DeleteMapping("/{id}")
    public String deleteAlbum(@PathVariable Long id) {
        albumService.deleteAlbum(id);
        return "redirect:/albums";
    }
}