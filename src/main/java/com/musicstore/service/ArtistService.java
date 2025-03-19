package com.musicstore.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicstore.model.Album;
import com.musicstore.model.Artist;
import com.musicstore.model.Review;
import com.musicstore.model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ArtistService {
    private final String FILE_PATH = "data/artists.json";
    private final String IMAGE_UPLOAD_DIR = "src/main/resources/static/images";
    private final ObjectMapper objectMapper;
    
    @Autowired
    private AlbumService albumService;

    public ArtistService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        createFileIfNotExists();
        createImageDirectoryIfNotExists();
    }

    private void createFileIfNotExists() {
        try {
            File file = new File(FILE_PATH);
            if (!file.exists()) {
                File directory = file.getParentFile();
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                objectMapper.writeValue(file, new ArrayList<Artist>());
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage file", e);
        }
    }

    private void createImageDirectoryIfNotExists() {
        try {
            Path directory = Paths.get(IMAGE_UPLOAD_DIR);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create image upload directory", e);
        }
    }

    public List<Artist> getAllArtists() {
        try {
            List<Artist> artists = objectMapper.readValue(new File(FILE_PATH), new TypeReference<List<Artist>>() {});
            List<Album> albums = albumService.getAllAlbums();
            for (Artist artist : artists) {
                List<Album> artistAlbums = albums.stream()
                    .filter(album -> album.getArtists().stream()
                            .anyMatch(a -> a.getName().equalsIgnoreCase(artist.getName())))
                    .toList();
                artist.setAlbums(artistAlbums);
            }
            return artists;
        } catch (IOException e) {
            throw new RuntimeException("Could not read Artists", e);
        }
    }

    public Optional<Artist> getArtistByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Artist name cannot be null or empty");
        }
        return getAllArtists().stream()
                .filter(artist -> artist.getName().equalsIgnoreCase(name.trim()))
                .findFirst();
    }

    public Optional<Artist> getArtistById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Artist ID cannot be null");
        }
        return getAllArtists().stream()
                .filter(artist -> artist.getId().equals(id))
                .findFirst();
    }

    public Artist saveArtist(Artist artist) {
        if (artist == null) {
            throw new IllegalArgumentException("Artist cannot be null");
        }

        List<Artist> artists = getAllArtists();
        if (artist.getId() == null) {
            // New artist
            if (getArtistByName(artist.getName()).isPresent()) {
                throw new RuntimeException("Artist name already exists");
            }
            artist.setId(generateId(artists));
            artists.add(artist);
        } else {
            // Existing artist
            int index = findArtistIndex(artists, artist.getId());
            if (index != -1) {
                artists.set(index, artist);
            } else {
                throw new RuntimeException("Artist not found with ID: " + artist.getId());
            }
        }
        saveAllArtists(artists);
        return artist;
    }

    public void saveArtistWithProfileImage(Artist artist, MultipartFile profileImage) throws IOException {
        if (profileImage == null || profileImage.isEmpty()) {
            throw new IllegalArgumentException("Profile image cannot be null or empty");
        }

        String contentType = profileImage.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }

        String originalFilename = profileImage.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid file name");
        }

        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String filename = "artist_" + (artist.getId() != null ? artist.getId() : "new") + "_" + 
                         System.currentTimeMillis() + fileExtension;

        Path filePath = Paths.get(IMAGE_UPLOAD_DIR, filename);
        Files.copy(profileImage.getInputStream(), filePath);

        artist.setImageUrl("/images/" + filename);
        saveArtist(artist);
    }

    public Artist updateArtist(Artist updatedArtist) {
        if (updatedArtist == null || updatedArtist.getId() == null) {
            throw new IllegalArgumentException("Artist or Artist ID cannot be null");
        }

        List<Artist> artists = getAllArtists();
        int artistIndex = findArtistIndex(artists, updatedArtist.getId());

        if (artistIndex == -1) {
            throw new RuntimeException("Artist not found with ID: " + updatedArtist.getId());
        }

        boolean nameExists = artists.stream()
                .filter(artist -> !artist.getId().equals(updatedArtist.getId()))
                .anyMatch(artist -> artist.getName().equalsIgnoreCase(updatedArtist.getName()));

        if (nameExists) {
            throw new RuntimeException("Artist name already exists");
        }

        artists.set(artistIndex, updatedArtist);
        saveAllArtists(artists);
        return updatedArtist;
    }

    public void deleteArtist(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Artist ID cannot be null");
        }

        List<Artist> artists = getAllArtists();
        Optional<Artist> artistToDelete = getArtistById(id);

        if (artistToDelete.isEmpty()) {
            throw new RuntimeException("Artist not found with ID: " + id);
        }

        artists.removeIf(a -> a.getId().equals(id));
        saveAllArtists(artists);
    }

    private Long generateId(List<Artist> artists) {
        return artists.stream()
                .mapToLong(Artist::getId)
                .max()
                .orElse(0L) + 1;
    }

    private int findArtistIndex(List<Artist> artists, Long id) {
        for (int i = 0; i < artists.size(); i++) {
            if (artists.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private void saveAllArtists(List<Artist> artists) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_PATH), artists);
        } catch (IOException e) {
            throw new RuntimeException("Could not save artists", e);
        }
    }
}
