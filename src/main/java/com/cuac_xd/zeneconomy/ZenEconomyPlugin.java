package com.cuac_xd.zeneconomy;

import com.cuac_xd.zeneconomy.api.ZenEconomyAPI;
import com.cuac_xd.zeneconomy.commands.*;
import com.cuac_xd.zeneconomy.config.ConfigManager;
import com.cuac_xd.zeneconomy.config.MessageManager;
import com.cuac_xd.zeneconomy.currency.CurrencyModel;
import com.cuac_xd.zeneconomy.currency.CurrencyRegistry;
import com.cuac_xd.zeneconomy.database.DatabaseManager;
import com.cuac_xd.zeneconomy.gui.TopBalancesGUI;
import com.cuac_xd.zeneconomy.gui.WalletGUI;
import com.cuac_xd.zeneconomy.listeners.PlayerConnectionListener;
import com.cuac_xd.zeneconomy.placeholders.ZenEconomyExpansion;
import com.cuac_xd.zeneconomy.user.UserManager;
import com.cuac_xd.zeneconomy.vault.VaultHook;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class ZenEconomyPlugin extends JavaPlugin {

    private CurrencyRegistry currencyRegistry;
    private MessageManager messageManager;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private UserManager userManager;
    private VaultHook vaultHook;
    private ZenEconomyExpansion placeholderExpansion;
    private TopBalancesGUI topBalancesGUI;

    private int autoSaveTaskId = -1;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();

        this.currencyRegistry = new CurrencyRegistry(this);
        this.messageManager = new MessageManager(this);
        this.configManager = new ConfigManager(this, currencyRegistry, messageManager);

        // Cargar configs y monedas
        this.configManager.load();

        // Inicializar persistencia
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.initialize();

        // Inicializar administradores de usuario y API
        this.userManager = new UserManager(this, databaseManager, currencyRegistry);
        new ZenEconomyAPI(currencyRegistry, userManager, databaseManager);

        // GUI Listeners
        WalletGUI walletGUI = new WalletGUI(userManager, currencyRegistry);
        this.topBalancesGUI = new TopBalancesGUI(this, databaseManager);
        Bukkit.getPluginManager().registerEvents(walletGUI, this);
        Bukkit.getPluginManager().registerEvents(topBalancesGUI, this);

        // Listener de conexión
        Bukkit.getPluginManager().registerEvents(new PlayerConnectionListener(userManager), this);

        // Registrar Comandos Principales
        ZenEconomyAdminCommand adminCmd = new ZenEconomyAdminCommand(userManager, currencyRegistry, messageManager, databaseManager, configManager);
        getCommand("zeneconomy").setExecutor(adminCmd);
        getCommand("zeneconomy").setTabCompleter(adminCmd);

        WalletCommand walletCmd = new WalletCommand(walletGUI, messageManager);
        getCommand("wallet").setExecutor(walletCmd);

        // Registrar comandos dedicados por cada moneda dinámicamente
        registerDedicatedCurrencyCommands();

        // Vault Hook (solo si una moneda tiene vault: true)
        if (currencyRegistry.getVaultCurrency().isPresent()) {
            if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
                this.vaultHook = new VaultHook(this, userManager, currencyRegistry);
                this.vaultHook.hook();
            } else {
                getLogger().info("Vault no detectado en el servidor.");
            }
        } else {
            getLogger().info("Ninguna moneda secundaria está marcada con 'vault: true'. Vault no se sobreescribe (conservando Essentials/CMI como economía principal).");
        }

        // PlaceholderAPI Hook
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            this.placeholderExpansion = new ZenEconomyExpansion(this, userManager, currencyRegistry, databaseManager);
            this.placeholderExpansion.register();
            getLogger().info("PlaceholderAPI detectado: expansión registrada.");
        }

        // Tarea de auto-guardado en background
        int autoSaveSecs = getConfig().getInt("auto-save-interval", 300);
        this.autoSaveTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            userManager.saveAll();
        }, autoSaveSecs * 20L, autoSaveSecs * 20L).getTaskId();

        long diff = System.currentTimeMillis() - start;
        getLogger().info("ZenEconomy habilitado exitosamente en " + diff + "ms!");
    }

    public void registerDedicatedCurrencyCommands() {
        for (CurrencyModel model : currencyRegistry.getAllCurrencies()) {
            if (model.commands() != null && !model.commands().isEmpty()) {
                String primaryCmd = model.commands().get(0);
                List<String> aliases = model.commands().size() > 1 ? model.commands().subList(1, model.commands().size()) : List.of();
                CurrencyDedicatedCommand dedicatedExecutor = new CurrencyDedicatedCommand(this, model, userManager, databaseManager, messageManager, topBalancesGUI);
                DynamicCommandRegistrar.register(this, primaryCmd, aliases, dedicatedExecutor, dedicatedExecutor);
                getLogger().info("Registrado comando dedicado para moneda '" + model.id() + "': /" + primaryCmd + " (alias: " + aliases + ")");
            }
        }
    }

    @Override
    public void onDisable() {
        if (autoSaveTaskId != -1) {
            Bukkit.getScheduler().cancelTask(autoSaveTaskId);
        }

        if (vaultHook != null) {
            vaultHook.unhook();
        }

        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
        }

        if (userManager != null) {
            userManager.saveAll();
        }

        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("ZenEconomy deshabilitado y datos salvaguardados.");
    }

    public CurrencyRegistry getCurrencyRegistry() {
        return currencyRegistry;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
