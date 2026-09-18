package com.cuac_xd.zeneconomy.placeholders;

import com.cuac_xd.zeneconomy.currency.CurrencyModel;
import com.cuac_xd.zeneconomy.currency.CurrencyRegistry;
import com.cuac_xd.zeneconomy.currency.NumberFormatter;
import com.cuac_xd.zeneconomy.database.DatabaseManager;
import com.cuac_xd.zeneconomy.database.dao.TopEntry;
import com.cuac_xd.zeneconomy.user.Account;
import com.cuac_xd.zeneconomy.user.UserManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ZenEconomyExpansion extends PlaceholderExpansion {

    private final Plugin plugin;
    private final UserManager userManager;
    private final CurrencyRegistry currencyRegistry;
    private final DatabaseManager databaseManager;

    // Cache top balances for placeholders: currencyId -> list
    private final Map<String, List<TopEntry>> topCache = new ConcurrentHashMap<>();
    private long lastTopUpdate = 0L;

    public ZenEconomyExpansion(Plugin plugin, UserManager userManager, CurrencyRegistry currencyRegistry, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.userManager = userManager;
        this.currencyRegistry = currencyRegistry;
        this.databaseManager = databaseManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "zeneconomy";
    }

    @Override
    public @NotNull String getAuthor() {
        return "cuac_xd";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        // Formatos soportados:
        // %zeneconomy_balance_<currency>%
        // %zeneconomy_balance_formatted_<currency>%
        // %zeneconomy_balance_raw_<currency>%
        // %zeneconomy_currency_name_<currency>%
        // %zeneconomy_currency_symbol_<currency>%
        // %zeneconomy_top_name_<currency>_<pos>%
        // %zeneconomy_top_balance_<currency>_<pos>%
        // %zeneconomy_top_balance_formatted_<currency>_<pos>%

        String[] parts = params.toLowerCase().split("_");
        if (parts.length == 0) return null;

        updateTopCacheIfNeeded();

        if (parts[0].equals("top")) {
            // top_name_money_1
            // top_balance_money_1
            // top_balance_formatted_money_1
            if (parts.length >= 4) {
                String type = parts[1];
                boolean formatted = false;
                String currencyId;
                int rank;

                if (type.equals("balance") && parts[2].equals("formatted") && parts.length >= 5) {
                    formatted = true;
                    currencyId = parts[3];
                    try {
                        rank = Integer.parseInt(parts[4]);
                    } catch (NumberFormatException e) {
                        return "";
                    }
                } else {
                    currencyId = parts[2];
                    try {
                        rank = Integer.parseInt(parts[3]);
                    } catch (NumberFormatException e) {
                        return "";
                    }
                }

                List<TopEntry> entries = topCache.getOrDefault(currencyId, Collections.emptyList());
                int index = rank - 1;
                if (index < 0 || index >= entries.size()) {
                    return type.equals("name") ? "---" : "0";
                }

                TopEntry entry = entries.get(index);
                if (type.equals("name")) {
                    return entry.username();
                } else if (type.equals("balance")) {
                    Optional<CurrencyModel> optCurr = currencyRegistry.getCurrency(currencyId);
                    if (formatted && optCurr.isPresent()) {
                        return optCurr.get().formatAmount(entry.balance());
                    }
                    return String.valueOf(entry.balance());
                }
            }
        }

        if (parts[0].equals("currency")) {
            if (parts.length >= 3) {
                String sub = parts[1];
                String currencyId = parts[2];
                Optional<CurrencyModel> optCurr = currencyRegistry.getCurrency(currencyId);
                if (optCurr.isEmpty()) return "";
                CurrencyModel curr = optCurr.get();
                if (sub.equals("name")) return curr.name();
                if (sub.equals("symbol")) return curr.symbol();
            }
        }

        if (parts[0].equals("balance") && player != null) {
            String currencyId = null;
            boolean formatted = false;
            boolean raw = false;

            if (parts.length == 2) {
                // balance_<currency>
                currencyId = parts[1];
                formatted = true;
            } else if (parts.length == 3) {
                // balance_formatted_<currency> o balance_raw_<currency>
                if (parts[1].equals("formatted")) {
                    formatted = true;
                } else if (parts[1].equals("raw")) {
                    raw = true;
                }
                currencyId = parts[2];
            }

            if (currencyId == null) return "0";

            Optional<CurrencyModel> optCurr = currencyRegistry.getCurrency(currencyId);
            if (optCurr.isEmpty()) return "0";

            CurrencyModel curr = optCurr.get();
            Account account = userManager.getCachedAccount(player.getUniqueId());
            double balance = account != null ? account.getBalance(currencyId) : curr.defaultBalance();

            if (raw) {
                return String.valueOf(balance);
            }
            if (formatted) {
                return curr.formatAmount(balance);
            }
            return String.valueOf(balance);
        }

        return null;
    }

    private void updateTopCacheIfNeeded() {
        long now = System.currentTimeMillis();
        long interval = plugin.getConfig().getLong("baltop.cache-update-interval", 180) * 1000L;
        if (now - lastTopUpdate > interval) {
            lastTopUpdate = now;
            for (CurrencyModel model : currencyRegistry.getAllCurrencies()) {
                databaseManager.getTopBalances(model.id(), 10).thenAccept(entries -> {
                    topCache.put(model.id(), entries);
                });
            }
        }
    }
}
