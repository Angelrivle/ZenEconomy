# ZenEconomy - Documentación Técnica y Guía de Configuración

Bienvenido a la documentación oficial de configuración y uso de **ZenEconomy**.  
ZenEconomy es un sistema de múltiples economías modulares para servidores de Minecraft **Paper 1.21+ (Java 21)**, optimizado con persistencia asíncrona (HikariCP), Adventure API nativo (MiniMessage) y comandos dedicados dinámicos por moneda al estilo de EcoBits.

---

## 📑 Tabla de Contenidos
1. [Requisitos del Sistema](#requisitos-del-sistema)
2. [Instalación](#instalación)
3. [Estructura de Archivos](#estructura-de-archivos)
4. [Configuración Principal (`config.yml`)](#configuración-principal-configyml)
5. [Configuración de Monedas (`currencies/*.yml`)](#configuración-de-monedas-currenciesyml)
6. [Mensajes y Formatos (`messages.yml`)](#mensajes-y-formatos-messagesyml)
7. [Comandos y Permisos](#comandos-y-permisos)
8. [Integraciones (Vault & PlaceholderAPI)](#integraciones)

---

## 1. Requisitos del Sistema
- **Plataforma**: Servidor Paper, Purpur o forks modernos de 1.21 en adelante.
- **Java**: Java 21 o superior.
- **Dependencias Opcionales**:
  - `PlaceholderAPI`: Para placeholders en Scoreboards, TAB, chat y menús.
  - `Vault`: Solo requerido si configuras alguna divisa secundaria para registrarse como proveedor de economía de Vault (`vault: true`). Si usas EssentialsX o CMI como economía principal, déjalo en `vault: false`.

---

## 2. Instalación
1. Descarga el archivo compilado `ZenEconomy-1.0.0.jar`.
2. Colócalo dentro de la carpeta `plugins/` de tu servidor.
3. Inicia o reinicia el servidor.
4. Se generará la carpeta `plugins/ZenEconomy/` con sus configuraciones por defecto.

---

## 3. Estructura de Archivos
```
plugins/ZenEconomy/
├── config.yml              # Configuración general y de base de datos
├── messages.yml            # Mensajes del plugin en formato MiniMessage
├── zeneconomy.db           # Base de datos SQLite (si se usa SQLite)
└── currencies/             # Definición de cada moneda individual
    ├── crystals.yml        # Moneda de ejemplo (Cristales)
    └── gems.yml            # Moneda de ejemplo (Gemas)
```

---

## 4. Configuración Principal (`config.yml`)

```yaml
# ZenEconomy Main Configuration
# Diseñado para Paper 1.21+ (Java 21)

# Configuración del motor de almacenamiento
database:
  # Opciones disponibles: SQLITE, MYSQL, MARIADB
  type: SQLITE
  
  sqlite:
    file: "zeneconomy.db"
    
  mysql:
    host: "localhost"
    port: 3306
    database: "minecraft"
    username: "root"
    password: "password"
    ssl: false
    table-prefix: "ze_"
    
  # Opciones del Pool de conexiones HikariCP
  pool:
    maximum-pool-size: 10
    minimum-idle: 5
    max-lifetime: 1800000       # 30 minutos
    connection-timeout: 10000   # 10 segundos

# Frecuencia del auto-guardado en background de las cuentas en memoria (en segundos)
auto-save-interval: 300

# Ajustes del comando y GUI de mejores balances (Top Balances)
baltop:
  # Intervalo de actualización del caché en segundos para no sobrecargar la base de datos
  cache-update-interval: 180
  # Cantidad de jugadores mostrados por página
  page-size: 10
```

---

## 5. Configuración de Monedas (`currencies/*.yml`)

Cada archivo dentro de la carpeta `currencies/` representa una moneda completamente independiente. Puedes crear tantos archivos como desees (por ejemplo: `tokens.yml`, `souls.yml`, `coins.yml`).

### Ejemplo Explicado: `currencies/crystals.yml`

```yaml
# Identificador único de la divisa (en minúsculas)
id: crystals

# Nombre descriptivo de la divisa
name: "Crystals"

# Símbolo que identifica a la moneda
symbol: "❖"

# Prefijo que se antepone a los mensajes de esta moneda (dejar en blanco para usar el del plugin)
prefix: ""

# Saldo con el que empieza cada nuevo jugador
default: 0

# Saldo máximo que un jugador puede almacenar (-1 para sin límite)
max: -1

# Si los jugadores pueden transferirse fondos entre sí mediante /<comando> pay
payable: true

# Si se permiten cantidades decimales en los comandos
decimal: true

# Cantidad máxima de decimales permitidos en comandos y formateos
max-decimals: 2

# Si esta divisa debe registrarse en Vault (Solo una divisa puede registrarse en Vault)
# Si usas EssentialsX o CMI como dinero principal, mantén esto en false.
vault: false

# Si el comando directo /crystals <jugador> debe actuar como atajo para consultar saldo
balance-shorthand: false

# Formato largo de visualización del balance (Placeholders: %currency%, %amount%, %symbol%)
format: "<aqua>%symbol%</aqua><green>%amount%</green> <aqua>%currency%</aqua>"

# Formato corto o abreviado (Placeholders: %currency%, %amount%, %symbol%)
format-short: "<aqua>%symbol%</aqua> <green>%amount%</green>"

# Patrón de formato decimal estándar (Java DecimalFormat)
decimal-format: "#,##0.00"

# Patrón de formato decimal abreviado
decimal-format-short: "#,##0.00"

# Lista de comandos que se registrarán dinámicamente para esta divisa
commands:
  - crystals
  - ecocrystals

# Icono representativo para el menú de Billetera (/wallet)
icon:
  material: "AMETHYST_SHARD"
  custom-model-data: 0
  display-name: "<aqua><bold>Cristales</bold></aqua>"
  lore:
    - "<gray>Moneda especial obtenida en eventos y misiones."
```

---

## 6. Mensajes y Formatos (`messages.yml`)

ZenEconomy utiliza la librería nativa **Adventure MiniMessage**. Admite gradientes, colores HEX, negritas, eventos de clic (`<click:run_command:...>`) y eventos de hover (`<hover:show_text:...>`).

Variables comunes disponibles en mensajes:
- `<target>`: Nombre del jugador objetivo.
- `<sender>`: Nombre del remitente.
- `<amount>`: Monto formateado con el símbolo/estilo de la divisa.
- `<currency>`: Nombre de la divisa.
- `<balance>`: Balance actual del jugador.
- `<rank>`: Posición del jugador en el ranking de baltop.

---

## 7. Comandos y Permisos

### Comandos Dedicados por Divisa
Por cada moneda configurada en `currencies/`, se registran sus comandos asociados (por ejemplo `/crystals`, `/gems`).

| Comando | Descripción | Permiso |
| :--- | :--- | :--- |
| `/<moneda>` | Muestra tu saldo actual | `zeneconomy.currency.<id>` |
| `/<moneda> [jugador]` | Consulta el saldo de otro jugador | `zeneconomy.currency.<id>.balance.others` |
| `/<moneda> bal [jugador]` | Consulta el saldo de un jugador | `zeneconomy.currency.<id>.balance.others` |
| `/<moneda> pay <jugador> <cantidad>` | Envía saldo a otro jugador | `zeneconomy.currency.<id>` |
| `/<moneda> top [pagina]` | Muestra el ranking de jugadores más ricos en el chat | `zeneconomy.currency.<id>` |
| `/<moneda> top gui` | Abre una interfaz gráfica con las cabezas de los jugadores | `zeneconomy.currency.<id>` |
| `/<moneda> give <jugador> <cantidad>` | Acredita fondos a un jugador | `zeneconomy.currency.<id>.admin` o `zeneconomy.admin` |
| `/<moneda> take <jugador> <cantidad>` | Retira fondos a un jugador | `zeneconomy.currency.<id>.admin` o `zeneconomy.admin` |
| `/<moneda> set <jugador> <cantidad>` | Establece el saldo exacto de un jugador | `zeneconomy.currency.<id>.admin` o `zeneconomy.admin` |
| `/<moneda> reset <jugador>` | Reinicia el saldo al valor por defecto | `zeneconomy.currency.<id>.admin` o `zeneconomy.admin` |

### Comandos Generales
| Comando | Alias | Descripción | Permiso |
| :--- | :--- | :--- | :--- |
| `/wallet` | `/billetera` | Abre el menú visual con todas las monedas del jugador | `zeneconomy.command.wallet` (Default: Todos) |
| `/zeneconomy currencies` | `/ze list` | Lista todas las monedas cargadas y sus propiedades | `zeneconomy.admin` |
| `/zeneconomy reload` | `/ze reload` | Recarga configuraciones, monedas y mensajes | `zeneconomy.admin` |
| `/zeneconomy give <jugador> <cant> <moneda>` | | Acredita saldo en cualquier moneda | `zeneconomy.admin` |
| `/zeneconomy take <jugador> <cant> <moneda>` | | Retira saldo en cualquier moneda | `zeneconomy.admin` |
| `/zeneconomy set <jugador> <cant> <moneda>` | | Fija el balance en cualquier moneda | `zeneconomy.admin` |
| `/zeneconomy reset <jugador> <moneda>` | | Reinicia saldo en cualquier moneda | `zeneconomy.admin` |

---

## 8. Integraciones

### PlaceholderAPI (PAPI)
| Placeholder | Descripción | Ejemplo de Salida |
| :--- | :--- | :--- |
| `%zeneconomy_balance_<currency>%` | Balance numérico o formateado | `1,500.00` |
| `%zeneconomy_balance_formatted_<currency>%` | Balance con el formato visual de la moneda | `❖1,500.00 Crystals` |
| `%zeneconomy_balance_raw_<currency>%` | Saldo numérico puro sin separadores | `1500.0` |
| `%zeneconomy_currency_name_<currency>%` | Nombre configurado de la moneda | `Crystals` |
| `%zeneconomy_currency_symbol_<currency>%` | Símbolo de la moneda | `❖` |
| `%zeneconomy_top_name_<currency>_<pos>%` | Nombre del jugador en la posición de ranking | `cuac_xd` |
| `%zeneconomy_top_balance_<currency>_<pos>%` | Balance en bruto del puesto de ranking | `50000.0` |
| `%zeneconomy_top_balance_formatted_<currency>_<pos>%` | Balance formateado del puesto de ranking | `❖50,000.00 Crystals` |

### Vault
- Por defecto ninguna moneda sobreescribe Vault (`vault: false`), permitiendo la compatibilidad directa con EssentialsX, CMI u otros núcleos.
- Si deseas que una de las monedas sustituya la economía global de Vault, edita su archivo en `currencies/` y coloca `vault: true`.
