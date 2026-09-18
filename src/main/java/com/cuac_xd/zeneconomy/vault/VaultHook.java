package com.cuac_xd.zeneconomy.vault;

import com.cuac_xd.zeneconomy.currency.CurrencyModel;
import com.cuac_xd.zeneconomy.currency.CurrencyRegistry;
import com.cuac_xd.zeneconomy.user.Account;
import com.cuac_xd.zeneconomy.user.UserManager;
import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

import java.util.Collections;
import java.util.List;

public class VaultHook extends AbstractEconomy {

    private final Plugin plugin;
    private final UserManager userManager;
    private final CurrencyRegistry currencyRegistry;

    public VaultHook(Plugin plugin, UserManager userManager, CurrencyRegistry currencyRegistry) {
        this.plugin = plugin;
        this.userManager = userManager;
        this.currencyRegistry = currencyRegistry;
    }

    public void hook() {
        Bukkit.getServicesManager().register(net.milkbowl.vault.economy.Economy.class, this, plugin, ServicePriority.High);
        plugin.getLogger().info("ZenEconomy se ha registrado exitosamente como Vault Economy Provider.");
    }

    public void unhook() {
        Bukkit.getServicesManager().unregister(net.milkbowl.vault.economy.Economy.class, this);
    }

    private CurrencyModel getVaultCurrency() {
        return currencyRegistry.getVaultCurrency().orElse(null);
    }

    @Override
    public boolean isEnabled() {
        return plugin.isEnabled();
    }

    @Override
    public String getName() {
        return "ZenEconomy";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        CurrencyModel curr = getVaultCurrency();
        return curr != null ? curr.maxDecimals() : 2;
    }

    @Override
    public String format(double amount) {
        CurrencyModel curr = getVaultCurrency();
        return curr != null ? curr.formatAmount(amount) : String.valueOf(amount);
    }

    @Override
    public String currencyNamePlural() {
        CurrencyModel curr = getVaultCurrency();
        return curr != null ? curr.name() : "Coins";
    }

    @Override
    public String currencyNameSingular() {
        CurrencyModel curr = getVaultCurrency();
        return curr != null ? curr.name() : "Coin";
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return true;
    }

    @Override
    public boolean hasAccount(String playerName) {
        return true;
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        CurrencyModel curr = getVaultCurrency();
        if (curr == null) return 0.0;
        try {
            Account account = userManager.getOrLoadAccount(player.getUniqueId(), player.getName()).join();
            return account != null ? account.getBalance(curr.id()) : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    @Override
    public double getBalance(String playerName) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return getBalance(player);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public boolean has(String playerName, double amount) {
        return getBalance(playerName) >= amount;
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Cannot withdraw negative funds");
        }
        CurrencyModel curr = getVaultCurrency();
        if (curr == null) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Default currency not configured");
        }
        try {
            boolean success = userManager.withdraw(player.getUniqueId(), curr.id(), amount, com.cuac_xd.zeneconomy.api.events.EconomyTransactionEvent.TransactionType.WITHDRAW).join();
            double balance = getBalance(player);
            if (success) {
                return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.SUCCESS, null);
            } else {
                return new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE, "Insufficient funds or limit exceeded");
            }
        } catch (Exception e) {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, e.getMessage());
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Cannot deposit negative funds");
        }
        CurrencyModel curr = getVaultCurrency();
        if (curr == null) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Default currency not configured");
        }
        try {
            boolean success = userManager.deposit(player.getUniqueId(), curr.id(), amount, com.cuac_xd.zeneconomy.api.events.EconomyTransactionEvent.TransactionType.DEPOSIT).join();
            double balance = getBalance(player);
            if (success) {
                return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.SUCCESS, null);
            } else {
                return new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE, "Max balance exceeded or limit reached");
            }
        } catch (Exception e) {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, e.getMessage());
        }
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return depositPlayer(player, amount);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        return true;
    }

    // World specific balances redirect to global balances
    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    // Banks unsupported
    @Override public EconomyResponse createBank(String name, String player) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported"); }
    @Override public EconomyResponse createBank(String name, OfflinePlayer player) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported"); }
    @Override public EconomyResponse deleteBank(String name) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported"); }
    @Override public EconomyResponse bankBalance(String name) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported"); }
    @Override public EconomyResponse bankHas(String name, double amount) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported"); }
    @Override public EconomyResponse bankWithdraw(String name, double amount) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported"); }
    @Override public EconomyResponse bankDeposit(String name, double amount) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported"); }
    @Override public EconomyResponse isBankOwner(String name, String playerName) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported"); }
    @Override public EconomyResponse isBankOwner(String name, OfflinePlayer player) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported"); }
    @Override public EconomyResponse isBankMember(String name, String playerName) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported"); }
    @Override public EconomyResponse isBankMember(String name, OfflinePlayer player) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported"); }
    @Override public List<String> getBanks() { return Collections.emptyList(); }
}
