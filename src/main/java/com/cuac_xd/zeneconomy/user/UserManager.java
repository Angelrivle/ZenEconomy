package com.cuac_xd.zeneconomy.user;

import com.cuac_xd.zeneconomy.api.events.EconomyTransactionEvent;
import com.cuac_xd.zeneconomy.currency.CurrencyModel;
import com.cuac_xd.zeneconomy.currency.CurrencyRegistry;
import com.cuac_xd.zeneconomy.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final CurrencyRegistry currencyRegistry;
    private final Map<UUID, Account> cachedAccounts = new ConcurrentHashMap<>();

    public UserManager(JavaPlugin plugin, DatabaseManager databaseManager, CurrencyRegistry currencyRegistry) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.currencyRegistry = currencyRegistry;
    }

    public CompletableFuture<Account> loadPlayer(UUID uuid, String username) {
        return databaseManager.loadBalances(uuid).thenApply(balances -> {
            Account account = cachedAccounts.computeIfAbsent(uuid, id -> new Account(id, username));
            account.setUsername(username);

            // Populate defaults if missing
            for (CurrencyModel model : currencyRegistry.getAllCurrencies()) {
                if (!balances.containsKey(model.id())) {
                    balances.put(model.id(), model.defaultBalance());
                    account.setDirty(true);
                }
            }
            account.loadBalancesFromDb(balances);
            return account;
        });
    }

    public Account getCachedAccount(UUID uuid) {
        return cachedAccounts.get(uuid);
    }

    public CompletableFuture<Account> getOrLoadAccount(UUID uuid, String fallbackName) {
        Account cached = cachedAccounts.get(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return databaseManager.loadBalances(uuid).thenApply(balances -> {
            Account account = new Account(uuid, fallbackName != null ? fallbackName : "Unknown");
            for (CurrencyModel model : currencyRegistry.getAllCurrencies()) {
                if (!balances.containsKey(model.id())) {
                    balances.put(model.id(), model.defaultBalance());
                }
            }
            account.loadBalancesFromDb(balances);
            cachedAccounts.put(uuid, account);
            return account;
        });
    }

    public CompletableFuture<Void> saveAccount(Account account) {
        if (!account.isDirty()) {
            return CompletableFuture.completedFuture(null);
        }
        account.setDirty(false);
        return databaseManager.saveAccount(account.getUuid(), account.getUsername(), account.getAllBalances());
    }

    public void unloadPlayer(UUID uuid) {
        Account account = cachedAccounts.remove(uuid);
        if (account != null) {
            saveAccount(account);
        }
    }

    public void saveAll() {
        for (Account account : cachedAccounts.values()) {
            saveAccount(account);
        }
    }

    public CompletableFuture<Boolean> deposit(UUID uuid, String currencyId, double amount, EconomyTransactionEvent.TransactionType type) {
        if (amount <= 0) return CompletableFuture.completedFuture(false);
        Optional<CurrencyModel> optCurr = currencyRegistry.getCurrency(currencyId);
        if (optCurr.isEmpty()) return CompletableFuture.completedFuture(false);

        CurrencyModel currency = optCurr.get();
        return getOrLoadAccount(uuid, null).thenApply(account -> {
            double current = account.getBalance(currencyId);
            double newBal = current + amount;
            if (currency.maxBalance() > 0 && newBal > currency.maxBalance()) {
                return false;
            }

            EconomyTransactionEvent event = new EconomyTransactionEvent(uuid, currencyId, amount, current, newBal, type);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return false;

            account.setBalance(currencyId, newBal);
            saveAccount(account);
            return true;
        });
    }

    public CompletableFuture<Boolean> withdraw(UUID uuid, String currencyId, double amount, EconomyTransactionEvent.TransactionType type) {
        if (amount <= 0) return CompletableFuture.completedFuture(false);
        Optional<CurrencyModel> optCurr = currencyRegistry.getCurrency(currencyId);
        if (optCurr.isEmpty()) return CompletableFuture.completedFuture(false);

        CurrencyModel currency = optCurr.get();
        return getOrLoadAccount(uuid, null).thenApply(account -> {
            double current = account.getBalance(currencyId);
            double newBal = current - amount;
            if (newBal < 0) {
                return false;
            }

            EconomyTransactionEvent event = new EconomyTransactionEvent(uuid, currencyId, amount, current, newBal, type);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return false;

            account.setBalance(currencyId, newBal);
            saveAccount(account);
            return true;
        });
    }

    public CompletableFuture<Boolean> setBalance(UUID uuid, String currencyId, double amount) {
        Optional<CurrencyModel> optCurr = currencyRegistry.getCurrency(currencyId);
        if (optCurr.isEmpty()) return CompletableFuture.completedFuture(false);

        CurrencyModel currency = optCurr.get();
        if (!currency.canStore(amount)) {
            return CompletableFuture.completedFuture(false);
        }

        return getOrLoadAccount(uuid, null).thenApply(account -> {
            double current = account.getBalance(currencyId);
            EconomyTransactionEvent event = new EconomyTransactionEvent(uuid, currencyId, amount, current, amount, EconomyTransactionEvent.TransactionType.SET);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return false;

            account.setBalance(currencyId, amount);
            saveAccount(account);
            return true;
        });
    }

    public CompletableFuture<Boolean> transfer(UUID from, UUID to, String currencyId, double amount) {
        if (amount <= 0 || from.equals(to)) return CompletableFuture.completedFuture(false);
        return withdraw(from, currencyId, amount, EconomyTransactionEvent.TransactionType.TRANSFER).thenCompose(success -> {
            if (!success) {
                return CompletableFuture.completedFuture(false);
            }
            return deposit(to, currencyId, amount, EconomyTransactionEvent.TransactionType.TRANSFER).thenApply(depSuccess -> {
                if (!depSuccess) {
                    // Refund if deposit failed
                    deposit(from, currencyId, amount, EconomyTransactionEvent.TransactionType.DEPOSIT);
                    return false;
                }
                return true;
            });
        });
    }
}
