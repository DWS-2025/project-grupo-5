# EchoReview - Music Album Review Platform

## Overview
EchoReview is a web-based platform that allows users to explore, review, and manage their favorite music albums. Built with Spring Boot, this application provides a robust and user-friendly interface for music enthusiasts to share their thoughts and discover new music.

## Features

### User Management
- User registration and authentication system
- User profiles with customizable information
- Admin and regular user role support
- Session management for secure access

### Album Management
- Comprehensive album catalog with detailed information
- Album details including:
  - Title
  - Artist
  - Genre
  - Release Year
  - Cover Art
  - Description
  - Tracklist
  - Streaming Platform Links (Spotify, Apple Music, Tidal)

### Favorites System
- Users can mark albums as favorites
- Personal favorite album collection for each user
- Easy management of favorite albums

### Review System
- Users can write and publish album reviews
- Rating system for albums
- Comment functionality on reviews

## Technical Implementation

### Backend
- Built with Spring Boot framework
- RESTful API architecture
- JSON-based data storage system
- Service-oriented architecture pattern

### Data Storage
- File-based JSON storage for:
  - User data (users.json)
  - Album information (albums.json)
  - Reviews (reviews.json)

### Security
- Session-based authentication
- Role-based access control
- Input validation and sanitization

## Usage

### User Registration
1. Navigate to the registration page
2. Fill in required information (username, email, password)
3. Submit the registration form

### Browsing Albums
- View the complete album catalog on the home page
- Use filters to sort by artist, genre, or year
- Click on individual albums for detailed information

### Managing Favorites
- Click the heart icon on any album to add it to favorites
- Access your favorite albums through your user profile
- Remove albums from favorites with a single click

### Writing Reviews
1. Navigate to an album's detail page
2. Click on "Write Review"
3. Enter your review text and rating
4. Submit the review

# IMPORTANT COMMITS
## ```@all```
- [6abaa14](https://github.com/DWS-2025/project-grupo-5/commit/6abaa14bc397178b8cb04eb55f1177e0df9df65b)
  - Functional login @noegomezz
  - Favorites and Reviews views @paaul19
  - Dinamic queue @M0ntoto
  - API for Users, albums, artist done @darkxvortex
 

## @darkxvortex

- [b46c56b](https://github.com/DWS-2025/project-grupo-5/commit/b46c56bef9343bcf7a8209654dd4046ccd57ebb6)
  - API REST controllers for User, Artist, Reviews and Albums (need some fixed)

- [e5d0171](https://github.com/DWS-2025/project-grupo-5/commit/e5d017153c8cbd89613453c8b5747b4ecd871963)
  - AJAX pagination added on API
  - All GET API functionality
    
- [4dd6b7d](https://github.com/DWS-2025/project-grupo-5/commit/4dd6b7da3c381a414701a94017d67d72a30e077e) 
  - Functional Login with database
  - DataLoader added
  - Some errors solved
- [f341b7d](https://github.com/DWS-2025/project-grupo-5/commit/f341b7d71b1f8d2a04c373e9829fc98c02d016de)
  - IMPORTANT COMMIT - Solve problems with H2
 


## @M0ntoto

- [6e5b9b5](https://github.com/DWS-2025/project-grupo-5/commit/6e5b9b5c4bcd2a23f6d56c5e86a5a761e9423ca0)
  - Functional Dinamic Query with 2 parameters (artist, release date).
  - Name artist fixed
    
- [66cca20](https://github.com/DWS-2025/project-grupo-5/commit/66cca20d2200df86402d8dc4e7583290dedf0da1)
  - Files (Albums Covers) can be upload in database

- [afdad1a](https://github.com/DWS-2025/project-grupo-5/commit/afdad1aae10f9aefd02ef1a865d43883cb501be0)
  - API REST Albums functionally (without DTOS)

- [5fe1988](https://github.com/DWS-2025/project-grupo-5/commit/5fe19884a173899f9ce3d31e70da5d4cace66255)
  - API REST Artist functionally (without DTOS)


## @paaul19

- [d8fb735](https://github.com/DWS-2025/project-grupo-5/commit/d8fb7358058ba8d1884c14405938734ad3028dc8)
  - Album view fixed.
  - Reviews and likes in bbdd.
    
- [caca0a5](https://github.com/DWS-2025/project-grupo-5/commit/caca0a509f20c1cb14af9864ac93784debde0ce0)
  - Admin artists view done.
  - Artists now can be deleted.
  - Albums can have more than 1 artist.
    
- [f73fd81](https://github.com/DWS-2025/project-grupo-5/commit/f73fd8133e6cd8d9845f3b755e9ee44e61a0652f)
  - Bbdd integration finished
    
- [c610762](https://github.com/DWS-2025/project-grupo-5/commit/c610762ea7edeb74b023a571edbe48dc180f4901)
  - Several issues have been fixed

## @noegomezz

- [f3bcfb8](https://github.com/DWS-2025/project-grupo-5/commit/f3bcfb8744ee35e97eeb234a2f2bd2eb8c9d4423)

   - Services finished using DTOs
  
- [2c982dd](https://github.com/DWS-2025/project-grupo-5/commit/2c982dd54bf241bf2dd51752af72935f03578a7a)

   - Refactoring multiple services and controllers 

- [5f1b58f](https://github.com/DWS-2025/project-grupo-5/commit/5f1b58f52a28e96da135aa281bda30a8a048b2d9)
 
   - All DTOs created

- [28c5410](https://github.com/DWS-2025/project-grupo-5/commit/28c54106b1cc4740f0b3fb1dc871f897633d2b61)

   - Classes migration to Database

- [df32fa8](https://github.com/DWS-2025/project-grupo-5/commit/df32fa8a31e4648401c9e504b548d250571fa660)

   - Class Artist created and almost completed
   
---
## Project Structure
```
project-grupo-5/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/musicstore/
│   │   │       ├── controller/
│   │   │       ├── model/
│   │   │       └── service/
│   │   └── resources/
│   └── test/
├── data/
│   ├── albums.json
│   ├── reviews.json
│   └── users.json
└── pom.xml
```

## Contributors
- darkxvortex
- paaul19
- M0ntoto
- noegomezz
