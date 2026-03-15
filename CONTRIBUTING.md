# Contributing to Telegram Bot Framework

Thank you for your interest in contributing! This guide explains how to get involved.

## Getting Started

1. Fork the repository and clone your fork:
   ```bash
   git clone git@github.com:<your-username>/telegram.git
   cd telegram
   ```

2. Create a feature branch from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. Build the project:
   ```bash
   ./gradlew build
   ```

## Development Requirements

- **Java 25** or later
- **Gradle 9.1+** (wrapper included)
- **Docker** (for Pulsar integration tests)

## Project Structure

```
telegram-bot-client/        # Reactive HTTP client for Telegram Bot API
telegram-bot-core/          # Shared interfaces (ReactiveQueue, UpdateHandler, BotResponse)
telegram-bot-poller/        # Update polling and response dispatch
telegram-bot-worker/        # Update processing pipeline
telegram-bot-queue-pulsar/  # Apache Pulsar queue implementation
```

## Code Guidelines

- Follow existing code style and formatting conventions in the project.
- Use meaningful names for classes, methods, and variables.
- Add Javadoc comments for all public APIs.
- Keep methods focused and concise.
- Prefer immutable data structures (records) for DTOs.

## Testing

All changes must include appropriate tests.

### Unit Tests

- Write unit tests using **JUnit 5** and **Mockito** (for interfaces) or **MockServer** (for HTTP clients).
- Use **Reactor StepVerifier** to test reactive streams.
- Run unit tests:
  ```bash
  ./gradlew test -x telegram-bot-queue-pulsar:test
  ```

### Integration Tests

The `telegram-bot-queue-pulsar` module uses **Testcontainers** to run Apache Pulsar in Docker during tests.

- Start Pulsar manually for development:
  ```bash
  docker compose up -d
  ```

- Run all tests including integration:
  ```bash
  ./gradlew test
  ```

- Stop Pulsar:
  ```bash
  docker compose down
  ```

### Test Conventions

- Place unit tests alongside the module they test.
- Name integration tests with the `IT` suffix (e.g., `PulsarReactiveQueueIT`).
- Use `StepVerifier` for all reactive stream assertions.
- Mock interfaces with Mockito; use MockServer for HTTP client tests.

## Submitting Changes

1. Ensure all tests pass locally.
2. Commit your changes with a clear, descriptive message:
   ```bash
   git commit -m "Add support for editing messages"
   ```
3. Push your branch and open a Pull Request against `main`.
4. Describe what the PR does and link any related issues.

## Pull Request Guidelines

- Keep PRs focused on a single change or feature.
- Include tests for new functionality.
- Update documentation if public APIs change.
- Ensure the build passes in CI before requesting review.

## Reporting Issues

- Use the GitHub issue tracker to report bugs or request features.
- Include steps to reproduce for bug reports.
- Provide the Java version, OS, and library version you are using.

## License

By contributing, you agree that your contributions will be licensed under the [Apache License 2.0](LICENSE).
