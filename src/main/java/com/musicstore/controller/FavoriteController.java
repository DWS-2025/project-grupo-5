package com.musicstore.controller;

import com.musicstore.model.Album;
import com.musicstore.model.User;
import com.musicstore.service.AlbumService;
import com.musicstore.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/favorites")
public class FavoriteController {

    @Autowired
    private UserService userService;

    @Autowired
    private AlbumService albumService;

    // Añadir un álbum a favoritos

    @PostMapping("/add")
    public String addFavorite(@RequestParam Long albumId,
                              @RequestParam String username,
                              Model model,
                              HttpSession session) { // Inyectamos la sesión
        try {
            // Validar que el usuario existe
            if (userService.getUserByUsername(username).isEmpty()) {
                model.addAttribute("error", "El usuario no existe: " + username);
                return "welcome"; // Renderiza página con mensaje de error
            }

            // Añadir a favoritos y actualizar sesión
            userService.addFavoriteAlbum(username, albumId, session);

            return "redirect:/" + albumId; // Redirige a detalles del álbum
        } catch (Exception e) {
            model.addAttribute("error", "Ocurrió un error al añadir el álbum a favoritos: " + e.getMessage());
            return "welcome"; // Renderiza página con mensaje de error
        }
    }


    @PostMapping("/delete")
    public String deleteFavorite(@RequestParam(required = true) Long albumId,
                                 @RequestParam(required = true) String username,
                                 Model model,
                                 HttpSession session) { // Inyectamos la sesión
        try {
            // Validar que el usuario existe
            if (userService.getUserByUsername(username).isEmpty()) {
                model.addAttribute("error", "El usuario no existe: " + username);
                return "welcome"; // Renderiza página con mensaje de error
            }

            // Eliminar de favoritos y actualizar la sesión
            userService.deleteFavoriteAlbum(username, albumId, session); // Pasamos la sesión

            return "redirect:/" + albumId; // Redirige a detalles del álbum
        } catch (Exception e) {
            model.addAttribute("error", "Ocurrió un error al eliminar el álbum de favoritos: " + e.getMessage());
            return "welcome"; // Renderiza página con mensaje de error
        }
    }



    @GetMapping
    public String showFavorites(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user"); // Obtenemos el usuario en sesión
        if (user == null || user.getUsername() == null) {
            model.addAttribute("error", "No se ha iniciado sesión o falta el nombre de usuario.");
            return "error"; // O redirige a la página de login
        }

        String username = user.getUsername();

        // Obtener álbumes favoritos del usuario
        List<Long> favoriteAlbumIds = userService.getFavoriteAlbums(username);
        List<Album> favoriteAlbums = favoriteAlbumIds.stream()
                .map(albumId -> albumService.getAlbumById(albumId).orElse(null))
                .filter(album -> album != null) // Filtrar álbumes nulos
                .collect(Collectors.toList());

        // Agregar datos al modelo
        model.addAttribute("username", username);
        model.addAttribute("favoriteAlbums", favoriteAlbums);
        return "favorites"; // Renderizar la vista HTML "favorites.html"
    }




}

