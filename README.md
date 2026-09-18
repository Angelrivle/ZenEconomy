# ZenEconomy 🪙

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Paper](https://img.shields.io/badge/Paper-1.21+-blue.svg)](https://papermc.io/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

**ZenEconomy** es un plugin modular y moderno de múltiples economías secundarias para servidores **Paper 1.21+** escrito en **Java 21**. Diseñado para trabajar en armonía con plugins de economía principal como EssentialsX o CMI sin secuestrar Vault.

---

## ✨ Características Principales
- 💎 **Múltiples Monedas Ilimitadas**: Cada moneda en su propio archivo YAML en `currencies/`.
- ⚡ **Comandos Dinámicos Dedicados**: `/crystals`, `/gems`, etc. con subcomandos `bal`, `pay`, `top`, `give`, `take`, `set`, `reset`.
- 🎨 **Adventure & MiniMessage**: Gradientes, colores HEX y estilos modernos sin ChatColor legacy.
- 💾 **Persistencia Asíncrona (HikariCP)**: Soporte out-of-the-box para **SQLite** y **MySQL / MariaDB** sin congelar el hilo principal de Paper.
- 📊 **PlaceholderAPI & Vault**: Integración completa para mostrar saldos y rankings. Vault solo se activa si marcas una moneda con `vault: true`.
- 🎒 **Menú de Billetera GUI**: Menú interactivo (`/wallet`) con iconos configurables y soporte de CustomModelData.

---

## 📚 Documentación
- [📖 Guía de Configuración Completa](Docs/DOC.md)
- [💻 Guía de la API para Desarrolladores (JitPack, Maven & Gradle)](Docs/API.md)

---

## 🚀 Instalación
1. Descarga la última versión desde [Releases](https://github.com/Angelrivle/ZenEconomy/releases).
2. Coloca el `.jar` en la carpeta `plugins/` de tu servidor Paper 1.21+.
3. Reinicia tu servidor.
