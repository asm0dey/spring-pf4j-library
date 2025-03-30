# OPDSKO Spring - E-Book Catalog with Plugin System

OPDSKO Spring is an OPDS (Open Publication Distribution System) catalog application for e-books, built with Spring Boot and a plugin architecture. It allows you to manage and serve your e-book collection through a standard OPDS catalog that can be accessed by various e-book reader applications.

## Features

- **Plugin-based architecture**: Support for various e-book formats through plugins (FB2, EPUB, INPX)
- **OPDS catalog**: Serve your e-books through a standard OPDS catalog
- **Search functionality**: Fast search through your e-book collection using Meilisearch
- **Distributed file storage**: Store your e-books efficiently using SeaweedFS
- **Authentication**: Optional OAuth2 authentication with Google
- **IP-based access control**: Restrict access to specific IP addresses
- **Docker Compose integration**: Easy deployment with Docker Compose

## Technologies Used

- **Spring Boot**: Core framework
- **Kotlin**: Programming language
- **PF4J**: Plugin Framework for Java
- **MongoDB/FerretDB**: Document database for metadata storage
- **Meilisearch**: Search engine
- **SeaweedFS**: Distributed file system
- **HTMX & Hyperscript**: Frontend interactivity
- **Bulma**: CSS framework
- **Docker Compose**: Container orchestration

## Project Structure

The project is organized as a multi-module Gradle project:

- **web**: Main application module
- **common**: Common interfaces and utilities
- **epub-support**: Plugin for EPUB format support
- **fb2-support**: Plugin for FB2 format support
- **fb2-to-epub-converter**: Plugin for converting FB2 to EPUB
- **inpx-support**: Plugin for INPX index files
- **seaweedfs-spring**: Spring integration for SeaweedFS
- **spring-meilisearch**: Spring integration for Meilisearch

## Installation

### Prerequisites

- Java 21 or later
- Docker and Docker Compose
- Gradle (optional, wrapper included)

### Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/spring-pf4j-library.git
   cd spring-pf4j-library
   ```

2. Create a `.env` file in the project root with the following variables:
   ```
   POSTGRES_USER=postgres
   POSTGRES_PASS=your_secure_password
   MEILI_MASTER_KEY=your_secure_master_key
   ```

3. Build the application and plugins:
   ```bash
   ./gradlew build
   ```

4. Start the Docker Compose services:
   ```bash
   docker-compose up -d
   ```

5. Run the application:
   ```bash
   ./gradlew :web:bootRun
   ```

6. Access the application at http://localhost:8080

## Configuration

The application can be configured through the `application.yml` file or environment variables:

### Scanner Configuration

Configure the sources to scan for e-books:

```yaml
scanner:
  sources:
    - /path/to/your/books/
    - /path/to/your/inpx/file.inpx
```

### Authentication Configuration

Enable and configure authentication:

```yaml
auth:
  enabled: true
  allowed-ips: 127.0.0.1,::1,192.168.0.0/24
  allowed-emails: user1@example.com,user2@example.com
  application-url: http://localhost:8080
```

Or use environment variables:

- `AUTH_ENABLED`: Set to "true" to enable authentication (default: false)
- `AUTH_ALLOWED_IPS`: Comma-separated list of IP addresses or subnets that can bypass authentication
- `AUTH_ALLOWED_EMAILS`: Comma-separated list of email addresses that are allowed to authenticate
- `AUTH_APPLICATION_URL`: The URL of the application, used for OAuth2 redirect

### Google OAuth2 Configuration

Configure Google OAuth2 for authentication:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: your-client-id
            client-secret: your-client-secret
```

Or use environment variables:

- `GOOGLE_CLIENT_ID`: Your Google OAuth2 client ID
- `GOOGLE_CLIENT_SECRET`: Your Google OAuth2 client secret

## Usage

1. Configure your book sources in the `application.yml` file.
2. Start the application.
3. The application will scan your sources and index your books.
4. Access the OPDS catalog at http://localhost:8080/common
5. Connect your e-book reader application to the OPDS catalog.

## Building a Docker Image

You can build a Docker image of the application:

```bash
./gradlew :web:bootBuildImage
```

This will create a Docker image named `asm0dey/web:0.0.1-SNAPSHOT`.

## GitHub Actions

The project includes GitHub Actions workflows for building and releasing the application and plugins:

### Main Workflow

The main workflow (`build.yml`) builds all modules and creates a release when a tag is pushed. It also builds and pushes a Docker image for the main application.

To trigger a release:
```bash
git tag v1.0.0
git push origin v1.0.0
```

### Plugin Workflows

Each plugin has its own workflow that builds and releases the plugin when a tag with the plugin's name is pushed:

- FB2 Support: `fb2-support.yml`
- EPUB Support: `epub-support.yml`
- FB2 to EPUB Converter: `fb2-to-epub-converter.yml`
- INPX Support: `inpx-support.yml`

To trigger a plugin release:
```bash
git tag fb2-support-v1.0.0
git push origin fb2-support-v1.0.0
```

### Web Module Workflow

The web module (main application) has its own workflow (`web.yml`) that builds and releases the application when a tag with the web module's name is pushed. It also builds and pushes a Docker image.

To trigger a web module release:
```bash
git tag web-v1.0.0
git push origin web-v1.0.0
```

## Development

### Adding a New Plugin

1. Create a new module for your plugin.
2. Implement the necessary interfaces from the `common` module.
3. Build your plugin with the `shadowJar` task.
4. The plugin will be automatically copied to the `plugins` directory.

### Running Tests

```bash
./gradlew test
```
