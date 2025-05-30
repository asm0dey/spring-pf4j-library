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
- **Mongock**: Database migration tool for MongoDB
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

- Docker and Docker Compose
- Git (optional, for cloning the repository)

### Recommended Installation Method

The recommended way to install and run OPDSKO Spring is using Docker Compose with the pre-built Docker image.

1. Create a `.env` file in the project root with the following variables:
   ```
   POSTGRES_USER=postgres
   POSTGRES_PASS=your_secure_password
   MEILI_MASTER_KEY=your_secure_master_key
   ```

2. Create a `compose.yml` file with the following content:
   ```yaml
   services:
     postgres:
       image: ghcr.io/ferretdb/postgres-documentdb:17-0.102.0-ferretdb-2.1.0
       restart: on-failure
       #    ports:
       #      - 5433:5432
       environment:
         - POSTGRES_USER=${POSTGRES_USER}
         - POSTGRES_PASSWORD=${POSTGRES_PASS}
         - POSTGRES_DB=postgres
         - PGDATA=/var/lib/postgresql/data/pgdata
       volumes:
         - ./data/postgres:/var/lib/postgresql/data/pgdata

     ferretdb:
       image: ghcr.io/ferretdb/ferretdb:2.1.0
       restart: on-failure
       depends_on:
         - postgres
       environment:
         - FERRETDB_POSTGRESQL_URL=postgres://${POSTGRES_USER}:${POSTGRES_PASS}@postgres:5432/postgres
         - MONGODB_ROOT_USERNAME=${POSTGRES_USER}
         - MONGODB_ROOT_PASSWORD=${POSTGRES_PASS}
       labels:
         org.springframework.boot.service-connection: mongo

     meilisearch:
       image: getmeili/meilisearch:latest
       environment:
         - MEILI_MASTER_KEY=${MEILI_MASTER_KEY}
       volumes:
         - "./data/meilisearch:/meili_data"

     seaweedfs:
       image: chrislusf/seaweedfs
       restart: on-failure
       command: "server -master -filer -ip.bind=0.0.0.0"
       volumes:
         - ./data/seaweedfs:/data
       healthcheck:
         test: netstat -an | grep 9333 > /dev/null; if [ 0 != $? ]; then exit 1; fi;
         interval: 5s
         timeout: 60s
         retries: 10
         start_period: 5s

     app:
       image: ghcr.io/asm0dey/opdsko:latest
       restart: unless-stopped
       ports:
         - 8081:8080
       volumes:
         - ./plugins:/workspace/plugins # Plugins path
         - ./book:/books:ro # Mount with books
       depends_on:
         - ferretdb
         - meilisearch
         - seaweedfs
       environment:
         - SCANNER_SOURCES=/books 
         - SPRING_DATA_MONGODB_USERNAME=${POSTGRES_USER}
         - SPRING_DATA_MONGODB_PASSWORD=${POSTGRES_PASS}
         - AUTH_ALLOWED_IPS=127.0.0.1,192.168.0.0/24
         - AUTH_ENABLED=false
         - AUTH_ALLOWED_EMAILS=your.email@example.com
         - AUTH_APPLICATION_URL=https://your-domain.com
         - SEAWEEDFS_HOST=seaweedfs
         - MEILISEARCH_HOST=meilisearch
         - MEILISEARCH_PORT=7700
         - MEILISEARCH_API_KEY=${MEILI_MASTER_KEY}
         - SPRING_DATA_MONGODB_HOST=ferretdb
   ```

3. Adjust the volumes in the `app` service to point to your e-book collection:
   ```yaml
   volumes:
     - ./plugins:/workspace/plugins
     - /path/to/your/books:/books:ro
   ```

4. Update the environment variables in the `app` service as needed:
   ```yaml
   environment:
     - SCANNER_SOURCES=/books/
     - AUTH_ALLOWED_EMAILS=your.email@example.com
     - AUTH_APPLICATION_URL=https://your-domain.com
   ```

5. Start the Docker Compose services:
   ```bash
   docker compose up -d
   ```

8. Access the application at http://localhost:8081

### Service Explanations

- **postgres**: PostgreSQL database used by FerretDB as a backend storage
- **ferretdb**: MongoDB-compatible database that uses PostgreSQL as its storage engine
- **meilisearch**: Fast search engine for indexing and searching your e-book collection
- **seaweedfs**: Distributed file system for storing e-book files and metadata
- **app**: The main OPDSKO Spring application that serves the OPDS catalog

### Alternative: Building from Source

If you prefer to build the application from source:

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/spring-pf4j-library.git
   cd spring-pf4j-library
   ```

2. Build the application and plugins:
   ```bash
   ./gradlew build
   ```

3. Start the Docker Compose services (without the app service):
   ```bash
   docker compose up -d postgres ferretdb meilisearch seaweedfs
   ```

4. Run the application:
   ```bash
   ./gradlew :web:bootRun
   ```

5. Access the application at http://localhost:8080

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

### Mongock Configuration

Mongock is used for database migrations. It's configured in the `application.yml` file:

```yaml
mongock:
  test-enabled: false
  migration-scan-package: [ "com.github.asm0dey.opdsko_spring.migrations" ]
  index-creation: true
  enabled: true
```

- `test-enabled`: Whether to run migrations during tests
- `migration-scan-package`: Package to scan for migration classes
- `index-creation`: Whether to create indexes
- `enabled`: Whether to enable Mongock migrations

## Usage

1. Configure your book sources in the `application.yml` file.
2. Start the application.
3. The application will scan your sources and index your books.
4. Access the OPDS catalog at http://localhost:8081/opds
5. Connect your e-book reader application to the OPDS catalog.

## Building a Docker Image

You can build a Docker image of the application:

```bash
./gradlew :web:bootBuildImage
```

This will create a Docker image named `asm0dey/p[ds:0.0.1`.

## GitHub Actions

The project includes GitHub Actions workflows for building and releasing the application and plugins:

### Main Workflow

The main workflow (`build.yml`) builds all modules and creates a release when a tag is pushed. It also builds and pushes a Docker image for the main application.

To trigger a release:
```bash
git tag v1.0.0
git push origin v1.0.0
```

## Recent Improvements

### Performance Enhancements

- **Parallel Processing**: Implemented parallel processing for fetching book images and descriptions using Kotlin coroutines, significantly improving page load times
- **Incremental Indexing**: Improved the book scanning process to index books incrementally as they are processed, rather than in a separate step at the end
- **Code Refactoring**: Reorganized code in several components for better maintainability and readability

### Infrastructure Updates

- **Database Migrations**: Added Mongock for MongoDB database migrations, making schema changes more reliable and manageable
- **Updated Dependencies**: Updated Docker images to latest versions:
  - FerretDB: Updated to version 2.1.0
  - PostgreSQL: Updated to version 17-0.102.0-ferretdb-2.1.0

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
