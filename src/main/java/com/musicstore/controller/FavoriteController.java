package com.musicstore.controller;

import com.musicstore.model.Album;
import com.musicstore.model.Review;
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
                              HttpSession session,
                              Model model) {
        try {
            // Obtén el usuario actual de la sesión
            User user = (User) session.getAttribute("user");
            if (user == null || user.getId() == null) {
                model.addAttribute("error", "No se ha iniciado sesión.");
                return "error";
            }

            Long auxUserId = user.getId();


            // Buscar el álbum
            Optional<Album> albumOptional = albumService.getAlbumById(albumId);
            if (albumOptional.isEmpty()) {
                model.addAttribute("error", "Álbum no encontrado.");
                return "error";
            }

            Album album = albumOptional.get();

            // Añadir el álbum a los favoritos del usuario (usando el servicio)
            userService.addFavoriteAlbum(auxUserId, albumId, session);

            if (!album.getFavoriteUsers().contains(auxUserId.toString())) {
                album.getFavoriteUsers().add(auxUserId.toString());
                albumService.saveAlbum(album); // Guardar los cambios
            }

            return "redirect:/" + albumId; // Redirige a la página de favoritos del usuario
        } catch (Exception e) {
            model.addAttribute("error", "Ocurrió un error al añadir el álbum a favoritos: " + e.getMessage());
            return "error"; // Renderiza página con el mensaje de error
        }
    }



    @PostMapping("/delete")
    public String deleteFavorite(@RequestParam Long albumId,
                                 HttpSession session,
                                 Model model) {
        try {
            // Obtén el usuario actual de la sesión
            User user = (User) session.getAttribute("user");
            if (user == null || user.getId() == null) {
                model.addAttribute("error", "No se ha iniciado sesión.");
                return "error";
            }

            Long userId = user.getId();  // Usamos el id en lugar del username

            // Buscar el álbum
            Optional<Album> albumOptional = albumService.getAlbumById(albumId);
            if (albumOptional.isEmpty()) {
                model.addAttribute("error", "Álbum no encontrado.");
                return "error";
            }

            Album album = albumOptional.get();

            // Eliminar el álbum de los favoritos del usuario (usando el servicio)
            userService.deleteFavoriteAlbum(userId, albumId, session);

            // Eliminar el usuario de la lista de favoritos del álbum (si está presente)
            if (album.getFavoriteUsers().contains(userId.toString())) {
                album.getFavoriteUsers().remove(userId.toString());
                albumService.saveAlbum(album); // Guardamos el álbum actualizado
            }

            return "redirect:/" + albumId; // Redirige a la página de favoritos del usuario
        } catch (Exception e) {
            model.addAttribute("error", "Ocurrió un error al eliminar el álbum de favoritos: " + e.getMessage());
            return "error"; // Renderiza página con el mensaje de error
        }
    }




    @GetMapping("/{username}")
    public String showFavorites(@PathVariable String username, HttpSession session, Model model) {
        // Get the requested user's favorite albums
        List<Long> favoriteAlbumIds = userService.getFavoriteAlbums(username);
        if (favoriteAlbumIds == null) {
            model.addAttribute("error", "Usuario no encontrado.");
            return "error";
        }

        List<Album> favoriteAlbums = favoriteAlbumIds.stream()
                .map(albumId -> albumService.getAlbumById(albumId).orElse(null))
                .filter(album -> album != null)
                .collect(Collectors.toList());

        // Get the user information (including the profile image)
        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            model.addAttribute("userProfileImage", user.getImageUrl()); // Add user profile image URL
        }

        // Get the current logged-in user (if any)
        User currentUser = (User) session.getAttribute("user");
        boolean isOwnProfile = currentUser != null && currentUser.getUsername().equals(username);

        // Add data to the model
        model.addAttribute("username", username);
        model.addAttribute("favoriteAlbums", favoriteAlbums);
        model.addAttribute("isOwnProfile", isOwnProfile);
        return "album/favorites";
    }



    public void addUserToFavorite(Long albumId, String username) {
        Optional<Album> albumOpt = albumService.getAlbumById(albumId);
        if (albumOpt.isPresent()) {
            Album album = albumOpt.get();
            if (!album.getFavoriteUsers().contains(username)) {
                album.getFavoriteUsers().add(username);
            }
            albumService.saveAlbum(album);
        }
    }

    public void removeUserFromFavorite(Long albumId, String username) {
        Optional<Album> albumOpt = albumService.getAlbumById(albumId);
        if (albumOpt.isPresent()) {
            Album album = albumOpt.get();
            album.getFavoriteUsers().remove(username);
            albumService.saveAlbum(album);
        }
    }

    @GetMapping("/error")
    public String error(Model model) {
        model.addAttribute("error", "You must be logged in to access this page.");
        return "error";
    }
}

