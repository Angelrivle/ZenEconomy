package com.cuac_xd.zeneconomy.commands;

import com.cuac_xd.zeneconomy.config.MessageManager;
import com.cuac_xd.zeneconomy.gui.WalletGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class WalletCommand implements CommandExecutor {

    private final WalletGUI walletGUI;
    private final MessageManager messageManager;

    public WalletCommand(WalletGUI walletGUI, MessageManager messageManager) {
        this.walletGUI = walletGUI;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, "player-only");
            return true;
        }

        walletGUI.open(player);
        return true;
    }
}
