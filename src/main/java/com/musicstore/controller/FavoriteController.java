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
                              HttpSession session,
                              Model model) {
        try {
            // Obtén el usuario actual de la sesión
            User user = (User) session.getAttribute("user");
            if (user == null || user.getUsername() == null) {
                model.addAttribute("error", "No se ha iniciado sesión.");
                return "error";
            }

            String username = user.getUsername();

            // Buscar el álbum
            Optional<Album> albumOptional = albumService.getAlbumById(albumId);
            if (albumOptional.isEmpty()) {
                model.addAttribute("error", "Álbum no encontrado.");
                return "error";
            }

            Album album = albumOptional.get();

            // Añadir el álbum a los favoritos del usuario (usando el servicio)
            userService.addFavoriteAlbum(username, albumId, session);


            if (!album.getFavoriteUsers().contains(username)) {
                album.getFavoriteUsers().add(username);
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
            if (user == null || user.getUsername() == null) {
                model.addAttribute("error", "No se ha iniciado sesión.");
                return "error";
            }

            String username = user.getUsername();

            // Verificar si el usuario existe (opcional, ya que está en sesión)
            Optional<User> userOptional = userService.getUserByUsername(username);
            if (userOptional.isEmpty()) {
                model.addAttribute("error", "El usuario no existe: " + username);
                return "error";
            }

            // Buscar el álbum
            Optional<Album> albumOptional = albumService.getAlbumById(albumId);
            if (albumOptional.isEmpty()) {
                model.addAttribute("error", "Álbum no encontrado.");
                return "error";
            }

            Album album = albumOptional.get();

            // Eliminar el álbum de los favoritos del usuario (usando el servicio)
            userService.deleteFavoriteAlbum(username, albumId, session);

            // Eliminar el usuario de la lista de favoritos del álbum (si está presente)
            if (album.getFavoriteUsers().contains(username)) {
                album.getFavoriteUsers().remove(username);
                albumService.saveAlbum(album); // Guardamos el álbum actualizado
            }

            return "redirect:/" + albumId; // Redirige a la página de favoritos del usuario
        } catch (Exception e) {
            model.addAttribute("error", "Ocurrió un error al eliminar el álbum de favoritos: " + e.getMessage());
            return "error"; // Renderiza página con el mensaje de error
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
        return "album/favorites"; // Renderizar la vista HTML "favorites.html"
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


}

