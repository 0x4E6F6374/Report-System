package cn.reportsystem.bukkit.legacy;

import cn.reportsystem.core.i18n.Messages;
import cn.reportsystem.core.model.PendingTeleport;
import cn.reportsystem.core.net.Channels;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * 代理端在"管理员已在目标子服"时改不了坐标，只能发消息让后端立刻执行。
 */
public final class TeleportChannelListener implements PluginMessageListener {

    private final ReportSystemPlugin plugin;

    public TeleportChannelListener(ReportSystemPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getServer().getMessenger()
                .registerIncomingPluginChannel(plugin, Channels.TELEPORT, this);
    }

    public void unregister() {
        plugin.getServer().getMessenger()
                .unregisterIncomingPluginChannel(plugin, Channels.TELEPORT, this);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player receiver, byte[] data) {
        if (!Channels.TELEPORT.equals(channel)) {
            return;
        }
        final UUID target;
        try {
            target = UUID.fromString(new String(data, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            return;
        }
        if (!receiver.getUniqueId().equals(target)) {
            return;
        }
        plugin.scheduler().runAsync(new Runnable() {
            @Override
            public void run() {
                deliver(receiver);
            }
        });
    }

    private void deliver(final Player player) {
        Optional<PendingTeleport> found;
        try {
            found = plugin.teleports().consume(player.getUniqueId());
        } catch (RuntimeException e) {
            plugin.logger().warn("读取待执行传送失败: " + e.getMessage());
            return;
        }
        if (!found.isPresent()) {
            return;
        }
        final PendingTeleport target = found.get();
        if (target.getExpiresAt() < System.currentTimeMillis()) {
            return;
        }
        plugin.scheduler().runGlobal(new Runnable() {
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
        });
    }
}
