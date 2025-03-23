# Movie Social Backend

A Spring Boot backend for the Movie Social application, serving as a proxy for The Movie Database (TMDB) API and managing user data.

## Features

- Proxy for TMDB API calls
- User authentication and authorization
- RESTful API for movie data
- MySQL database integration

## Technology Stack

- Java 17
- Spring Boot 3.2.5
- Spring Security
- Spring Data JPA
- MySQL Database
- Gradle
- Lombok
- Swagger/OpenAPI

## Getting Started

### Prerequisites

- Java 17 or higher
- MySQL Server
- TMDB API Key (register at [themoviedb.org](https://www.themoviedb.org))

### Configuration

1. Edit `src/main/resources/application.properties`:
   - Set your database credentials
   - Add your TMDB API key
   - Configure JWT secret (for production)

### Building and Running

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun
```

The API will be available at `http://localhost:8080`

## API Endpoints

### Content Endpoints

- `GET /api/contents/trending` - Get trending movies
- `GET /api/contents/top-rated` - Get top-rated movies
- `GET /api/contents/upcoming` - Get upcoming movies
- `GET /api/contents/now-playing` - Get now playing movies
- `GET /api/contents/movie/{id}` - Get movie details
- `GET /api/contents/movie/{id}/videos` - Get movie videos
- `GET /api/contents/movie/{id}/reviews` - Get movie reviews
- `GET /api/contents/tv/{id}` - Get TV show details
- `GET /api/contents/tv/{id}/videos` - Get TV show videos
- `GET /api/contents/tv/{id}/reviews` - Get TV show reviews

### Authentication Endpoints (to be implemented)

- `POST /api/auth/register` - Register a new user
- `POST /api/auth/login` - Authenticate a user

## API Documentation

API documentation with Swagger UI is available at:
`http://localhost:8080/swagger-ui.html`
