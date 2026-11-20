package cn.reportsystem.bukkit.legacy;

import cn.reportsystem.core.command.Permissions;
import cn.reportsystem.core.i18n.MessageService;
import cn.reportsystem.core.i18n.Messages;
import cn.reportsystem.core.model.PendingTeleport;
import cn.reportsystem.core.model.ReportStatus;
import cn.reportsystem.core.storage.StorageException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;
import java.util.UUID;

public final class PlayerSessionListener implements Listener {

    private final ReportSystemPlugin plugin;

    public PlayerSessionListener(ReportSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        final boolean notify = plugin.config().notify.onJoinPending
                && player.hasPermission(Permissions.NOTIFY);

        plugin.scheduler().runAsync(new Runnable() {
            @Override
            public void run() {
                // 先从库里取回玩家上次选择的语言，后续消息才不会是默认语言
                plugin.locales().refresh(uuid);
                consumeTeleport(player);
                if (notify) {
                    notifyPending(player);
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.locales().forget(event.getPlayer().getUniqueId());
        plugin.chatTracker().forget(event.getPlayer().getUniqueId());
    }

    private void consumeTeleport(final Player player) {
        try {
            Optional<PendingTeleport> found = plugin.teleports().consume(player.getUniqueId());
            if (!found.isPresent()) {
                return;
            }
            final PendingTeleport target = found.get();
            if (target.getExpiresAt() < System.currentTimeMillis()) {
                return;
            }
            plugin.scheduler().runLater(new Runnable() {
                @Override
                public void run() {
                    World world = Bukkit.getWorld(target.getWorld());
                    if (world == null) {
                        plugin.logger().warn("待执行传送的世界不存在: " + target.getWorld());
                        return;
                    }
                    player.teleport(new Location(world, target.getX(), target.getY(), target.getZ()));
                    player.sendMessage(plugin.msgFor(player).format(Messages.TELEPORT_ARRIVED));
                }
            }, 20L);
        } catch (StorageException e) {
            plugin.logger().warn("处理待执行传送失败: " + e.getMessage());
        }
    }

    private void notifyPending(final Player player) {
        long count = plugin.service().stats().get(ReportStatus.PENDING);
        if (count <= 0) {
            return;
        }
        final MessageService messages = plugin.msgFor(player);
        final String text = messages.format(Messages.NOTIFY_PENDING_COUNT, "count", count);
        plugin.scheduler().runGlobal(new Runnable() {
            @Override
            public void run() {
                player.sendMessage(text);
            }
        });
    }
}
