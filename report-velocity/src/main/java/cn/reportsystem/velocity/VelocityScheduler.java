package cn.reportsystem.velocity;

import cn.reportsystem.core.model.PlayerRef;
import cn.reportsystem.core.platform.PluginScheduler;

import com.velocitypowered.api.proxy.ProxyServer;

import java.util.concurrent.TimeUnit;

/**
 * 代理端没有主线程限制，同步任务直接在当前线程跑，只有延迟任务交给调度器。
 */
public final class VelocityScheduler implements PluginScheduler {

    private final ProxyServer proxy;
    private final Object plugin;

    public VelocityScheduler(ProxyServer proxy, Object plugin) {
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
        proxy.getScheduler().buildTask(plugin, task)
                .delay(Math.max(ticks, 1L) * 50L, TimeUnit.MILLISECONDS)
                .schedule();
    }

    @Override
    public void runFor(PlayerRef player, Runnable task) {
        if (proxy.getPlayer(player.getUuid()).isPresent()) {
            task.run();
        }
    }
}
