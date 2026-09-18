# ZenEconomy

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/)
[![Paper](https://img.shields.io/badge/Paper-1.21+-blue.svg)](https://papermc.io/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Release](https://img.shields.io/github/v/release/Angelrivle/ZenEconomy.svg)](https://github.com/Angelrivle/ZenEconomy/releases)

ZenEconomy is a modern, high-performance multi-currency economy plugin engineered for Paper 1.21+ servers using Java 21. It is designed to operate as a modular secondary economy system alongside core economy plugins (such as EssentialsX or CMI) without overriding Vault unless explicitly instructed to do so.

---

## Key Features

- **Unlimited Custom Currencies**: Each currency is fully customizable via isolated YAML configuration files located in the `currencies/` directory.
- **Dedicated Dynamic Commands**: Each currency automatically registers its own top-level commands (e.g., `/crystals`, `/gems`) with built-in subcommands (`bal`, `pay`, `top`, `give`, `take`, `set`, `reset`).
- **Adventure & MiniMessage Native**: Complete formatting support using modern Kyori Adventure components, HEX colors, and gradients without legacy ChatColor dependencies.
- **Asynchronous Persistence (HikariCP)**: High-performance connection pooling with native support for SQLite, MySQL, and MariaDB, ensuring zero main-thread locking.
- **PlaceholderAPI & Vault Integration**: Extensive placeholder support for scoreboards and tab lists. Vault registration is optional on a per-currency basis.
- **Interactive GUI System**: Visual wallet interface (`/wallet`) and top balances viewer supporting custom materials and CustomModelData.
- **Extensible Public API**: Complete synchronous and asynchronous API prepared for third-party plugin integration via JitPack.

---

## Documentation

Comprehensive documentation is available in the `Docs` directory:

- [Plugin Configuration and Usage Guide](Docs/DOC.md)
- [Developer API and JitPack Integration Guide](Docs/API.md)

---

## Installation

1. Download the latest release from the [Releases](https://github.com/Angelrivle/ZenEconomy/releases) page.
2. Place the `ZenEconomy-1.0.0.jar` into your server's `plugins/` directory.
3. Start or restart your server running Paper 1.21 or newer with Java 21.
4. Customize your currencies inside `plugins/ZenEconomy/currencies/`.

---

## Developer Integration (JitPack)

### Maven
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.Angelrivle</groupId>
        <artifactId>ZenEconomy</artifactId>
        <version>1.0.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### Gradle (Kotlin DSL)
```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.Angelrivle:ZenEconomy:1.0.0")
}
```

---

## License

This project is licensed under the terms of the MIT License.
