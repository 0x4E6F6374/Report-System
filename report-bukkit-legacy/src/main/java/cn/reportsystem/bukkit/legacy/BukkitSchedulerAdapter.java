package cn.reportsystem.bukkit.legacy;

import cn.reportsystem.core.model.PlayerRef;
import cn.reportsystem.core.platform.PluginScheduler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class BukkitSchedulerAdapter implements PluginScheduler {

    private final Plugin plugin;

    public BukkitSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runGlobal(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
            return;
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void runAsync(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public void runLater(Runnable task, long ticks) {
        Bukkit.getScheduler().runTaskLater(plugin, task, ticks);
    }

    @Override
    public void runFor(PlayerRef player, Runnable task) {
        Player online = Bukkit.getPlayer(player.getUuid());
        if (online == null) {
            return;
        }
        runGlobal(task);
    }
}
