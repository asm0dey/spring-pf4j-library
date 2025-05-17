# Development Guidelines for Spring-PF4J-Library

This document provides essential information for developers working on this project.

## Build/Configuration Instructions

### Prerequisites
- JDK 17 or higher
- Gradle 8.x (or use the included Gradle wrapper)
- Docker and Docker Compose (for development environment)

### Building the Project
1. Clone the repository
2. Build the project using Gradle:
   ```bash
   ./gradlew build
   ```

3. To build only specific modules:
   ```bash
   ./gradlew :common:build
   ./gradlew :web:build
   ```

### Plugin System
This project uses PF4J (Plugin Framework for Java) for plugin support. Plugins are built as separate modules and packaged as JAR files.

1. Build all plugins:
   ```bash
   ./gradlew copyPlugins
   ```
   This task builds the plugin JARs and copies them to the `plugins` directory.

2. The main application (`web` module) automatically loads plugins from the `plugins` directory at startup.

### Docker Compose Setup
The project includes a Docker Compose configuration for development:

1. Start the development environment:
   ```bash
   docker-compose up -d
   ```

2. This will start the required services:
   - MongoDB (database)
   - Meilisearch (search engine)
   - SeaweedFS (file storage)

## Testing Information

### Running Tests
1. Run all tests:
   ```bash
   ./gradlew test
   ```

2. Run tests for a specific module:
   ```bash
   ./gradlew :common:test
   ./gradlew :web:test
   ```

3. Run a specific test class:
   ```bash
   ./gradlew :common:test --tests "com.github.asm0dey.opdsko.common.AuthorTest"
   ```

### Adding New Tests
1. Tests should be placed in the `src/test/kotlin` directory of the respective module.
2. Use JUnit 5 for testing.
3. For Kotlin tests, use the `kotlin.test` package for assertions.
4. Name test classes with a `Test` suffix.
5. Use backtick-enclosed method names for better readability.

### Example Test
Here's a simple test for the `Author` class in the `common` module:

```kotlin
// Imports needed: org.junit.jupiter.api.Test, kotlin.test.assertEquals
class AuthorTest {
    @Test
    fun `computeFullName with lastName and firstName`() {
        val author = AuthorAdapter(lastName = "Doe", firstName = "John")
        assertEquals("Doe, John", author.computeFullName())
    }
}
```

### Integration Tests
For integration tests that require external services:
1. Use Testcontainers to spin up required services (MongoDB, Meilisearch, SeaweedFS).
2. See `OpdskoSpringApplicationTests.kt` for an example of using Testcontainers.

## Additional Development Information

### Project Structure
- `common`: Core interfaces and classes used by other modules
- `web`: Main Spring Boot application
- `epub-support`, `fb2-support`, etc.: Plugin modules for different file formats
- `fb2-to-epub-converter`: Plugin for converting FB2 files to EPUB format
- `seaweedfs-spring`, `spring-meilisearch`: Integration modules for external services

### Custom Spring Integration Services

#### SeaweedFS Spring Integration

The `seaweedfs-spring` module provides Spring Boot auto-configuration for SeaweedFS, a distributed object storage system. This integration simplifies the use of SeaweedFS in Spring Boot applications.

**Features:**
- Auto-configuration of SeaweedFS client
- Spring Boot Docker Compose integration for development environments
- Property-based configuration

**Configuration Properties:**
```properties
# SeaweedFS configuration
seaweedfs.host=localhost
seaweedfs.port=9333
seaweedfs.filer-port=8888
seaweedfs.filer-grpc-port=18888
```

**Usage:**
1. Add the `seaweedfs-spring` module as a dependency
2. Add the SeaweedFS client library dependency
3. Configure the properties in your `application.properties` or `application.yml`
4. Inject the `FilerClient` bean where needed:
   ```kotlin
   @Autowired
   private lateinit var filerClient: FilerClient
   ```

#### Meilisearch Spring Integration

The `spring-meilisearch` module provides Spring Boot auto-configuration for Meilisearch, a powerful, fast, and open-source search engine.

**Features:**
- Auto-configuration of Meilisearch client
- Spring Boot Docker Compose integration for development environments
- Property-based configuration

**Configuration Properties:**
```properties
# Meilisearch configuration
meilisearch.host=localhost
meilisearch.port=7700
meilisearch.api-key=masterKey
```

**Usage:**
1. Add the `spring-meilisearch` module as a dependency
2. Add the Meilisearch client library dependency
3. Configure the properties in your `application.properties` or `application.yml`
4. Inject the `Client` bean where needed:
   ```kotlin
   @Autowired
   private lateinit var meilisearchClient: com.meilisearch.sdk.Client
   ```

### Plugin Development
To create a new plugin:
1. Create a new module in the project
2. Add PF4J dependencies
3. Implement the appropriate extension point interface (e.g., `BookHandler`, `FormatConverter`)
4. Configure the plugin manifest in the build.gradle.kts file:
   ```kotlin
   manifest {
       attributes(
           "Plugin-Id" to "your-plugin-id",
           "Plugin-Version" to version,
           "Plugin-Provider" to "your-name",
           "Plugin-Class" to "com.your.package.YourPluginClass",
           "Plugin-Dependencies" to "optional-dependency-plugin-id"
       )
   }
   ```

### Code Style
- Follow Kotlin coding conventions
- Use meaningful names for classes, methods, and variables
- Write comprehensive tests for new functionality
- Document public APIs with KDoc comments

### Debugging
- For plugin issues, check the application logs for PF4J-related messages
- For Docker-related issues, use `docker-compose logs` to view container logs
- The application uses Spring Boot's logging configuration, which can be adjusted in `application.properties` or `application.yml`
