package com.musicstore.service;

import com.musicstore.model.Album;
import com.musicstore.model.Artist;
import com.musicstore.model.User;
import com.musicstore.repository.AlbumRepository;
import com.musicstore.repository.ArtistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.sql.Blob;
import java.sql.SQLException;
import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialException;

@Service
public class AlbumService {

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private FileStorageService fileStorageService;

    public List<Album> getAllAlbums() {
        return albumRepository.findAll();
    }

    public Optional<Album> getAlbumById(Long id) {
        return albumRepository.findById(id);
    }

    public Album saveAlbum(Album album) {
        if (album.getArtists() != null && !album.getArtists().isEmpty()) {
            String artistNamesString = String.join(", ", album.getArtists().stream()
                .map(Artist::getName)
                .toList());
            
            String[] artistNames = artistNamesString.split("\\s*,\\s*");
            album.getArtists().clear();
            
            for (String artistName : artistNames) {
                if (artistName != null && !artistName.trim().isEmpty()) {
                    Optional<Artist> existingArtist = artistRepository.findByNameContainingIgnoreCase(artistName.trim()).stream().findFirst();
                    Artist artist;
                    
                    if (existingArtist.isPresent()) {
                        artist = existingArtist.get();
                    } else {
                        artist = new Artist(artistName.trim());
                        artist = artistRepository.save(artist);
                    }
                    
                    album.getArtists().add(artist);
                }
            }
        }
        return albumRepository.save(album);
    }

    public Album saveAlbumWithImage(Album album, MultipartFile imageFile) throws IOException, javax.sql.rowset.serial.SerialException, java.sql.SQLException {
        Album savedAlbum = albumRepository.save(album);
        
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                savedAlbum.setImageData(imageFile.getBytes());
                savedAlbum.setImageUrl("/api/albums/" + savedAlbum.getId() + "/image");
                return albumRepository.save(savedAlbum);
            } catch (IOException e) {
                throw new RuntimeException("Failed to process image file: " + e.getMessage(), e);
            }
        }
        return savedAlbum;
    }

    /*
    public Album saveAlbumWithAudio(Album album, MultipartFile audioFile2) throws IOException {
        if (audioFile2 != null && !audioFile2.isEmpty()) {
            String audioUrl = fileStorageService.storeAudio(audioFile2);
            album.setAudioFile(audioUrl);
        }
        return albumRepository.save(album);
    }*/

    public Album saveAlbumWithAudio(Album album, MultipartFile audioFile) throws IOException, javax.sql.rowset.serial.SerialException, java.sql.SQLException {
        Album savedAlbum = albumRepository.save(album);

        if (audioFile != null && !audioFile.isEmpty()) {
            try {
                savedAlbum.setAudioData(audioFile.getBytes());
                savedAlbum.setAudioFile("/api/albums/" + savedAlbum.getId() + "/audio");
                return albumRepository.save(savedAlbum);
            } catch (IOException e) {
                throw new RuntimeException("Failed to process audio file: " + e.getMessage(), e);
            }
        }
        return savedAlbum;
    }

    public void deleteAlbum(Long id) {
        Optional<Album> albumOpt = albumRepository.findById(id);
        if (albumOpt.isPresent()) {
            Album album = albumOpt.get();
            // Clear favorite relationships
            for (User user : album.getFavoriteUsers()) {
                user.getFavoriteAlbums().remove(album);
            }
            album.getFavoriteUsers().clear();
            albumRepository.delete(album);
        } else {
            albumRepository.deleteById(id);
        }
    }
}