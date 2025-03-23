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

### Movie Endpoints

- `GET /api/movies/trending` - Get trending movies
- `GET /api/movies/top-rated` - Get top-rated movies
- `GET /api/movies/upcoming` - Get upcoming movies
- `GET /api/movies/now-playing` - Get now playing movies

### Authentication Endpoints (to be implemented)

- `POST /api/auth/register` - Register a new user
- `POST /api/auth/login` - Authenticate a user

## API Documentation

API documentation with Swagger UI is available at:
`http://localhost:8080/swagger-ui.html`
