package com.cuac_xd.zeneconomy.api;

import com.cuac_xd.zeneconomy.currency.CurrencyModel;
import com.cuac_xd.zeneconomy.currency.CurrencyRegistry;
import com.cuac_xd.zeneconomy.database.DatabaseManager;
import com.cuac_xd.zeneconomy.database.dao.TopEntry;
import com.cuac_xd.zeneconomy.user.Account;
import com.cuac_xd.zeneconomy.user.UserManager;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * ZenEconomy Public API.
 * Proporciona métodos síncronos y asíncronos para interactuar con las economías y saldos de jugadores.
 */
public class ZenEconomyAPI {

    private static ZenEconomyAPI instance;
    private final CurrencyRegistry currencyRegistry;
    private final UserManager userManager;
    private final DatabaseManager databaseManager;

    public ZenEconomyAPI(CurrencyRegistry currencyRegistry, UserManager userManager, DatabaseManager databaseManager) {
        this.currencyRegistry = currencyRegistry;
        this.userManager = userManager;
        this.databaseManager = databaseManager;
        instance = this;
    }

    /**
     * Obtiene la instancia estática del API de ZenEconomy.
     * @return ZenEconomyAPI singleton
     */
    @NotNull
    public static ZenEconomyAPI get() {
        if (instance == null) {
            throw new IllegalStateException("ZenEconomyAPI no ha sido inicializada aún. Asegúrate de que ZenEconomy esté habilitado.");
        }
        return instance;
    }

    // ==========================================
    // MÉTODOS DE CONSULTA DE MONEDAS
    // ==========================================

    /**
     * Obtiene el modelo de una moneda por su ID.
     * @param currencyId ID de la moneda (ej. "crystals", "gems")
     * @return Optional con el modelo de la moneda
     */
    public Optional<CurrencyModel> getCurrency(@NotNull String currencyId) {
        return currencyRegistry.getCurrency(currencyId);
    }

    /**
     * Comprueba si una moneda existe y está registrada.
     * @param currencyId ID de la moneda
     * @return true si existe
     */
    public boolean currencyExists(@NotNull String currencyId) {
        return currencyRegistry.getCurrency(currencyId).isPresent();
    }

    /**
     * Obtiene todas las monedas registradas en ZenEconomy.
     * @return Colección de modelos de monedas
     */
    @NotNull
    public Collection<CurrencyModel> getCurrencies() {
        return currencyRegistry.getAllCurrencies();
    }

    /**
     * Obtiene los IDs de todas las monedas registradas.
     * @return Set de identificadores de monedas
     */
    @NotNull
    public Set<String> getCurrencyIds() {
        return currencyRegistry.getCurrencyIds();
    }

    /**
     * Obtiene la moneda vinculada a Vault si existe alguna.
     * @return Optional con la moneda configurada con vault: true
     */
    public Optional<CurrencyModel> getVaultCurrency() {
        return currencyRegistry.getVaultCurrency();
    }

    // ==========================================
    // MÉTODOS SÍNCRONOS (JUGADORES EN LÍNEA / CACHE)
    // ==========================================

    /**
     * Obtiene el balance síncrono de un jugador en línea.
     * @param player Jugador en línea
     * @param currencyId ID de la moneda
     * @return Balance actual o valor por defecto si no está cargado
     */
    public double getBalanceSync(@NotNull Player player, @NotNull String currencyId) {
        return getBalanceSync(player.getUniqueId(), currencyId);
    }

    /**
     * Obtiene el balance en memoria de un UUID si su cuenta está en caché.
     * @param uuid UUID del jugador
     * @param currencyId ID de la moneda
     * @return Balance en memoria o 0.0 si no está en caché
     */
    public double getBalanceSync(@NotNull UUID uuid, @NotNull String currencyId) {
        Account account = userManager.getCachedAccount(uuid);
        if (account != null) {
            return account.getBalance(currencyId);
        }
        return currencyRegistry.getCurrency(currencyId).map(CurrencyModel::defaultBalance).orElse(0.0);
    }

    /**
     * Comprueba si un jugador en línea tiene al menos cierta cantidad de fondos en una divisa.
     * @param player Jugador en línea
     * @param currencyId ID de la moneda
     * @param amount Cantidad a verificar
     * @return true si tiene saldo suficiente
     */
    public boolean hasSync(@NotNull Player player, @NotNull String currencyId, double amount) {
        return getBalanceSync(player.getUniqueId(), currencyId) >= amount;
    }

    /**
     * Comprueba si un UUID en caché tiene al menos cierta cantidad de fondos.
     * @param uuid UUID del jugador
     * @param currencyId ID de la moneda
     * @param amount Cantidad a verificar
     * @return true si tiene saldo suficiente
     */
    public boolean hasSync(@NotNull UUID uuid, @NotNull String currencyId, double amount) {
        return getBalanceSync(uuid, currencyId) >= amount;
    }

    // ==========================================
    // MÉTODOS ASÍNCRONOS (JUGADORES ONLINE / OFFLINE)
    // ==========================================

    /**
     * Obtiene el balance de cualquier jugador (online u offline) de forma asíncrona.
     * @param uuid UUID del jugador
     * @param currencyId ID de la moneda
     * @return CompletableFuture con el balance numérico
     */
    public CompletableFuture<Double> getBalance(@NotNull UUID uuid, @NotNull String currencyId) {
        Account cached = userManager.getCachedAccount(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached.getBalance(currencyId));
        }
        return userManager.getOrLoadAccount(uuid, null).thenApply(acc -> acc.getBalance(currencyId));
    }

    /**
     * Obtiene el balance de un OfflinePlayer.
     * @param player OfflinePlayer
     * @param currencyId ID de la moneda
     * @return CompletableFuture con el balance
     */
    public CompletableFuture<Double> getBalance(@NotNull OfflinePlayer player, @NotNull String currencyId) {
        return getBalance(player.getUniqueId(), currencyId);
    }

    /**
     * Comprueba si un jugador (online u offline) tiene saldo suficiente.
     * @param uuid UUID del jugador
     * @param currencyId ID de la moneda
     * @param amount Cantidad requerida
     * @return CompletableFuture con true si tiene saldo suficiente
     */
    public CompletableFuture<Boolean> has(@NotNull UUID uuid, @NotNull String currencyId, double amount) {
        return getBalance(uuid, currencyId).thenApply(bal -> bal >= amount);
    }

    /**
     * Deposita saldo a un jugador.
     * @param uuid UUID del jugador
     * @param currencyId ID de la moneda
     * @param amount Cantidad a depositar (debe ser > 0)
     * @return CompletableFuture con true si la transacción fue exitosa
     */
    public CompletableFuture<Boolean> deposit(@NotNull UUID uuid, @NotNull String currencyId, double amount) {
        return userManager.deposit(uuid, currencyId, amount, com.cuac_xd.zeneconomy.api.events.EconomyTransactionEvent.TransactionType.DEPOSIT);
    }

    /**
     * Retira saldo de un jugador.
     * @param uuid UUID del jugador
     * @param currencyId ID de la moneda
     * @param amount Cantidad a retirar (debe ser > 0)
     * @return CompletableFuture con true si tenía saldo suficiente y se retiró con éxito
     */
    public CompletableFuture<Boolean> withdraw(@NotNull UUID uuid, @NotNull String currencyId, double amount) {
        return userManager.withdraw(uuid, currencyId, amount, com.cuac_xd.zeneconomy.api.events.EconomyTransactionEvent.TransactionType.WITHDRAW);
    }

    /**
     * Fija el balance exacto de un jugador en una moneda.
     * @param uuid UUID del jugador
     * @param currencyId ID de la moneda
     * @param amount Nuevo balance
     * @return CompletableFuture con true si se fijó con éxito
     */
    public CompletableFuture<Boolean> setBalance(@NotNull UUID uuid, @NotNull String currencyId, double amount) {
        return userManager.setBalance(uuid, currencyId, amount);
    }

    /**
     * Restablece el balance de un jugador al valor por defecto de la moneda.
     * @param uuid UUID del jugador
     * @param currencyId ID de la moneda
     * @return CompletableFuture con true si se restableció con éxito
     */
    public CompletableFuture<Boolean> resetBalance(@NotNull UUID uuid, @NotNull String currencyId) {
        Optional<CurrencyModel> opt = getCurrency(currencyId);
        if (opt.isEmpty()) return CompletableFuture.completedFuture(false);
        return setBalance(uuid, currencyId, opt.get().defaultBalance());
    }

    /**
     * Transfiere saldo de un jugador a otro.
     * @param from UUID del emisor
     * @param to UUID del receptor
     * @param currencyId ID de la moneda
     * @param amount Cantidad a transferir
     * @return CompletableFuture con true si la transferencia se completó con éxito
     */
    public CompletableFuture<Boolean> transfer(@NotNull UUID from, @NotNull UUID to, @NotNull String currencyId, double amount) {
        return userManager.transfer(from, to, currencyId, amount);
    }

    // ==========================================
    // FORMATEO Y UTILERÍAS
    // ==========================================

    /**
     * Formatea una cantidad con el formato largo configurado para la moneda.
     * @param currencyId ID de la moneda
     * @param amount Cantidad numérica
     * @return String formateado con MiniMessage / símbolos
     */
    public String format(@NotNull String currencyId, double amount) {
        return getCurrency(currencyId)
                .map(curr -> curr.formatAmount(amount))
                .orElse(String.valueOf(amount));
    }

    /**
     * Formatea una cantidad con el formato corto o abreviado (ej. 1.5k, 2M).
     * @param currencyId ID de la moneda
     * @param amount Cantidad numérica
     * @return String formateado abreviado
     */
    public String formatShort(@NotNull String currencyId, double amount) {
        return getCurrency(currencyId)
                .map(curr -> curr.formatAmountShort(amount))
                .orElse(String.valueOf(amount));
    }

    /**
     * Devuelve el Adventure Component listo para enviar al chat o inventario.
     * @param currencyId ID de la moneda
     * @param amount Cantidad numérica
     * @return Component de Adventure
     */
    public Component formatComponent(@NotNull String currencyId, double amount) {
        return getCurrency(currencyId)
                .map(curr -> curr.formatComponent(amount))
                .orElse(Component.text(amount));
    }

    // ==========================================
    // TOP BALANCES / RANKINGS
    // ==========================================

    /**
     * Consulta el top de jugadores con mayor saldo en una moneda.
     * @param currencyId ID de la moneda
     * @param limit Cantidad máxima de puestos a consultar (ej. 10)
     * @return CompletableFuture con lista ordenada de TopEntry
     */
    public CompletableFuture<List<TopEntry>> getTopBalances(@NotNull String currencyId, int limit) {
        return databaseManager.getTopBalances(currencyId, limit);
    }
}
