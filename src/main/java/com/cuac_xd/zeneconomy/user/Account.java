package com.cuac_xd.zeneconomy.user;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Account {

    private final UUID uuid;
    private String username;
    private final Map<String, Double> balances = new ConcurrentHashMap<>();
    private volatile boolean dirty = false;

    public Account(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
        this.dirty = true;
    }

    public double getBalance(String currencyId) {
        return balances.getOrDefault(currencyId.toLowerCase(), 0.0);
    }

    public void setBalance(String currencyId, double amount) {
        balances.put(currencyId.toLowerCase(), amount);
        this.dirty = true;
    }

    public boolean has(String currencyId, double amount) {
        return getBalance(currencyId) >= amount;
    }

    public Map<String, Double> getAllBalances() {
        return Collections.unmodifiableMap(balances);
    }

    public void loadBalancesFromDb(Map<String, Double> loaded) {
        this.balances.putAll(loaded);
        this.dirty = false;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
