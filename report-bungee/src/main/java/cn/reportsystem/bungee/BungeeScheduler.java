package cn.reportsystem.bungee;

import cn.reportsystem.core.model.PlayerRef;
import cn.reportsystem.core.platform.PluginScheduler;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.concurrent.TimeUnit;

public final class BungeeScheduler implements PluginScheduler {

    private final ProxyServer proxy;
    private final Plugin plugin;

    public BungeeScheduler(ProxyServer proxy, Plugin plugin) {
        this.proxy = proxy;
        this.plugin = plugin;
    }

    @Override
    public void runGlobal(Runnable task) {
        task.run();
    }

    @Override
    public void runAsync(Runnable task) {
        task.run();
    }

    @Override
    public void runLater(Runnable task, long ticks) {
        proxy.getScheduler().schedule(plugin, task, Math.max(ticks, 1L) * 50L, TimeUnit.MILLISECONDS);
    }

    @Override
    public void runFor(PlayerRef player, Runnable task) {
        if (proxy.getPlayer(player.getUuid()) != null) {
            task.run();
        }
    }
}
