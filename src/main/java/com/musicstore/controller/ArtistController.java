package com.musicstore.controller;

import com.musicstore.model.User;
import com.musicstore.service.AlbumService;
import com.musicstore.service.ArtistService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

@Controller
public class ArtistController {
    @Autowired
    private ArtistService artistService;

    private AlbumService albumService;

    public String artistHome(Model model, HttpSession session) {

        model.addAttribute("albums", albumService.getAllAlbums());
        User user = (User) session.getAttribute("user");

        if (user != null) {
            model.addAttribute("user", user);
        } else {
            User anonymousUser = new User();
            anonymousUser.setAnonymous(true);
            model.addAttribute("user", anonymousUser);
        }
        return "album/welcome";
    }
}
