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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
public class ArtistService {
    private final String FILE_PATH = "data/artists.json";
    private final ObjectMapper objectMapper;
    @Autowired
    private AlbumService albumService;

    public void deleteArtistByName(String artistName) {
        List<Artist> artists = getAllArtists();
        Optional<Artist> artistToDelete = getArtistByName(artistName);

        if (artistToDelete.isPresent()) {
            Artist artist = artistToDelete.get();
            
            // Remove the artist from the list
            artists.removeIf(a -> a.getName().equals(artistName));
            saveAllArtists(artists);
        } else {
            throw new RuntimeException("Artist not found");
        }
    }

    public ArtistService() {
        this.objectMapper = new ObjectMapper();
        createFileIfNotExists();
    }

    private void createFileIfNotExists() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            try {
                objectMapper.writeValue(file, new ArrayList<Artist>());
            } catch (IOException e) {
                throw new RuntimeException("Could not initialize storage file", e);
            }
        }
    }

    public List<Artist> getAllArtists() {
        try {
            return objectMapper.readValue(new File(FILE_PATH), new TypeReference<List<Artist>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Could not read Artists", e);
        }
    }

    public Optional<Artist> getArtistByName(String name) {
        return getAllArtists().stream()
                .filter(artist -> artist.getName().equals(name))
                .findFirst();
    }

    public void deleteArtist(Long id) {
        List<Artist> artists = getAllArtists();
        Optional<Artist> artistToDelete = getArtistById(id);

        if (artistToDelete.isPresent()) {
            Artist artist = artistToDelete.get();
            
            // Remove the artist from the list
            artists.removeIf(a -> a.getId().equals(id));
            saveAllArtists(artists);
        } else {
            throw new RuntimeException("Artist not found");
        }
    }

    public Optional<Artist> getArtistById(Long id) {
        return getAllArtists().stream()
                .filter(artist -> artist.getId().equals(id))
                .findFirst();
    }

    public Artist saveArtist(Artist artist) {List<Artist> artists = getAllArtists();
        if (artist.getId() == null) {
            if (getArtistByName(artist.getName()).isPresent()) {
                throw new RuntimeException("Name already exists");
            }
            artist.setId(generateId(artists));
            artists.add(artist);
        } else {
            int index = findArtistIndex(artists, artist.getId());
            if (index != -1) {
                artists.set(index, artist);
            } else {
                artists.add(artist);
            }
        }
        saveAllArtists(artists);
        return artist;
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
    

    public void saveArtistWithProfileImage(Artist artist, MultipartFile profileImage) throws IOException {
        if (profileImage == null || profileImage.isEmpty()) {
            throw new RuntimeException("Profile image cannot be null or empty");
        }

        // Validate file type
        String contentType = profileImage.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Only image files are allowed");
        }

        // Generate a unique filename
        String originalFilename = profileImage.getOriginalFilename();
        String fileExtension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        String filename = "artist_" + artist.getId() + "_" + System.currentTimeMillis() + fileExtension;

        // Create images directory if it doesn't exist
        File uploadDir = new File("src/main/resources/static/images");
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // Save the file
        File destFile = new File(uploadDir.getAbsolutePath() + File.separator + filename);
        profileImage.transferTo(destFile);

        // Update Artist with image URL
        artist.setImageUrl("/images/" + filename);
        saveArtist(artist);
    }

    public Artist updateArtist(Artist updatedArtist) {
        if (updatedArtist == null || updatedArtist.getId() == null) {
            throw new RuntimeException("Artist or Artist ID cannot be null");
        }

        List<Artist> artists = getAllArtists();
        int artistIndex = findArtistIndex(artists, updatedArtist.getId());

        if (artistIndex == -1) {
            throw new RuntimeException("Artist not found with ID: " + updatedArtist.getId());
        }

        // Check if the new artist name is already taken by another artist
        boolean nameExists = artists.stream()
                .filter(artist -> !artist.getId().equals(updatedArtist.getId()))
                .anyMatch(artist -> artist.getName().equals(updatedArtist.getName()));

        if (nameExists) {
            throw new RuntimeException("Artist name already exists");
        }

        // Update the artist
        artists.set(artistIndex, updatedArtist);
        saveAllArtists(artists);

        return updatedArtist;
    }
}
