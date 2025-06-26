# EchoReviews - Music Album Review Platform
> [!NOTE]
> You can visit our web in: [echoreviews.site](https://echoreviews.site)
## Overview
EchoReviews is a web-based platform that allows users to explore, review, and manage their favorite music albums. Built with Spring Boot, this application provides a robust and user-friendly interface for music enthusiasts to share their thoughts and discover new music.

## Features
### User Management
- User registration and authentication system
- User profiles with customizable information
- Admin and regular user role support
- Session management for secure access
- User banning system for content moderation
- High-risk user flagging for suspicious activity
- Password hashing for secure storage
- HttpOnly cookies to prevent XSS attacks
- User-agent validation to prevent session hijacking
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
- Dynamic search with multiple parameters (artist, release date)
- Support for multiple artists per album
- Image upload capability for album covers
- Admin panel for managing artists and albums
### Favorites System
- Users can mark albums as favorites
- Personal favorite album collection for each user
- Easy management of favorite albums
- Database persistence of user preferences
### Review System
- Users can write and publish album reviews
- Rating system for albums
- Comment functionality on reviews
- Rich text editing with markdown support (EasyMDE)
- HTML sanitization for secure content rendering
- Review moderation by administrators
- Risk assessment for review content 
### Technical Implementation
#### Backend
- Built with Spring Boot framework
- RESTful API architecture
- Service-oriented architecture pattern
- DTO pattern for data transfer
- Query-by-example for dynamic searching
- Entity mapping with repositories
### Data Storage
- Database integration for persistent storage
- Database tables for:
  - Users
  - Albums
  - Artists
  - Reviews
  - User favorites
### Security
- Session-based authentication
- Role-based access control
- Input validation and sanitization
- CSRF protection
- Content security policy headers
- OWASP HTML Sanitizer for user-generated content
- Session invalidation on logout
### Usage
#### User Registration
1. Navigate to the registration page 
2. Fill in required information (username, email, password)
2. Submit the registration form
3. Secure password toggle for visibility control
#### Browsing Albums
1. View the complete album catalog on the home page
2. Use filters to sort by artist, genre, or year
3. Dynamic search functionality for finding specific albums
4. Click on individual albums for detailed information
#### Managing Favorites
1. Click the heart icon on any album to add it to favorites
2. Access your favorite albums through your user profile
3. Remove albums from favorites with a single click
4. Real-time updates in the database
#### Writing Reviews
1. Navigate to an album's detail page
2. Click on "Write Review"
3. Use the markdown editor for rich text formatting
4. Enter your review text and rating
5. Submit the review for publication after sanitization

# Important commits Phase III (2)
### [fd13dab](https://github.com/DWS-2025/project-grupo-5/commit/fd13dab90f589fe8799393f3449f7cddc9d811aa)
- Final commit (@darkxvortex, @noegomezz, @paaul19, @M0ntoto)

# Important commits Phase III

## @darkxvortex

### [0f5d027](https://github.com/DWS-2025/project-grupo-5/commit/0f5d0271af03e08d1efa055b47b12b778f219f30) and [750f163](https://github.com/DWS-2025/project-grupo-5/commit/750f1630b4fa4bc8b081ff973d0a6e156a0c3ae7)
- added user-agent validation for protecting from hijacked sessions with logs

### [ec08911](https://github.com/DWS-2025/project-grupo-5/commit/ec0891180f3a2d338565cea9d6d1bab2b526ae5c)
- Add the possibility to ban users
- Mark users as high-risk
- Search for potential risk reviews
- Add toggle switches for admin, high-risk users, and banned users
- still needs to implemented that the high-risk users flag is added automatically


### [b3bc133](https://github.com/DWS-2025/project-grupo-5/commit/b3bc1336622e80cfc07b629265ff1fb7fc20a310)
- Reviews with markdown implemented as admin and user
- Security check on reviews
- some minor errors

### [5bb0e88](https://github.com/DWS-2025/project-grupo-5/commit/5bb0e8874f9111516821e9b23de0e3dba3e8d129)
  - added rich text to reviews and EasyMDE in commit [6798ce3](https://github.com/DWS-2025/project-grupo-5/commit/6798ce36448194c96ca6b4736df7f9375014a216)
  - saved markdown file to database
  - used OWASP HTML Sanitizer for displaying content
  - sanitizers.Formatting.and(sanitizers.Blocks)` blocks <script> tags
  - these tags are removed during sanitization
  - content is render securely

### [dd8be52](https://github.com/DWS-2025/project-grupo-5/commit/dd8be52c2149a33c6f518cf0c63a676846ab9e43)
- Toggle visibility of the password
- Password are now hashed in the database
- If a session is closed (log out), the session is invalidated.
- Cookies are now HttpOnly
- Admin Role implemented successfully

### [fb7d55e](https://github.com/DWS-2025/project-grupo-5/commit/fb7d55e48255766301092812bb779baeeaeb32a3)
- Added CSRF

### [40ec2b0](https://github.com/DWS-2025/project-grupo-5/commit/40ec2b03e49b33f68064378d33a92555483debe2)
  - Dinamic search with Query-by-examples spring boot
  - More changes




## @M0ntoto

### [88dd6f4](https://github.com/DWS-2025/project-grupo-5/commit/88dd6f41c61146678bd88ea75d8b61c9a76252b1) and [64ab000](https://github.com/DWS-2025/project-grupo-5/commit/64ab000e45917c60d024608460c4d42520326749) and [07870a3](https://github.com/DWS-2025/project-grupo-5/commit/07870a35c01af96c927a0af57acc3a2444490cd3)
- Api Rest Album, User, Artist with Image (JWT) (CREATE AND UPDATE)
- Postman Collection

### [0793e9b](https://github.com/DWS-2025/project-grupo-5/commit/0793e9b3e2a9db1c33787dcbb63738c1085ea723)
- Api Rest PDF (UPLOAD, DELETE, VIEW)
- Postman Collection

### [50d2190](https://github.com/DWS-2025/project-grupo-5/commit/50d21908772c91b49e3e48b61247823f42e320a5)
- Api Rest Follow and Unfollow
- Postman Collection

### [cb47f6d](https://github.com/DWS-2025/project-grupo-5/commit/cb47f6d80aebe73be2c65a17fac70257b67c4ea2)
- Add and save PDF without DataBase


## @paaul19

### [961e7b1](https://github.com/DWS-2025/project-grupo-5/commit/961e7b1e8cb4002028dfd9ab5c0e067c82cdc7f2)
  - PDFs can be uploaded via the API
    
### [15a0ad9](https://github.com/DWS-2025/project-grupo-5/commit/15a0ad9e302e1add569229d47ae31c6cb73ef863)
  - Followers and followings are back. 
  - Users can upload PDFs.
    
### [96bfb86](https://github.com/DWS-2025/project-grupo-5/commit/96bfb86c59e0a30db7d624e731c2968c666de1c4)
  - Review functionality via the API completed
    
### [570e7f0](https://github.com/DWS-2025/project-grupo-5/commit/570e7f09ead1fe227a24f015373ce5818e16dbf2)
  - Added functionality to view followers and following via the API. 
  - Added functionality to like and unlike albums.

### [727103f](https://github.com/DWS-2025/project-grupo-5/commit/727103f937e717c8b4930fb5f523b2f7d5c852be)
  - Added functionality to view top ratings and top likes on albums via the API. 
  - Fixed album ratings.

## @noegomezz

### [1f49bf0](https://github.com/DWS-2025/project-grupo-5/commit/1f49bf07d7917a488a84471e5bc8b90fce0ee557) and [aebb443](https://github.com/DWS-2025/project-grupo-5/commit/aebb44394bbd87a3eec84b63b78973524c536fbd)
- Admin can edit user profile (password included)

### [bd03e5b](https://github.com/DWS-2025/project-grupo-5/commit/bd03e5be373a10c7190ca49d073858524b426746) and [d006831](https://github.com/DWS-2025/project-grupo-5/commit/d0068318efd458898f213ce1cc9a43c11a300270)
- Full project translated
  
### [88cc52b](https://github.com/DWS-2025/project-grupo-5/commit/88cc52b8b7a6bad08b75a64a8d447fcd48f6ee85)
- JWT implemented

### [7eadfbe](https://github.com/DWS-2025/project-grupo-5/commit/7eadfbe39ba8d728363521ecfe98e853ec339fab) and [bc86b48](https://github.com/DWS-2025/project-grupo-5/commit/bc86b483db587114eb4e14fe785bc7b91d289aa5) and [925c499](https://github.com/DWS-2025/project-grupo-5/commit/925c49985d2587cae57c4c5d0471522cbb148f20) and [da7abf9](https://github.com/DWS-2025/project-grupo-5/commit/da7abf9f8bd6de6100777d5e3a7b1bcf235b492f) 
- Delete User API
- Update User API
- Update User Password API
- Upload User Image API

### [c9549e0](https://github.com/DWS-2025/project-grupo-5/commit/c9549e0d45ad4ffcdfcebf65abd57e661a8d7582) 
- Postman Collection Final Version

  
---
## Project Structure
```
project-grupo-5/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/echoreviews/
│   │   │       ├── config/
│   │   │       ├── controller/
│   │   │       │   ├── api/
│   │   │       ├── dto/
│   │   │       ├── mapper/
│   │   │       ├── model/
│   │   │       ├── repository/
│   │   │       ├── security/
│   │   │       ├── service/
│   │   │       ├── util/
│   │   │       └── EchoReviewsApplication.java
│   │   └── resources/
│   │       ├── static/
│   │       │   ├── css/
│   │       │   └── images/
│   │       ├── templates/
│   │       │   ├── album/
│   │       │   ├── artist/
│   │       │   ├── auth/
│   │       │   ├── fragments/
│   │       │   ├── review/
│   │       │   ├── reviews/
│   │       │   ├── user/
│   │       │   └── error.html
│   │       └── application.properties
├── pom.xml
└── README.md
```

## Contributors
- darkxvortex
- paaul19
- M0ntoto
- noegomezz
