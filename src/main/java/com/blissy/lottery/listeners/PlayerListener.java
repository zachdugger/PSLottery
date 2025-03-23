package com.blissy.lottery.listeners;

import com.blissy.lottery.Lottery;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerListener implements Listener {
    private final Lottery plugin;

    public PlayerListener(Lottery plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Process offline rewards after a short delay (to ensure all plugins are loaded)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (event.getPlayer().isOnline()) {
                    plugin.getLotteryManager().processOfflineRewards(event.getPlayer());
                }
            }
        }.runTaskLater(plugin, 60); // 3 seconds delay
    }
}