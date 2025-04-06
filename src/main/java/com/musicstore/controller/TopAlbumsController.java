package com.musicstore.controller;

import com.musicstore.dto.AlbumDTO;
import com.musicstore.service.AlbumService;
import com.musicstore.service.ReviewService;
import com.musicstore.mapper.AlbumMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/top-albums")
public class TopAlbumsController {

    @Autowired
    private AlbumService albumService;
    
    @Autowired
    private ReviewService reviewService;

    @Autowired
    private AlbumMapper albumMapper;
    
    @GetMapping
    public String showTopAlbums(@RequestParam(required = false, defaultValue = "likes") String sortBy, Model model) {
        List<AlbumDTO> allAlbums = albumService.getAllAlbums();
        
        allAlbums.forEach(album -> {
            album.updateAverageRating(reviewService.getReviewsByAlbumId(album.id()));
        });
    
        List<AlbumDTO> topAlbums;
        if (sortBy.equals("rating")) {
            topAlbums = allAlbums.stream()
                    .sorted((a1, a2) -> Double.compare(a2.getAverageRating(), a1.getAverageRating()))
                    .limit(10)
                    .collect(Collectors.toList());
            model.addAttribute("sortBy", "rating");
        } else {
            topAlbums = allAlbums.stream()
                    .sorted((a1, a2) -> Integer.compare(a2.getFavoriteUsers().size(), a1.getFavoriteUsers().size()))
                    .limit(10)
                    .collect(Collectors.toList());
            model.addAttribute("sortBy", "likes");
        }
    
        model.addAttribute("topAlbums", topAlbums);
        return "album/top-albums";
    }
}