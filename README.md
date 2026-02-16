# Telegram Bot Client

A reactive Java client for the [Telegram Bot API](https://core.telegram.org/bots/api) built on Spring WebFlux and Project Reactor.

## Features

- Reactive, non-blocking HTTP calls via Spring `WebClient`
- Long-polling support for receiving updates
- Send messages with optional parse mode and reply keyboards
- Invoice creation and pre-checkout query handling
- Jackson-based serialization for all Telegram API types

## Requirements

- Java 25+
- Gradle 9.1+

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("dev.alimov.telegram-bot-client:telegram-bot-client:1.0.0")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'dev.alimov.telegram-bot-client:telegram-bot-client:1.0.0'
}
```

### Maven

```xml
<dependency>
    <groupId>dev.alimov.telegram-bot-client</groupId>
    <artifactId>telegram-bot-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

```java
import dev.alimov.telegram.api.TelegramBotClient;

// Create a client with your bot token
TelegramBotClient client = new TelegramBotClient("YOUR_BOT_TOKEN");

// Send a message
client.sendMessage(chatId, "Hello from Telegram Bot Client!", null, null)
      .subscribe(response -> System.out.println("Sent: " + response.getResult().messageId()));

// Poll for updates
client.getUpdates(0, 100, 30, List.of("message"))
      .subscribe(update -> System.out.println("Received: " + update.message().text()));
```

## API Overview

### Creating a Client

```java
// Simple — uses default ObjectMapper and WebClient
TelegramBotClient client = new TelegramBotClient("BOT_TOKEN");

// Custom ObjectMapper and WebClient
ObjectMapper mapper = new ObjectMapper();
WebClient webClient = WebClient.builder().build();
TelegramBotClient client = new TelegramBotClient("BOT_TOKEN", mapper, webClient);

// Fully custom — override the API endpoint (useful for testing)
TelegramBotClient client = new TelegramBotClient("http://localhost:8080", "BOT_TOKEN", mapper, webClient);
```

### Polling for Updates

```java
client.getUpdates(offset, limit, timeout, List.of("message", "callback_query"))
      .subscribe(update -> {
          if (update.message() != null) {
              System.out.println(update.message().text());
          }
      });
```

### Sending Messages

```java
// Plain text
client.sendMessage(chatId, "Hello!", null, null);

// With HTML parse mode
client.sendMessage(chatId, "<b>Bold text</b>", ParseMode.HTML, null);

// With a reply keyboard
ReplyKeyboardMarkup keyboard = ReplyKeyboardMarkup.builder()
    .keyboard(List.of(List.of(
        new KeyboardButton("Option A", null, null),
        new KeyboardButton("Option B", null, null)
    )))
    .build();

client.sendMessage(chatId, "Choose:", null, keyboard);
```

### Invoices and Payments

```java
// Send an invoice
client.setInvoice(chatId, "Product", "Description", "payload", 1000, "USD");

// Answer a pre-checkout query
client.answerPreCheckoutQuery(queryId, true, "");
```

## Building from Source

```bash
git clone https://github.com/fnklabs/telegram.git
cd telegram
./gradlew build
```

## Running Tests

```bash
./gradlew test
```

## Publishing

Artifacts are published to Maven Central using the [vanniktech maven-publish](https://github.com/vanniktech/gradle-maven-publish-plugin) plugin with automatic release enabled.

Configure the following Gradle properties (in `~/.gradle/gradle.properties` or via environment variables):

| Gradle Property         | Environment Variable        | Description                   |
|-------------------------|-----------------------------|-------------------------------|
| `mavenCentralUsername`  | `ORG_GRADLE_PROJECT_mavenCentralUsername` | Maven Central (Sonatype) username |
| `mavenCentralPassword`  | `ORG_GRADLE_PROJECT_mavenCentralPassword` | Maven Central (Sonatype) password |
| `signing.keyId`         | `ORG_GRADLE_PROJECT_signing.keyId`        | GPG key ID (last 8 chars)     |
| `signing.password`      | `ORG_GRADLE_PROJECT_signing.password`     | GPG key passphrase            |
| `signing.secretKeyRingFile` | `ORG_GRADLE_PROJECT_signing.secretKeyRingFile` | Path to GPG keyring file |

```bash
./gradlew publishAllPublicationsToMavenCentralRepository
```

Staging and release are handled automatically — once the upload completes, the plugin closes and releases the staging repository.

### GPG Key Setup

A GPG key is required to sign artifacts for Maven Central.

#### 1. Install GPG

**macOS:**

```bash
brew install gnupg
```

**Linux (Debian/Ubuntu):**

```bash
sudo apt-get install gnupg
```

**Linux (Fedora/RHEL):**

```bash
sudo dnf install gnupg2
```

#### 2. Generate a Key

```bash
gpg --full-generate-key
```

When prompted:
- Key type: `RSA and RSA`
- Key size: `4096`
- Expiration: `0` (no expiration) or your preferred duration
- Enter your name, email, and a passphrase

#### 3. Find Your Key ID

```bash
gpg --list-keys --keyid-format short
```

Output will look like:

```
pub   rsa4096/AABBCCDD 2025-01-01 [SC]
      1234567890ABCDEF1234567890AABBCCDD
uid           [ultimate] Your Name <your@email.com>
```

The key ID is the 8-character value after `rsa4096/` — in this example, `AABBCCDD`.

#### 4. Export the Secret Keyring

```bash
gpg --export-secret-keys -o ~/.gradle/secring.gpg
```

#### 5. Publish Your Public Key to a Keyserver

Maven Central verifies signatures against public keyservers:

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys AABBCCDD
```

#### 6. Configure `~/.gradle/gradle.properties`

```properties
mavenCentralUsername=your-sonatype-username
mavenCentralPassword=your-sonatype-password

signing.keyId=AABBCCDD
signing.password=your-gpg-passphrase
signing.secretKeyRingFile=/Users/yourname/.gradle/secring.gpg
```

Replace `AABBCCDD` with your key ID, the password with your GPG passphrase, and the path with the absolute path to your exported keyring.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the [Apache License 2.0](LICENSE).
