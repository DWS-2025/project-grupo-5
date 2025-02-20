package com.musicstore.controller;

import com.musicstore.service.AlbumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class HomeController {
    @Autowired
    private AlbumService albumService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("albums", albumService.getAllAlbums());
        return "album/welcome";
    }

    @GetMapping("/{id}")
    public String viewAlbum(@PathVariable Long id, Model model) {
        albumService.getAlbumById(id).ifPresent(album -> model.addAttribute("album", album));
        return "album/view";
    }
}