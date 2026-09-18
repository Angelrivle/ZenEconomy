package com.cuac_xd.zeneconomy.config;

import com.cuac_xd.zeneconomy.currency.CurrencyRegistry;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {

    private final JavaPlugin plugin;
    private final CurrencyRegistry currencyRegistry;
    private final MessageManager messageManager;

    public ConfigManager(JavaPlugin plugin, CurrencyRegistry currencyRegistry, MessageManager messageManager) {
        this.plugin = plugin;
        this.currencyRegistry = currencyRegistry;
        this.messageManager = messageManager;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        currencyRegistry.loadCurrencies();
        messageManager.loadMessages();
    }

    public void reload() {
        plugin.reloadConfig();
        currencyRegistry.loadCurrencies();
        messageManager.loadMessages();
    }
}
