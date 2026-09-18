package com.cuac_xd.zeneconomy.currency;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class CurrencyRegistry {

    private final JavaPlugin plugin;
    private final Map<String, CurrencyModel> currencies = new ConcurrentHashMap<>();
    private final Map<String, CurrencyModel> commandToCurrency = new ConcurrentHashMap<>();

    public CurrencyRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadCurrencies() {
        currencies.clear();
        commandToCurrency.clear();

        File currenciesDir = new File(plugin.getDataFolder(), "currencies");
        if (!currenciesDir.exists()) {
            currenciesDir.mkdirs();
            saveDefaultCurrencyResource("crystals.yml");
            saveDefaultCurrencyResource("gems.yml");
        }

        File[] files = currenciesDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                    String fileNameNoExt = file.getName().substring(0, file.getName().lastIndexOf('.'));
                    String id = config.getString("id", fileNameNoExt).toLowerCase();
                    String name = config.getString("name", id);
                    String symbol = config.getString("symbol", "❖");
                    String prefix = config.getString("prefix", "");
                    double defaultBal = config.getDouble("default", 0.0);
                    double max = config.getDouble("max", -1.0);
                    boolean payable = config.getBoolean("payable", true);
                    boolean decimal = config.getBoolean("decimal", true);
                    int maxDecimals = config.getInt("max-decimals", 2);
                    boolean vault = config.getBoolean("vault", false);
                    boolean balanceShorthand = config.getBoolean("balance-shorthand", false);
                    String format = config.getString("format", "%symbol%%amount% %currency%");
                    String formatShort = config.getString("format-short", "%symbol% %amount%");
                    String decimalFormat = config.getString("decimal-format", "#,##0.00");
                    String decimalFormatShort = config.getString("decimal-format-short", "#,##0.00");
                    List<String> commands = config.getStringList("commands");
                    if (commands.isEmpty()) {
                        commands = List.of(id);
                    }

                    String matName = config.getString("icon.material", "EMERALD");
                    Material material = Material.matchMaterial(matName);
                    if (material == null) material = Material.EMERALD;
                    int customModelData = config.getInt("icon.custom-model-data", 0);
                    String iconDisplayName = config.getString("icon.display-name", "<aqua>" + name + "</aqua>");
                    List<String> iconLore = config.getStringList("icon.lore");

                    CurrencyModel model = new CurrencyModel(
                            id, name, symbol, prefix, defaultBal, max, payable,
                            decimal, maxDecimals, vault, balanceShorthand,
                            format, formatShort, decimalFormat, decimalFormatShort,
                            commands, material, customModelData, iconDisplayName, iconLore
                    );

                    currencies.put(id, model);

                    for (String cmd : commands) {
                        commandToCurrency.put(cmd.toLowerCase(), model);
                    }

                    plugin.getLogger().info("Moneda cargada: " + id + " con comandos: " + commands);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Error al cargar moneda desde archivo: " + file.getName(), e);
                }
            }
        }
    }

    private void saveDefaultCurrencyResource(String resourceName) {
        File target = new File(plugin.getDataFolder(), "currencies/" + resourceName);
        if (!target.exists()) {
            try (InputStream in = plugin.getResource("currencies/" + resourceName)) {
                if (in != null) {
                    Files.copy(in, target.toPath());
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "No se pudo extraer recurso por defecto: " + resourceName, e);
            }
        }
    }

    public Optional<CurrencyModel> getCurrency(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(currencies.get(id.toLowerCase()));
    }

    public Optional<CurrencyModel> getCurrencyByCommand(String command) {
        if (command == null) return Optional.empty();
        return Optional.ofNullable(commandToCurrency.get(command.toLowerCase()));
    }

    public Optional<CurrencyModel> getVaultCurrency() {
        return currencies.values().stream().filter(CurrencyModel::vault).findFirst();
    }

    public Collection<CurrencyModel> getAllCurrencies() {
        return Collections.unmodifiableCollection(currencies.values());
    }

    public Set<String> getCurrencyIds() {
        return Collections.unmodifiableSet(currencies.keySet());
    }
}
