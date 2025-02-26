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

## Installation

1. Clone the repository:
```bash
git clone [repository-url]
```

2. Navigate to the project directory:
```bash
cd project-grupo-5
```

3. Build the project using Maven:
```bash
mvn clean install
```

4. Run the application:
```bash
mvn spring-boot:run
```

The application will be available at `http://localhost:8080`

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
