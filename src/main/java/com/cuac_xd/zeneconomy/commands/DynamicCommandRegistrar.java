package com.cuac_xd.zeneconomy.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class DynamicCommandRegistrar {

    private static CommandMap commandMap;
    private static final List<String> registeredCommands = new ArrayList<>();

    static {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());
        } catch (Exception e) {
            Bukkit.getLogger().warning("No se pudo obtener CommandMap vía reflexión: " + e.getMessage());
        }
    }

    public static void register(Plugin plugin, String commandName, List<String> aliases, CommandExecutor executor, TabCompleter completer) {
        if (commandMap == null) return;

        Command customCommand = new Command(commandName, "Comando dedicado de ZenEconomy", "/" + commandName, aliases != null ? aliases : List.of()) {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
                return executor.onCommand(sender, this, commandLabel, args);
            }

            @Override
            public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
                if (completer != null) {
                    List<String> list = completer.onTabComplete(sender, this, alias, args);
                    return list != null ? list : super.tabComplete(sender, alias, args);
                }
                return super.tabComplete(sender, alias, args);
            }
        };

        commandMap.register(plugin.getName().toLowerCase(), customCommand);
        registeredCommands.add(commandName);
        if (aliases != null) {
            registeredCommands.addAll(aliases);
        }
    }
}
