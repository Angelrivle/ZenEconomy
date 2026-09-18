package com.cuac_xd.zeneconomy.gui;

import com.cuac_xd.zeneconomy.currency.CurrencyModel;
import com.cuac_xd.zeneconomy.database.DatabaseManager;
import com.cuac_xd.zeneconomy.database.dao.TopEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TopBalancesGUI implements InventoryHolder, Listener {

    private final Plugin plugin;
    private final DatabaseManager databaseManager;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public TopBalancesGUI(Plugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public void open(Player player, CurrencyModel currency) {
        databaseManager.getTopBalances(currency.id(), 10).thenAccept(entries -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Inventory inv = Bukkit.createInventory(this, 36, miniMessage.deserialize("<gradient:#00FFA3:#00B8FF><bold>Top 10 - " + currency.name() + "</bold></gradient>"));

                int[] slots = {10, 11, 12, 13, 14, 15, 16, 21, 22, 23};
                for (int i = 0; i < entries.size() && i < slots.length; i++) {
                    TopEntry entry = entries.get(i);
                    ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) skull.getItemMeta();
                    meta.setOwningPlayer(Bukkit.getOfflinePlayer(entry.uuid()));

                    meta.displayName(miniMessage.deserialize("<gold><bold>#" + entry.rank() + "</bold></gold> <yellow>" + entry.username() + "</yellow>"));
                    List<Component> lore = new ArrayList<>();
                    lore.add(miniMessage.deserialize("<gray>Balance: <green>" + currency.formatAmount(entry.balance()) + "</green>"));
                    meta.lore(lore);

                    skull.setItemMeta(meta);
                    inv.setItem(slots[i], skull);
                }

                player.openInventory(inv);
            });
        });
    }

    @Override
    public @NotNull Inventory getInventory() {
        return Bukkit.createInventory(null, 9);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof TopBalancesGUI) {
            event.setCancelled(true);
        }
    }
}
