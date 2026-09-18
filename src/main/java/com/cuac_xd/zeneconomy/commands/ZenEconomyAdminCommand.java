package com.cuac_xd.zeneconomy.commands;

import com.cuac_xd.zeneconomy.config.ConfigManager;
import com.cuac_xd.zeneconomy.config.MessageManager;
import com.cuac_xd.zeneconomy.currency.CurrencyModel;
import com.cuac_xd.zeneconomy.currency.CurrencyRegistry;
import com.cuac_xd.zeneconomy.database.DatabaseManager;
import com.cuac_xd.zeneconomy.user.UserManager;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ZenEconomyAdminCommand implements CommandExecutor, TabCompleter {

    private final UserManager userManager;
    private final CurrencyRegistry currencyRegistry;
    private final MessageManager messageManager;
    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;

    public ZenEconomyAdminCommand(UserManager userManager, CurrencyRegistry currencyRegistry, MessageManager messageManager, DatabaseManager databaseManager, ConfigManager configManager) {
        this.userManager = userManager;
        this.currencyRegistry = currencyRegistry;
        this.messageManager = messageManager;
        this.databaseManager = databaseManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("zeneconomy.admin")) {
            messageManager.sendMessage(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("reload")) {
            configManager.reload();
            messageManager.sendMessage(sender, "reload-success");
            return true;
        }

        if (sub.equals("currencies") || sub.equals("list")) {
            sender.sendMessage(messageManager.parse("<gradient:#00FFA3:#00B8FF><bold>--- Monedas Registradas ---</bold></gradient>"));
            for (CurrencyModel model : currencyRegistry.getAllCurrencies()) {
                sender.sendMessage(messageManager.parse("<gold>•</gold> <yellow>" + model.id() + "</yellow> (" + model.name() + ") Símbolo: " + model.symbol() + " | MaxDec: " + model.maxDecimals() + " | Default: " + model.defaultBalance() + " | Max: " + (model.maxBalance() < 0 ? "Sin límite" : model.formatAmount(model.maxBalance())) + " | Comandos: " + model.commands()));
            }
            return true;
        }

        // Acciones sobre jugadores: give, take, set, reset
        // /zeneconomy give <player> <amount> <moneda>
        // /zeneconomy take <player> <amount> <moneda>
        // /zeneconomy set <player> <amount> <moneda>
        // /zeneconomy reset <player> <moneda>
        if (sub.equals("give") || sub.equals("take") || sub.equals("set")) {
            if (args.length < 4) {
                sender.sendMessage(messageManager.parse("<red>Uso: /zeneconomy " + sub + " <jugador> <cantidad> <moneda></red>"));
                return true;
            }

            String targetName = args[1];
            double amount;
            try {
                amount = Double.parseDouble(args[2]);
                if (amount < 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
                    messageManager.sendMessage(sender, "invalid-amount");
                    return true;
                }
            } catch (NumberFormatException e) {
                messageManager.sendMessage(sender, "invalid-amount");
                return true;
            }

            String currencyId = args[3];
            Optional<CurrencyModel> optCurr = currencyRegistry.getCurrency(currencyId);
            if (optCurr.isEmpty()) {
                messageManager.sendMessage(sender, "invalid-currency", Placeholder.parsed("currency", currencyId));
                return true;
            }

            CurrencyModel currency = optCurr.get();
            resolveTargetUuid(targetName, optUuid -> {
                if (optUuid.isEmpty()) {
                    messageManager.sendMessage(sender, "player-not-found", Placeholder.parsed("target", targetName));
                    return;
                }

                var uuid = optUuid.get();
                if (sub.equals("give")) {
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
                } else if (sub.equals("take")) {
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
                } else {
                    userManager.setBalance(uuid, currency.id(), amount).thenAccept(ok -> {
                        if (ok) {
                            messageManager.sendMessage(sender, "admin-set",
                                    Placeholder.parsed("target", targetName),
                                    Placeholder.parsed("amount", currency.formatAmount(amount))
                            );
                        } else {
                            sender.sendMessage(messageManager.parse("<red>No se pudo establecer el balance. Verifica los límites configurados.</red>"));
                        }
                    });
                }
            });

            return true;
        }

        if (sub.equals("reset")) {
            if (args.length < 3) {
                sender.sendMessage(messageManager.parse("<red>Uso: /zeneconomy reset <jugador> <moneda></red>"));
                return true;
            }

            String targetName = args[1];
            String currencyId = args[2];
            Optional<CurrencyModel> optCurr = currencyRegistry.getCurrency(currencyId);
            if (optCurr.isEmpty()) {
                messageManager.sendMessage(sender, "invalid-currency", Placeholder.parsed("currency", currencyId));
                return true;
            }

            CurrencyModel currency = optCurr.get();
            resolveTargetUuid(targetName, optUuid -> {
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

            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void resolveTargetUuid(String name, java.util.function.Consumer<Optional<java.util.UUID>> callback) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            callback.accept(Optional.of(online.getUniqueId()));
            return;
        }
        databaseManager.lookupUuidByName(name).thenAccept(callback);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(messageManager.parse("<gradient:#00FFA3:#00B8FF><bold>--- Comandos ZenEconomy ---</bold></gradient>"));
        sender.sendMessage(messageManager.parse("<yellow>/zeneconomy give <jugador> <cantidad> [moneda]</yellow> <gray>- Dar fondos</gray>"));
        sender.sendMessage(messageManager.parse("<yellow>/zeneconomy take <jugador> <cantidad> [moneda]</yellow> <gray>- Quitar fondos</gray>"));
        sender.sendMessage(messageManager.parse("<yellow>/zeneconomy set <jugador> <cantidad> [moneda]</yellow> <gray>- Fijar balance</gray>"));
        sender.sendMessage(messageManager.parse("<yellow>/zeneconomy reset <jugador> [moneda]</yellow> <gray>- Reiniciar balance por defecto</gray>"));
        sender.sendMessage(messageManager.parse("<yellow>/zeneconomy currencies</yellow> <gray>- Ver lista de monedas</gray>"));
        sender.sendMessage(messageManager.parse("<yellow>/zeneconomy reload</yellow> <gray>- Recargar configuraciones</gray>"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("give", "take", "set", "reset", "currencies", "reload").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2 && List.of("give", "take", "set", "reset").contains(args[0].toLowerCase())) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).toList();
        }
        if (args.length == 3 && List.of("give", "take", "set").contains(args[0].toLowerCase())) {
            return List.of("10", "100", "1000", "5000");
        }
        if ((args.length == 4 && List.of("give", "take", "set").contains(args[0].toLowerCase())) ||
            (args.length == 3 && args[0].equalsIgnoreCase("reset"))) {
            return currencyRegistry.getCurrencyIds().stream()
                    .filter(c -> c.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).toList();
        }
        return Collections.emptyList();
    }
}
