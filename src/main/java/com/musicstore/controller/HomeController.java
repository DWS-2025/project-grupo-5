package com.musicstore.controller;

import com.musicstore.service.AlbumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @Autowired
    private AlbumService albumService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("albums", albumService.getAllAlbums());
        return "album/welcome";
    }
}