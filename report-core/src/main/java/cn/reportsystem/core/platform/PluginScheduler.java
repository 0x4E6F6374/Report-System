package cn.reportsystem.core.platform;

import cn.reportsystem.core.model.PlayerRef;

/**
 * 所有跨线程任务都经由这里派发。Folia 上 BukkitScheduler 不可用，
 * 平台实现负责选择正确的调度器。
 */
public interface PluginScheduler {

    void runGlobal(Runnable task);

    void runAsync(Runnable task);

    void runLater(Runnable task, long ticks);

    /**
     * 玩家离线时任务直接丢弃，调用方不要依赖它一定会执行。
     */
    void runFor(PlayerRef player, Runnable task);
}
