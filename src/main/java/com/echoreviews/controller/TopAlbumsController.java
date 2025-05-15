package com.echoreviews.controller;

import com.echoreviews.dto.AlbumDTO;
import com.echoreviews.service.AlbumService;
import com.echoreviews.service.ReviewService;
import com.echoreviews.mapper.AlbumMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/top-albums")
public class TopAlbumsController {

    @Autowired
    private AlbumService albumService;

    @Autowired
    private ReviewService reviewService;

    @GetMapping
    public String showTopAlbums(@RequestParam(required = false, defaultValue = "likes") String sortBy, Model model) {
        List<AlbumDTO> allAlbums = albumService.getAllAlbums();

        allAlbums.forEach(album -> {
            album.updateAverageRating(reviewService.getReviewsByAlbumId(album.id()));
        });

        List<AlbumDTO> topAlbums = allAlbums.stream()
                .sorted((a1, a2) -> {
                    if ("rating".equals(sortBy)) {
                        return Double.compare(a2.getAverageRating(), a1.getAverageRating());
                    } else {
                        int a2Likes = Optional.ofNullable(a2.getFavoriteUsers()).map(List::size).orElse(0);
                        int a1Likes = Optional.ofNullable(a1.getFavoriteUsers()).map(List::size).orElse(0);
                        return Integer.compare(a2Likes, a1Likes);
                    }
                })
                .limit(10)
                .collect(Collectors.toList());

        model.addAttribute("topAlbums", topAlbums);
        model.addAttribute("sortBy", sortBy);
        return "album/top-albums";
    }
}
