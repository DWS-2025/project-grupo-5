package com.musicstore.controller;

import com.musicstore.model.Review;
import com.musicstore.model.User;
import com.musicstore.service.AlbumService;
import com.musicstore.service.ReviewService;
import com.musicstore.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;
    @Autowired
    private AlbumService albumService;
    @Autowired
    private UserService userService;

    @PostMapping("/{albumId}")
    public String addReview(
            @PathVariable Long albumId,
            @RequestParam String content,
            @RequestParam int rating,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            if (rating < 1 || rating > 5 || content.isBlank()) {
                System.err.println("Datos inválidos. Reseña no guardada.");
                return "redirect:/" + albumId + "?error=invalidReview";
            }

            Review review = new Review();
            review.setContent(content);
            review.setRating(rating);
            review.setUsername(user.getUsername());
            review.setUserId(user.getId());
            review.setAlbumId(albumId);

            System.out.println("Guardando reseña del usuario: " + user.getUsername());
            reviewService.addReview(albumId, review); // Asociar la reseña al álbum
        }
        return "redirect:/" + albumId;
    }

    @GetMapping("/{albumId}")
    public String getReviewsByAlbumId(@PathVariable Long albumId, Model model) {
        model.addAttribute("reviews", reviewService.getReviewsByAlbumId(albumId));
        return "reviews/list"; // Vista que mostrará las reseñas
    }
}
