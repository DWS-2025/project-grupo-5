package com.musicstore.controller;

import com.musicstore.model.Album;
import com.musicstore.service.AlbumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/top-albums")
public class TopAlbumsController {

    @Autowired
    private AlbumService albumService;

    @GetMapping
    public String showTopAlbums(Model model) {
        List<Album> allAlbums = albumService.getAllAlbums();
        
        // Sort albums by number of favorites and get top 10
        List<Album> topAlbums = allAlbums.stream()
                .sorted((a1, a2) -> Integer.compare(a2.getFavoriteUsers().size(), a1.getFavoriteUsers().size()))
                .limit(10)
                .collect(Collectors.toList());

        model.addAttribute("topAlbums", topAlbums);
        return "album/top-albums";
    }
}