package com.cuac_xd.zeneconomy.commands;

import com.cuac_xd.zeneconomy.config.MessageManager;
import com.cuac_xd.zeneconomy.currency.CurrencyModel;
import com.cuac_xd.zeneconomy.database.DatabaseManager;
import com.cuac_xd.zeneconomy.database.dao.TopEntry;
import com.cuac_xd.zeneconomy.gui.TopBalancesGUI;
import com.cuac_xd.zeneconomy.user.Account;
import com.cuac_xd.zeneconomy.user.UserManager;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CurrencyDedicatedCommand implements CommandExecutor, TabCompleter {

    private final Plugin plugin;
    private final CurrencyModel currency;
    private final UserManager userManager;
    private final DatabaseManager databaseManager;
    private final MessageManager messageManager;
    private final TopBalancesGUI topBalancesGUI;

    public CurrencyDedicatedCommand(Plugin plugin, CurrencyModel currency, UserManager userManager, DatabaseManager databaseManager, MessageManager messageManager, TopBalancesGUI topBalancesGUI) {
        this.plugin = plugin;
        this.currency = currency;
        this.userManager = userManager;
        this.databaseManager = databaseManager;
        this.messageManager = messageManager;
        this.topBalancesGUI = topBalancesGUI;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        // Subcomandos:
        // /<currency>
        // /<currency> [jugador]  (si balance-shorthand es true o permiso balance.others)
        // /<currency> bal [jugador]
        // /<currency> pay <jugador> <cantidad>
        // /<currency> top [pagina / gui]
        // /<currency> give <jugador> <cantidad>
        // /<currency> take <jugador> <cantidad>
        // /<currency> set <jugador> <cantidad>
        // /<currency> reset <jugador>

        String permBase = "zeneconomy.currency." + currency.id();

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                messageManager.sendMessage(sender, "player-only");
                return true;
            }
            showBalance(player, player.getName(), player.getUniqueId());
            return true;
        }

        String sub = args[0].toLowerCase();

        // balance-shorthand o chequeo directo /<currency> <player>
        if (args.length == 1 && !isKnownSubcommand(sub)) {
            if (!sender.hasPermission(permBase + ".balance.others") && !sender.hasPermission("zeneconomy.admin")) {
                messageManager.sendMessage(sender, "no-permission");
                return true;
            }
            resolveAndShowBalance(sender, args[0]);
            return true;
        }

        switch (sub) {
            case "bal":
            case "balance": {
                if (args.length == 1) {
                    if (!(sender instanceof Player player)) {
                        messageManager.sendMessage(sender, "player-only");
                        return true;
                    }
                    showBalance(player, player.getName(), player.getUniqueId());
                    return true;
                }
                if (!sender.hasPermission(permBase + ".balance.others") && !sender.hasPermission("zeneconomy.admin")) {
                    messageManager.sendMessage(sender, "no-permission");
                    return true;
                }
                resolveAndShowBalance(sender, args[1]);
                return true;
            }
            case "pay": {
                if (!(sender instanceof Player player)) {
                    messageManager.sendMessage(sender, "player-only");
                    return true;
                }
                if (!currency.payable()) {
                    player.sendMessage(messageManager.parse("<red>Los pagos entre jugadores están deshabilitados para la moneda " + currency.name() + ".</red>"));
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage(messageManager.parse("<red>Uso: /" + label + " pay <jugador> <cantidad></red>"));
                    return true;
                }
                handlePay(player, args[1], args[2]);
                return true;
            }
            case "top": {
                if (args.length >= 2 && args[1].equalsIgnoreCase("gui")) {
                    if (!(sender instanceof Player player)) {
                        messageManager.sendMessage(sender, "player-only");
                        return true;
                    }
                    topBalancesGUI.open(player, currency);
                    return true;
                }
                int page = 1;
                if (args.length >= 2) {
                    try { page = Math.max(1, Integer.parseInt(args[1])); } catch (NumberFormatException ignored) {}
                }
                handleTop(sender, label, page);
                return true;
            }
            case "give":
            case "take":
            case "set": {
                if (!sender.hasPermission(permBase + ".admin") && !sender.hasPermission("zeneconomy.admin")) {
                    messageManager.sendMessage(sender, "no-permission");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(messageManager.parse("<red>Uso: /" + label + " " + sub + " <jugador> <cantidad></red>"));
                    return true;
                }
                handleAdminModify(sender, sub, args[1], args[2]);
                return true;
            }
            case "reset": {
                if (!sender.hasPermission(permBase + ".admin") && !sender.hasPermission("zeneconomy.admin")) {
                    messageManager.sendMessage(sender, "no-permission");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(messageManager.parse("<red>Uso: /" + label + " reset <jugador></red>"));
                    return true;
                }
                handleAdminReset(sender, args[1]);
                return true;
            }
            default:
                sendHelp(sender, label);
                return true;
        }
    }

    private boolean isKnownSubcommand(String sub) {
        return List.of("bal", "balance", "pay", "top", "give", "take", "set", "reset").contains(sub);
    }

    private void showBalance(CommandSender sender, String targetName, java.util.UUID uuid) {
        userManager.getOrLoadAccount(uuid, targetName).thenAccept(acc -> {
            double bal = acc.getBalance(currency.id());
            if (sender instanceof Player p && p.getUniqueId().equals(uuid)) {
                messageManager.sendMessage(sender, "balance-self",
                        Placeholder.parsed("currency_display", currency.name()),
                        Placeholder.parsed("balance", currency.formatAmount(bal))
                );
            } else {
                messageManager.sendMessage(sender, "balance-others",
                        Placeholder.parsed("target", targetName),
                        Placeholder.parsed("currency_display", currency.name()),
                        Placeholder.parsed("balance", currency.formatAmount(bal))
                );
            }
        });
    }

    private void resolveAndShowBalance(CommandSender sender, String targetName) {
        Player online = Bukkit.getPlayerExact(targetName);
        if (online != null) {
            showBalance(sender, online.getName(), online.getUniqueId());
            return;
        }
        databaseManager.lookupUuidByName(targetName).thenAccept(optUuid -> {
            if (optUuid.isEmpty()) {
                messageManager.sendMessage(sender, "player-not-found", Placeholder.parsed("target", targetName));
                return;
            }
            showBalance(sender, targetName, optUuid.get());
        });
    }

    private void handlePay(Player player, String targetName, String amountStr) {
        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        if (targetPlayer == null) {
            messageManager.sendMessage(player, "player-not-found", Placeholder.parsed("target", targetName));
            return;
        }
        if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
            messageManager.sendMessage(player, "cannot-pay-self");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
                messageManager.sendMessage(player, "invalid-amount");
                return;
            }
            if (!currency.decimal() && amount != Math.floor(amount)) {
                player.sendMessage(messageManager.parse("<red>Esta moneda no admite decimales.</red>"));
                return;
            }
        } catch (NumberFormatException e) {
            messageManager.sendMessage(player, "invalid-amount");
            return;
        }

        Account playerAccount = userManager.getCachedAccount(player.getUniqueId());
        double balance = playerAccount != null ? playerAccount.getBalance(currency.id()) : 0.0;
        if (balance < amount) {
            messageManager.sendMessage(player, "insufficient-funds",
                    Placeholder.parsed("currency", currency.name()),
                    Placeholder.parsed("value", currency.formatAmount(balance))
            );
            return;
        }

        userManager.transfer(player.getUniqueId(), targetPlayer.getUniqueId(), currency.id(), amount).thenAccept(success -> {
            if (success) {
                messageManager.sendMessage(player, "pay-sent",
                        Placeholder.parsed("amount", currency.formatAmount(amount)),
                        Placeholder.parsed("target", targetPlayer.getName())
                );
                messageManager.sendMessage(targetPlayer, "pay-received",
                        Placeholder.parsed("amount", currency.formatAmount(amount)),
                        Placeholder.parsed("sender", player.getName())
                );
            } else {
                messageManager.sendMessage(player, "max-balance-reached",
                        Placeholder.parsed("currency", currency.name()),
                        Placeholder.parsed("max", currency.formatAmount(currency.maxBalance()))
                );
            }
        });
    }

    private void handleTop(CommandSender sender, String label, int page) {
        int pageSize = plugin.getConfig().getInt("baltop.page-size", 10);
        databaseManager.getTopBalances(currency.id(), 100).thenAccept(allEntries -> {
            int total = allEntries.size();
            int maxPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
            int clampedPage = Math.min(page, maxPages);

            int start = (clampedPage - 1) * pageSize;
            int end = Math.min(start + pageSize, total);

            messageManager.sendMessage(sender, "baltop-header",
                    Placeholder.parsed("currency_name", currency.name()),
                    Placeholder.parsed("page", String.valueOf(clampedPage)),
                    Placeholder.parsed("max_pages", String.valueOf(maxPages))
            );

            if (allEntries.isEmpty()) {
                sender.sendMessage(messageManager.parse("<gray>No hay registros aún.</gray>"));
            } else {
                for (int i = start; i < end; i++) {
                    TopEntry entry = allEntries.get(i);
                    messageManager.sendMessage(sender, "baltop-entry",
                            Placeholder.parsed("rank", String.valueOf(entry.rank())),
                            Placeholder.parsed("player", entry.username()),
                            Placeholder.parsed("balance", currency.formatAmount(entry.balance()))
                    );
                }
            }

            if (clampedPage < maxPages) {
                messageManager.sendMessage(sender, "baltop-footer",
                        Placeholder.parsed("currency", currency.id()),
                        Placeholder.parsed("next_page", String.valueOf(clampedPage + 1))
                );
            }
        });
    }

    private void handleAdminModify(CommandSender sender, String action, String targetName, String amountStr) {
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount < 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
                messageManager.sendMessage(sender, "invalid-amount");
                return;
            }
        } catch (NumberFormatException e) {
            messageManager.sendMessage(sender, "invalid-amount");
            return;
        }

        Player online = Bukkit.getPlayerExact(targetName);
        if (online != null) {
            applyAdminModify(sender, action, targetName, online.getUniqueId(), amount);
            return;
        }
        databaseManager.lookupUuidByName(targetName).thenAccept(optUuid -> {
            if (optUuid.isEmpty()) {
                messageManager.sendMessage(sender, "player-not-found", Placeholder.parsed("target", targetName));
                return;
            }
            applyAdminModify(sender, action, targetName, optUuid.get(), amount);
        });
    }

    private void applyAdminModify(CommandSender sender, String action, String targetName, java.util.UUID uuid, double amount) {
        if (action.equals("give")) {
            userManager.deposit(uuid, currency.id(), amount, com.cuac_xd.zeneconomy.api.events.EconomyTransactionEvent.TransactionType.DEPOSIT).thenAccept(ok -> {
                if (ok) {
                    messageManager.sendMessage(sender, "admin-give",
                            Placeholder.parsed("amount", currency.formatAmount(amount)),
                            Placeholder.parsed("target", targetName)
                    );
                } else {
                    messageManager.sendMessage(sender, "max-balance-reached",
                            Placeholder.parsed("currency", currency.name()),
                            Placeholder.parsed("max", currency.formatAmount(currency.maxBalance()))
                    );
                }
            });
        } else if (action.equals("take")) {
            userManager.withdraw(uuid, currency.id(), amount, com.cuac_xd.zeneconomy.api.events.EconomyTransactionEvent.TransactionType.WITHDRAW).thenAccept(ok -> {
                if (ok) {
                    messageManager.sendMessage(sender, "admin-take",
                            Placeholder.parsed("amount", currency.formatAmount(amount)),
                            Placeholder.parsed("target", targetName)
                    );
                } else {
                    messageManager.sendMessage(sender, "min-balance-reached",
                            Placeholder.parsed("min", "0")
                    );
                }
            });
        } else if (action.equals("set")) {
            userManager.setBalance(uuid, currency.id(), amount).thenAccept(ok -> {
                if (ok) {
                    messageManager.sendMessage(sender, "admin-set",
                            Placeholder.parsed("target", targetName),
                            Placeholder.parsed("amount", currency.formatAmount(amount))
                    );
                } else {
                    sender.sendMessage(messageManager.parse("<red>No se pudo establecer el balance (fuera de límites permitidos).</red>"));
                }
            });
        }
    }

    private void handleAdminReset(CommandSender sender, String targetName) {
        Player online = Bukkit.getPlayerExact(targetName);
        if (online != null) {
            userManager.setBalance(online.getUniqueId(), currency.id(), currency.defaultBalance()).thenAccept(ok -> {
                messageManager.sendMessage(sender, "admin-reset",
                        Placeholder.parsed("target", targetName),
                        Placeholder.parsed("currency", currency.name())
                );
            });
            return;
        }
        databaseManager.lookupUuidByName(targetName).thenAccept(optUuid -> {
            if (optUuid.isEmpty()) {
                messageManager.sendMessage(sender, "player-not-found", Placeholder.parsed("target", targetName));
                return;
            }
            userManager.setBalance(optUuid.get(), currency.id(), currency.defaultBalance()).thenAccept(ok -> {
                messageManager.sendMessage(sender, "admin-reset",
                        Placeholder.parsed("target", targetName),
                        Placeholder.parsed("currency", currency.name())
                );
            });
        });
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(messageManager.parse("<gradient:#00FFA3:#00B8FF><bold>--- " + currency.name() + " Comandos ---</bold></gradient>"));
        sender.sendMessage(messageManager.parse("<yellow>/" + label + "</yellow> <gray>- Ver tu balance</gray>"));
        sender.sendMessage(messageManager.parse("<yellow>/" + label + " bal [jugador]</yellow> <gray>- Consultar balance</gray>"));
        if (currency.payable()) {
            sender.sendMessage(messageManager.parse("<yellow>/" + label + " pay <jugador> <cantidad></yellow> <gray>- Pagar fondos</gray>"));
        }
        sender.sendMessage(messageManager.parse("<yellow>/" + label + " top [pagina/gui]</yellow> <gray>- Ver los más ricos</gray>"));
        if (sender.hasPermission("zeneconomy.currency." + currency.id() + ".admin") || sender.hasPermission("zeneconomy.admin")) {
            sender.sendMessage(messageManager.parse("<gold>Admin:</gold> <yellow>/" + label + " give|take|set <jugador> <cantidad></yellow>"));
            sender.sendMessage(messageManager.parse("<gold>Admin:</gold> <yellow>/" + label + " reset <jugador></yellow>"));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        boolean isAdmin = sender.hasPermission("zeneconomy.currency." + currency.id() + ".admin") || sender.hasPermission("zeneconomy.admin");
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            list.add("bal");
            if (currency.payable()) list.add("pay");
            list.add("top");
            if (isAdmin) {
                list.add("give");
                list.add("take");
                list.add("set");
                list.add("reset");
            }
            // También sugerir nombres de jugadores para balance rápido
            Bukkit.getOnlinePlayers().forEach(p -> list.add(p.getName()));
            return list.stream().filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).toList();
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("bal") || sub.equals("balance") || sub.equals("pay") || sub.equals("give") || sub.equals("take") || sub.equals("set") || sub.equals("reset")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).toList();
            }
            if (sub.equals("top")) {
                return List.of("gui", "1", "2", "3").stream().filter(s -> s.startsWith(args[1].toLowerCase())).toList();
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("pay") || sub.equals("give") || sub.equals("take") || sub.equals("set")) {
                return List.of("10", "50", "100", "500", "1000");
            }
        }

        return Collections.emptyList();
    }
}
