package com.echoreviews.service;

import com.echoreviews.dto.AlbumDTO;
import com.echoreviews.dto.ArtistDTO;
import com.echoreviews.dto.ReviewDTO;
import com.echoreviews.dto.UserDTO;
import com.echoreviews.model.User;
import com.echoreviews.model.Artist;
import com.echoreviews.model.Album;
import com.echoreviews.model.Review;
import com.echoreviews.repository.UserRepository;
import com.echoreviews.repository.ArtistRepository;
import com.echoreviews.repository.AlbumRepository;
import com.echoreviews.repository.ReviewRepository;
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

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private ReviewRepository reviewRepository;

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
        // Check if database is empty
        if (userRepository.count() == 0) {
            // Load images
            byte[] defaultUserImage = loadImage("static/images/default-user.jpg");
            byte[] badBunnyImage = loadImage("static/images/bad-bunny.jpg");
            byte[] morganImage = loadImage("static/images/morgan.jpg");
            byte[] unVeranoSinTiImage = loadImage("static/images/un-verano-sin-ti.jpg");
            byte[] debiTirarMasFotosImage = loadImage("static/images/debi-tirar-mas-fotos.jpg");
            byte[] hotelMorganImage = loadImage("static/images/hotel-morgan.jpg");

            // Create initial users
            UserDTO adminDTO = new UserDTO(
                    null,
                    "admin",
                    "AdmIn135#$!",
                    "admin@echoreview.com",
                    true,
                    false,
                    false,
                    "/images/default-user.jpg",
                    defaultUserImage,
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>()
            );
            UserDTO savedAdmin = userService.createOrUpdateAdmin(adminDTO);

            UserDTO userDTO = new UserDTO(
                    null,
                    "raul.santamaria",
                    "RaulSanta123#$!",
                    "raul.santamaria@echoreview.com",
                    false,
                    false,
                    false,
                    "/images/default-user.jpg",
                    defaultUserImage,
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>()
            );
            UserDTO savedUser = userService.saveUser(userDTO);

            // Create initial artist
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

            // Create second album (by Bad Bunny)
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

            // Create second artist
            ArtistDTO artist2 = new ArtistDTO(
                    null,
                    "Morgan",
                    "Spain",
                    "/images/morgan.jpg",
                    new ArrayList<>(),
                    new ArrayList<>(),
                    morganImage
            );
            ArtistDTO savedArtist2 = artistService.saveArtist(artist2);

            // Create second user
            UserDTO user2DTO = new UserDTO(
                    null,
                    "maria.garcia",
                    "MariaGarcia123#$!",
                    "maria.garcia@echoreview.com",
                    false,
                    false,
                    false,
                    "/images/default-user.jpg",
                    defaultUserImage,
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>()
            );
            UserDTO savedUser2 = userService.saveUser(user2DTO);

            // Create second album (by Bad Bunny)
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

            // Create third album (by Morgan)
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

            // Create reviews
            ReviewDTO reviewDTO = new ReviewDTO(
                    null,
                    savedAlbum.id(),
                    savedUser.id(),
                    savedUser.username(),
                    savedUser.imageUrl(),
                    savedAlbum.title(),
                    savedAlbum.imageUrl(),
                    "¡Increíble álbum! **Bad Bunny** demuestra una vez más su versatilidad musical y su conexión con sus raíces puertorriqueñas.",
                    5
            );
            reviewService.addReview(savedAlbum.id(), reviewDTO);

            // Create second review
            ReviewDTO review2DTO = new ReviewDTO(
                    null,
                    savedAlbum3.id(),
                    savedUser2.id(),
                    savedUser2.username(),
                    savedUser2.imageUrl(),
                    savedAlbum3.title(),
                    savedAlbum3.imageUrl(),
                    "Hotel Morgan es una obra que destaca por su riqueza sonora y emocional. Cada pista es una habitación distinta en este viaje musical, donde Morgan demuestra su madurez artística y su capacidad para reinventarse sin perder su esencia. <script>alert()</script>",
                    5
            );
            reviewService.addReview(savedAlbum3.id(), review2DTO);

            // Create third review
            ReviewDTO review3DTO = new ReviewDTO(
                    null,
                    savedAlbum2.id(),
                    savedAdmin.id(),
                    savedAdmin.username(),
                    savedAdmin.imageUrl(),
                    savedAlbum2.title(),
                    savedAlbum2.imageUrl(),
                    "# Un Verano Sin Ti " +
                            "es un álbum que captura la esencia del verano y la nostalgia. Bad Bunny nos lleva por un viaje de emociones, desde la fiesta hasta la introspección, con un sonido fresco y contagioso.",
                    4
            );
            reviewService.addReview(savedAlbum2.id(), review3DTO);
        } else {
            System.out.println("Database is not empty. Initial data will not be loaded.");
        }
    }
}