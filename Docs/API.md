# ZenEconomy Developer API Guide

Esta guía explica detalladamente cómo integrar la API de **ZenEconomy** en tus propios plugins de Spigot/Paper utilizando **Maven**, **Gradle** y **JitPack**.

---

## 📦 Dependencia (Maven & Gradle con JitPack)

Dado que el código fuente estará alojado en GitHub, puedes importarlo directamente mediante [JitPack](https://jitpack.io).

### Maven
Añade el repositorio de JitPack a tu bloque `<repositories>`:
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Añade la dependencia de ZenEconomy a tu bloque `<dependencies>`:
```xml
<dependency>
    <groupId>com.github.Angelrivle</groupId>
    <artifactId>ZenEconomy</artifactId>
    <version>1.0.0</version> <!-- O el tag/commit de tu release -->
    <scope>provided</scope>
</dependency>
```

---

### Gradle (Kotlin DSL - `build.gradle.kts`)
```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.Angelrivle:ZenEconomy:1.0.0")
}
```

### Gradle (Groovy DSL - `build.gradle`)
```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.Angelrivle:ZenEconomy:1.0.0'
}
```

---

## 🔌 Configurar `plugin.yml` o `paper-plugin.yml`

Añade ZenEconomy como dependencia blanda (`softdepend`) o dependencia estricta (`depend`) en el `plugin.yml` de tu plugin:

```yaml
name: MiPlugin
version: 1.0.0
main: com.miusuario.miplugin.MiPlugin
depend: [ZenEconomy] # o softdepend si ZenEconomy es opcional
```

---

## 💻 Ejemplos de Uso de la API

### 1. Obtener la Instancia de la API
```java
import com.cuac_xd.zeneconomy.api.ZenEconomyAPI;

ZenEconomyAPI api = ZenEconomyAPI.get();
```

---

### 2. Comprobar y Obtener Monedas
```java
// Comprobar si una divisa está registrada en el servidor
if (api.currencyExists("crystals")) {
    // Obtener modelo de la divisa
    api.getCurrency("crystals").ifPresent(currency -> {
        getLogger().info("Moneda: " + currency.name());
        getLogger().info("Símbolo: " + currency.symbol());
        getLogger().info("¿Permite decimales?: " + currency.decimal());
    });
}
```

---

### 3. Operaciones Síncronas (Rápidas en Memoria / Jugadores Online)
Si el jugador está conectado, su saldo está cargado en memoria de alta velocidad:

```java
Player player = ...;

// Consultar saldo síncrono
double balance = api.getBalanceSync(player, "crystals");

// Verificar si el jugador tiene suficiente saldo
if (api.hasSync(player, "crystals", 50.0)) {
    player.sendMessage("¡Tienes fondos suficientes!");
}
```

---

### 4. Operaciones Asíncronas (Recomendadas para Cualquier Jugador / Base de Datos)
Para jugadores que puedan estar desconectados o para transacciones seguras sin congelar el hilo principal:

#### Consultar saldo:
```java
UUID playerUuid = player.getUniqueId();

api.getBalance(playerUuid, "crystals").thenAccept(balance -> {
    player.sendMessage("Tu balance es: " + balance);
});
```

#### Depositar fondos:
```java
api.deposit(playerUuid, "crystals", 100.0).thenAccept(success -> {
    if (success) {
        player.sendMessage("¡Se han depositado 100 cristales en tu cuenta!");
    } else {
        player.sendMessage("No se pudo completar el depósito (límite máximo alcanzado o cantidad inválida).");
    }
});
```

#### Retirar fondos / Cobrar:
```java
api.withdraw(playerUuid, "crystals", 25.0).thenAccept(success -> {
    if (success) {
        player.sendMessage("¡Compra realizada! Se han retirado 25 cristales.");
    } else {
        player.sendMessage("Fondos insuficientes para realizar esta compra.");
    }
});
```

#### Transferir entre jugadores:
```java
UUID remitente = player1.getUniqueId();
UUID receptor = player2.getUniqueId();

api.transfer(remitente, receptor, "crystals", 50.0).thenAccept(success -> {
    if (success) {
        player1.sendMessage("Enviaste 50 cristales con éxito.");
    } else {
        player1.sendMessage("Transferencia fallida.");
    }
});
```

---

### 5. Formateo y Adventure Components
Puedes aprovechar los patrones configurados en `currencies/*.yml` para formatear montos en texto o componentes de Adventure listos para enviar al jugador:

```java
// Formato largo en texto: ej. "❖1,500.00 Crystals"
String formattedText = api.format("crystals", 1500.0);

// Formato corto abreviado: ej. "❖1.5k"
String formattedShort = api.formatShort("crystals", 1500.0);

// Componente Adventure nativo con colores/gradientes:
net.kyori.adventure.text.Component component = api.formatComponent("crystals", 1500.0);
player.sendMessage(component);
```

---

### 6. Consultar Top Balances (Rankings)
```java
// Obtener los 10 mejores balances de una moneda
api.getTopBalances("crystals", 10).thenAccept(entries -> {
    for (TopEntry entry : entries) {
        getLogger().info("#" + entry.rank() + " " + entry.username() + " - " + entry.balance());
    }
});
```

---

### 7. Escuchar Eventos de Transacciones
ZenEconomy dispara el evento `EconomyTransactionEvent` en cada depósito, retiro o transferencia:

```java
import com.cuac_xd.zeneconomy.api.events.EconomyTransactionEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class TransactionListener implements Listener {

    @EventHandler
    public void onTransaction(EconomyTransactionEvent event) {
        UUID uuid = event.getPlayerUuid();
        String currency = event.getCurrencyId();
        double amount = event.getAmount();
        EconomyTransactionEvent.TransactionType type = event.getTransactionType();

        // Puedes cancelar la transacción si lo deseas:
        // event.setCancelled(true);
    }
}
```
