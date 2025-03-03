package com.musicstore.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicstore.model.Album;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FileStorageService {
    private final String FILE_PATH = System.getProperty("user.dir") + "/data/albums.json";
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

        Optional<Album> existingAlbum = albums.stream()
                .filter(a -> a.getId().equals(album.getId()))
                .findFirst();

        if (existingAlbum.isPresent()) {
            int index = albums.indexOf(existingAlbum.get());
            albums.set(index, album);
        } else {
            album.setId(generateId(albums));
            albums.add(album);
        }

        saveAllAlbums(albums);
        return album;
    }


    public void deleteAlbum(Long id) {
        try {
            File file = new File(FILE_PATH);
            if (!file.exists()) {
                System.out.println("El archivo no existe en la ruta: " + FILE_PATH);
                return;
            }

            List<Album> albums = objectMapper.readValue(file, new TypeReference<List<Album>>() {});

            boolean removed = albums.removeIf(album -> album.getId().equals(id));

            if (removed) {
                System.out.println("Álbum con ID " + id + " ha sido eliminado.");
                saveAllAlbums(albums);
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

    private void saveAllAlbums(List<Album> albums) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_PATH), albums);
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar el archivo JSON: " + e.getMessage());
        }
    }


    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Failed to store empty file.");
        }

        try {
            // Get the absolute path for the uploads directory
            String projectDir = System.getProperty("user.dir");
            String uploadDir = projectDir + "/src/main/resources/static/resources/uploads";
            File directory = new File(uploadDir);

            // Create directory if it doesn't exist
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    throw new RuntimeException("Failed to create directory: " + uploadDir);
                }
            }

            // Get the original filename without any changes
            String originalFilename = file.getOriginalFilename();

            // If the original filename is null (edge case), we set a default name
            String fileName = (originalFilename != null ? originalFilename : "unknown");

            File dest = new File(directory, fileName);

            // Transfer the file
            file.transferTo(dest);

            // Verify the file was created
            if (!dest.exists()) {
                throw new RuntimeException("Failed to store file: " + fileName);
            }

            // Return the relative path for the image URL
            return "/resources/uploads/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file. Error: " + e.getMessage(), e);
        }
    }

    public String storeAudio(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Failed to store empty file.");
        }

        try {
            // Get the absolute path for the uploads directory
            String projectDir = System.getProperty("user.dir");
            String uploadDir = projectDir + "/src/main/resources/static/snippets";
            File directory = new File(uploadDir);

            // Create directory if it doesn't exist
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    throw new RuntimeException("Failed to create directory: " + uploadDir);
                }
            }

            // Get the original filename without any changes
            String originalFilename = file.getOriginalFilename();

            // If the original filename is null (edge case), we set a default name
            String fileName = (originalFilename != null ? originalFilename : "unknown");

            File dest = new File(directory, fileName);

            // Transfer the file
            file.transferTo(dest);

            // Verify the file was created
            if (!dest.exists()) {
                throw new RuntimeException("Failed to store file: " + fileName);
            }

            // Return the relative path for the image URL
            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file. Error: " + e.getMessage(), e);
        }
    }


}