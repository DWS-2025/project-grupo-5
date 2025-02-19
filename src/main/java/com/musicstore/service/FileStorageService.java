package com.musicstore.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicstore.model.Album;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class FileStorageService {
    private final String FILE_PATH = "data/albums.json";
    private final ObjectMapper objectMapper;

    public FileStorageService() {
        this.objectMapper = new ObjectMapper();
        createFileIfNotExists();
    }

    private void createFileIfNotExists() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            try {
                objectMapper.writeValue(file, new ArrayList<Album>());
            } catch (IOException e) {
                throw new RuntimeException("Could not initialize storage file", e);
            }
        }
    }

    public List<Album> getAllAlbums() {
        try {
            return objectMapper.readValue(new File(FILE_PATH), new TypeReference<List<Album>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Could not read albums", e);
        }
    }

    public Optional<Album> getAlbumById(Long id) {
        return getAllAlbums().stream()
                .filter(album -> album.getId().equals(id))
                .findFirst();
    }

    public Album saveAlbum(Album album) {
        List<Album> albums = getAllAlbums();
        if (album.getId() == null) {
            album.setId(generateId(albums));
            albums.add(album);
        } else {
            int index = findAlbumIndex(albums, album.getId());
            if (index != -1) {
                albums.set(index, album);
            } else {
                albums.add(album);
            }
        }
        saveAllAlbums(albums);
        return album;
    }

    public void deleteAlbum(Long id) {
        try {
            // Leer el archivo JSON y convertirlo a una lista de álbumes
            File file = new File(FILE_PATH);
            if (!file.exists()) {
                System.out.println("El archivo no existe en la ruta: " + FILE_PATH);
                return;  // Si el archivo no existe, no procedemos
            }

            // Leer los álbumes desde el archivo
            List<Album> albums = objectMapper.readValue(file, new TypeReference<List<Album>>() {});

            // Eliminar el álbum que coincide con el ID
            boolean removed = albums.removeIf(album -> album.getId().equals(id));

            // Si el álbum fue eliminado, lo guardamos de nuevo
            if (removed) {
                System.out.println("Álbum con ID " + id + " ha sido eliminado.");
                saveAllAlbums(albums);  // Guardamos la lista actualizada
            } else {
                System.out.println("No se encontró el álbum con ID " + id);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Hubo un error al leer o escribir el archivo JSON.");
        }
    }




    private Long generateId(List<Album> albums) {
        return albums.stream()
                .mapToLong(Album::getId)
                .max()
                .orElse(0L) + 1;
    }

    private int findAlbumIndex(List<Album> albums, Long id) {
        for (int i = 0; i < albums.size(); i++) {
            if (albums.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private void saveAllAlbums(List<Album> albums) {
        try {
            // Sobrescribir el archivo con la lista actualizada
            objectMapper.writeValue(new File(FILE_PATH), albums);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error al guardar el archivo.");
        }
    }
}