package cn.reportsystem.paper;

import cn.reportsystem.core.command.Permissions;
import cn.reportsystem.core.i18n.Messages;
import cn.reportsystem.core.model.PendingTeleport;
import cn.reportsystem.core.model.ReportStatus;
import cn.reportsystem.core.storage.StorageException;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Optional;

public final class JoinListener implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final PaperPlugin plugin;

    public JoinListener(PaperPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        plugin.scheduler().runAsync(new Runnable() {
            @Override
            public void run() {
                consumeTeleport(player);
            }
        });
        if (plugin.config().notify.onJoinPending && player.hasPermission(Permissions.NOTIFY)) {
            plugin.scheduler().runAsync(new Runnable() {
                @Override
                public void run() {
                    notifyPending(player);
                }
            });
        }
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
                    Location destination = new Location(world, target.getX(), target.getY(), target.getZ());
                    plugin.scheduler().runFor(new cn.reportsystem.core.model.PlayerRef(
                            player.getUniqueId(), player.getName()), new Runnable() {
                        @Override
                        public void run() {
                            player.teleportAsync(destination);
                            player.sendMessage(LEGACY.deserialize(
                                    plugin.messages().format(Messages.TELEPORT_ARRIVED)));
                        }
                    });
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
        final String text = plugin.messages().format(Messages.NOTIFY_PENDING_COUNT, "count", count);
        plugin.scheduler().runGlobal(new Runnable() {
            @Override
            public void run() {
                player.sendMessage(LEGACY.deserialize(text));
            }
        });
    }
}
