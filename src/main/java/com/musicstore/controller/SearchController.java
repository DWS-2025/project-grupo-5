
/*
package com.musicstore.controller;

import com.musicstore.model.Album;
import com.musicstore.model.User;
import com.musicstore.service.AlbumService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class SearchController {

    @Autowired
    private AlbumService albumService;

    @GetMapping("/search")
    public String search(Model model, HttpSession session) {
        List<Album> albums = albumService.getAllAlbums();
        model.addAttribute("albums", albums);
        User user = (User) session.getAttribute("user");
        if (user != null) {
            model.addAttribute("user", user);
        } else {
            User anonymousUser = new User();
            anonymousUser.setAnonymous(true);
            model.addAttribute("user", anonymousUser);
        }
        return "album/search";
    }


}


*/