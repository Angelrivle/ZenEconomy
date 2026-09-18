package com.cuac_xd.zeneconomy.gui;

import com.cuac_xd.zeneconomy.currency.CurrencyModel;
import com.cuac_xd.zeneconomy.currency.CurrencyRegistry;
import com.cuac_xd.zeneconomy.user.Account;
import com.cuac_xd.zeneconomy.user.UserManager;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class WalletGUI implements InventoryHolder, Listener {

    private final UserManager userManager;
    private final CurrencyRegistry currencyRegistry;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public WalletGUI(UserManager userManager, CurrencyRegistry currencyRegistry) {
        this.userManager = userManager;
        this.currencyRegistry = currencyRegistry;
    }

    public void open(Player player) {
        Account account = userManager.getCachedAccount(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(this, 27, miniMessage.deserialize("<gradient:#00FFA3:#00B8FF><bold>Mi Billetera</bold></gradient>"));

        // Relleno decorativo
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(Component.empty());
        glass.setItemMeta(glassMeta);

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, glass);
        }

        int slot = 10;
        for (CurrencyModel curr : currencyRegistry.getAllCurrencies()) {
            if (slot > 16) break;

            double balance = account != null ? account.getBalance(curr.id()) : 0.0;
            ItemStack item = new ItemStack(curr.iconMaterial());
            ItemMeta meta = item.getItemMeta();

            if (curr.customModelData() > 0) {
                meta.setCustomModelData(curr.customModelData());
            }

            meta.displayName(miniMessage.deserialize(curr.iconDisplayName()));
            List<Component> lore = new ArrayList<>();

            for (String l : curr.iconLore()) {
                lore.add(miniMessage.deserialize(l));
            }
            lore.add(Component.empty());
            lore.add(miniMessage.deserialize("<gray>Balance: <green>" + curr.formatAmount(balance) + "</green>"));
            lore.add(miniMessage.deserialize("<dark_gray>ID Moneda: <white>" + curr.id() + "</white>"));

            meta.lore(lore);
            item.setItemMeta(meta);

            inv.setItem(slot++, item);
        }

        player.openInventory(inv);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return Bukkit.createInventory(null, 9);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof WalletGUI) {
            event.setCancelled(true);
        }
    }
}
