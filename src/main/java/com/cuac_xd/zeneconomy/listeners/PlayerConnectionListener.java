package com.cuac_xd.zeneconomy.listeners;

import com.cuac_xd.zeneconomy.user.Account;
import com.cuac_xd.zeneconomy.user.UserManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final UserManager userManager;

    public PlayerConnectionListener(UserManager userManager) {
        this.userManager = userManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }
        // Carga asíncrona de datos del jugador antes de que entre al mundo
        userManager.loadPlayer(event.getUniqueId(), event.getName()).join();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        // Guarda y descarga la cuenta de la memoria
        userManager.unloadPlayer(event.getPlayer().getUniqueId());
    }
}
