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

# Important Commits
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
