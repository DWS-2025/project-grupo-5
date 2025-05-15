package com.musicstore.service;

import com.musicstore.dto.AlbumDTO;
import com.musicstore.dto.ArtistDTO;
import com.musicstore.dto.ReviewDTO;
import com.musicstore.dto.UserDTO;
import com.musicstore.model.User;
import com.musicstore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @Autowired
    private ArtistService artistService;

    @Autowired
    private AlbumService albumService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private UserRepository userRepository;

    private byte[] loadImage(String imagePath) {
        try {
            ClassPathResource imgFile = new ClassPathResource(imagePath);
            return StreamUtils.copyToByteArray(imgFile.getInputStream());
        } catch (IOException e) {
            System.err.println("Error loading image: " + imagePath);
            return null;
        }
    }

    @Override
    public void run(String... args) throws Exception {
        // Verificar si la base de datos está vacía
        if (userRepository.count() == 0) {
            // Cargar imágenes
            byte[] defaultUserImage = loadImage("static/images/default-user.jpg");
            byte[] badBunnyImage = loadImage("static/images/bad-bunny.jpg");
            byte[] morganImage = loadImage("static/images/morgan.jpg");
            byte[] unVeranoSinTiImage = loadImage("static/images/un-verano-sin-ti.jpg");
            byte[] debiTirarMasFotosImage = loadImage("static/images/debi-tirar-mas-fotos.jpg");
            byte[] hotelMorganImage = loadImage("static/images/hotel-morgan.jpg");

            // Crear usuarios iniciales
            UserDTO adminDTO = new UserDTO(
                null,
                "admin",
                "admin123",
                "admin@echoreview.com",
                true,
                "/images/default-user.jpg",
                defaultUserImage,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
            );
            UserDTO savedAdmin = userService.saveUser(adminDTO);

            UserDTO userDTO = new UserDTO(
                null,
                "raul.santamaria",
                "password123",
                "raul.santamaria@echoreview.com",
                false,
                "/images/default-user.jpg",
                defaultUserImage,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
            );
            UserDTO savedUser = userService.saveUser(userDTO);

            // Crear artista inicial
            ArtistDTO artist1 = new ArtistDTO(
                null,
                "Bad Bunny",
                "Puerto Rico",
                "/images/bad-bunny.jpg",
                new ArrayList<>(),
                new ArrayList<>(),
                badBunnyImage
            );
            ArtistDTO savedArtist = artistService.saveArtist(artist1);

            // Crear álbum inicial
            List<Long> artistIds = new ArrayList<>();
            artistIds.add(savedArtist.id());
            List<String> artistNames = new ArrayList<>();
            artistNames.add(savedArtist.name());

            AlbumDTO albumDTO = new AlbumDTO(
                null,
                "DEBÍ TIRAR MÁS FOTOS",
                "Latino",
                "/images/debi-tirar-mas-fotos.jpg",
                null,
                "Nuevo álbum de Bad Bunny con un sonido más personal y arraigado a Puerto Rico",
                "NUEVAYOL + VOY A LLEVARTE PA PR + BAILE INOLVIDABLE + PERFUMITO NUEVO (ft. Rainao) + WELTITA (ft. Chuwi) + VELDÁ (ft. Dei V y Omar Courtz) + EL CLÚB + KETU TECRÉ + BOKETE + KLOUFRENS + TURISTA + CAFÉ CON RON (ft. Pleneros de la Cresta) + PITORRO DE COCO + LO QUE LE PASÓ A HAWAII + EOO + DTMF  + LA MUDANZA",
                2025,
                "https://open.spotify.com/album/5K79FLRUCSysQnVESLcTdb",
                "https://music.apple.com/album/deb%C3%AD-tirar-m%C3%A1s-fotos/1787022393",
                "https://tidal.com/browse/album/409386860",
                0.0,
                artistIds,
                new ArrayList<>(),
                artistNames,
                new ArrayList<>(),
                debiTirarMasFotosImage,
                null,
                null
            );
            AlbumDTO savedAlbum = albumService.saveAlbum(albumDTO);

            // Crear segundo artista
            ArtistDTO artist2 = new ArtistDTO(
                null,
                "Morgan",
                "España",
                "/images/morgan.jpg",
                new ArrayList<>(),
                new ArrayList<>(),
                morganImage
            );
            ArtistDTO savedArtist2 = artistService.saveArtist(artist2);

            // Crear segundo usuario
            UserDTO user2DTO = new UserDTO(
                null,
                "maria.garcia",
                "password456",
                "maria.garcia@echoreview.com",
                false,
                "/images/default-user.jpg",
                defaultUserImage,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
            );
            UserDTO savedUser2 = userService.saveUser(user2DTO);

            // Crear segundo álbum (de Bad Bunny)
            AlbumDTO album2DTO = new AlbumDTO(
                null,
                "Un Verano Sin Ti",
                "Latino",
                "/images/un-verano-sin-ti.jpg",
                null,
                "Este renovado proyecto de Bad Bunny venía siendo rumoreado por los fans, algunos meses después luego de la salida de EL ÚLTIMO TOUR DEL MUNDO.",
                "Moscow Mule + Después de la Playa + Me Porto Bonito (feat. Chencho Corleone) + Tití Me Preguntó + Un Ratito + Yo No Soy Celoso + Tarot (feat. JHAYCO) + Neverita + La Corriente (feat. Tony Dize) + Efecto + Party (feat. Rauw Alejandro) + Aguacero + Enséñame a Bailar + Ojitos Lindos (feat. Bomba Estéreo) + Dos Mil 16 + El Apagón + Otro Atardecer (feat. The Marías) + Un Coco + Andrea (feat. Buscabulla) + Me Fui de Vacaciones + Un Verano Sin Ti + Agosto + Callaita (feat. Tainy)",
                2022,
                "https://open.spotify.com/album/3RQQmkQEvNCY4prGKE6oc5",
                "https://music.apple.com/us/album/un-verano-sin-ti/1622045499",
                "https://tidal.com/browse/album/227498982",
                0.0,
                artistIds,
                new ArrayList<>(),
                artistNames,
                new ArrayList<>(),
                unVeranoSinTiImage,
                null,
                null
            );
            AlbumDTO savedAlbum2 = albumService.saveAlbum(album2DTO);

            // Crear tercer álbum (de Morgan)
            List<Long> artist2Ids = new ArrayList<>();
            artist2Ids.add(savedArtist2.id());
            List<String> artist2Names = new ArrayList<>();
            artist2Names.add(savedArtist2.name());

            AlbumDTO album3DTO = new AlbumDTO(
                null,
                "Hotel Morgan",
                "Pop",
                "/images/hotel-morgan.jpg",
                null,
                "Hotel Morgan es el cuarto álbum de estudio de Morgan. Grabado en Ocean Sound, Noruega, y producido por Martin García Duque.",
                "Intro: Delta + Cruel + Eror 406 + El Jimador + Radio + 1838 + Arena + Pyra + Jon & Julia + Altar + Final",
                2025,
                "https://open.spotify.com/intl-es/album/6RFZkL8rPHJeoKO4NCwUjE",
                "https://music.apple.com/in/album/hotel-morgan/1779551364",
                "https://tidal.com/browse/album/399330972",
                0.0,
                artist2Ids,
                new ArrayList<>(),
                artist2Names,
                new ArrayList<>(),
                hotelMorganImage,
                null,
                null
            );
            AlbumDTO savedAlbum3 = albumService.saveAlbum(album3DTO);

            // Crear reseñas
            ReviewDTO reviewDTO = new ReviewDTO(
                null,
                savedAlbum.id(),
                savedUser.id(),
                savedUser.username(),
                savedUser.imageUrl(),
                savedAlbum.title(),
                savedAlbum.imageUrl(),
                "¡Increíble álbum! Bad Bunny demuestra una vez más su versatilidad musical y su conexión con sus raíces puertorriqueñas.",
                5
            );
            reviewService.addReview(savedAlbum.id(), reviewDTO);

            // Crear segunda reseña
            ReviewDTO review2DTO = new ReviewDTO(
                null,
                savedAlbum3.id(),
                savedUser2.id(),
                savedUser2.username(),
                savedUser2.imageUrl(),
                savedAlbum3.title(),
                savedAlbum3.imageUrl(),
                "Hotel Morgan es una obra que destaca por su riqueza sonora y emocional. Cada pista es una habitación distinta en este viaje musical, donde Morgan demuestra su madurez artística y su capacidad para reinventarse sin perder su esencia.",
                5
            );
            reviewService.addReview(savedAlbum3.id(), review2DTO);

            // Agregar álbum a favoritos de raul.santamaria
            List<Long> updatedFavorites = new ArrayList<>(savedUser.favoriteAlbumIds());
            updatedFavorites.add(savedAlbum.id());
            UserDTO updatedUserDTO = savedUser.withFavoriteAlbumIds(updatedFavorites);
            savedUser = userService.saveUser(updatedUserDTO);

            // No se puede hacer de momento porque las sesiones no pueden ser nulas
            /*
            // Establecer relación de seguimiento mutuo entre usuarios
            userService.followUser(savedUser.id(), savedUser2.id(), null);
            userService.followUser(savedUser2.id(), savedUser.id(), null);

            // Agregar álbum a favoritos
            userService.addFavoriteAlbum(savedUser.id(), savedAlbum.id(), null);
             */
        }
    }
}