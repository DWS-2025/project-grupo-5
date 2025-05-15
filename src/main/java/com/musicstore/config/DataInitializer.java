package com.musicstore.config;

import com.musicstore.dto.UserDTO;
import com.musicstore.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
@DependsOn({"passwordEncoder", "userService"})
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;

    public DataInitializer(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        // Crear usuario admin si no existe
        if (userService.getUserByUsername("admin").isEmpty()) {
            UserDTO adminUser = new UserDTO(
                null,
                "admin",
                "Admin123#$!", // Contraseña que cumple con todos los requisitos
                "admin@echoreviews.com",
                true,
                null,
                null,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
            );
            userService.registerUser(adminUser);
        }
    }
} 