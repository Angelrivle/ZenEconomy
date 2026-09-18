package com.cuac_xd.zeneconomy.api.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class EconomyTransactionEvent extends Event implements Cancellable {

    public enum TransactionType {
        DEPOSIT,
        WITHDRAW,
        SET,
        TRANSFER
    }

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;

    private final UUID playerUuid;
    private final String currencyId;
    private final double amount;
    private final double oldBalance;
    private final double newBalance;
    private final TransactionType transactionType;

    public EconomyTransactionEvent(UUID playerUuid, String currencyId, double amount, double oldBalance, double newBalance, TransactionType transactionType) {
        super(true); // Asynchronous by nature
        this.playerUuid = playerUuid;
        this.currencyId = currencyId;
        this.amount = amount;
        this.oldBalance = oldBalance;
        this.newBalance = newBalance;
        this.transactionType = transactionType;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getCurrencyId() {
        return currencyId;
    }

    public double getAmount() {
        return amount;
    }

    public double getOldBalance() {
        return oldBalance;
    }

    public double getNewBalance() {
        return newBalance;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
