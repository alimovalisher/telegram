# Contributing to Telegram Bot Client

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

## Code Guidelines

- Follow existing code style and formatting conventions in the project.
- Use meaningful names for classes, methods, and variables.
- Add Javadoc comments for all public APIs.
- Keep methods focused and concise.
- Prefer immutable data structures (records) for DTOs.

## Testing

All changes must include appropriate tests.

- Write unit tests using **JUnit 5** and **MockServer**.
- Use **Reactor StepVerifier** to test reactive streams.
- Run the full test suite before submitting:
  ```bash
  ./gradlew test
  ```

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
