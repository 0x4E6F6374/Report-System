package cn.reportsystem.paper;

import cn.reportsystem.core.model.PlayerRef;
import cn.reportsystem.core.platform.PluginScheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

/**
 * Folia 的 BukkitScheduler 会直接抛异常，所以按品牌分两条路走。
 * 用类存在性探测而不是 ServerBuildInfo，是为了兼容 1.20.5 这类早期版本。
 */
public final class PaperScheduler implements PluginScheduler {

    private final Plugin plugin;
    private final boolean folia;

    public PaperScheduler(Plugin plugin) {
        this.plugin = plugin;
        this.folia = detectFolia();
    }

    public boolean isFolia() {
        return folia;
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public void runGlobal(Runnable task) {
        if (folia) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, task);
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            task.run();
            return;
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void runAsync(Runnable task) {
        if (folia) {
            Bukkit.getAsyncScheduler().runNow(plugin, new Consumer<ScheduledTask>() {
                @Override
                public void accept(ScheduledTask scheduledTask) {
                    task.run();
                }
            });
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public void runLater(Runnable task, long ticks) {
        long delay = Math.max(ticks, 1L);
        if (folia) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, new Consumer<ScheduledTask>() {
                @Override
                public void accept(ScheduledTask scheduledTask) {
                    task.run();
                }
            }, delay);
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, task, delay);
    }

    @Override
    public void runFor(PlayerRef player, Runnable task) {
        Player online = Bukkit.getPlayer(player.getUuid());
        if (online == null) {
            return;
        }
        if (folia) {
            online.getScheduler().run(plugin, new Consumer<ScheduledTask>() {
                @Override
                public void accept(ScheduledTask scheduledTask) {
                    task.run();
                }
            }, null);
            return;
        }
        runGlobal(task);
    }
}
