package cn.reportsystem.paper;

import cn.reportsystem.core.i18n.Messages;
import cn.reportsystem.core.model.PendingTeleport;
import cn.reportsystem.core.net.Channels;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

public final class TeleportChannelListener implements PluginMessageListener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final PaperPlugin plugin;

    public TeleportChannelListener(PaperPlugin plugin) {
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
        plugin.scheduler().runFor(new cn.reportsystem.core.model.PlayerRef(
                player.getUniqueId(), player.getName()), new Runnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld(target.getWorld());
                if (world == null) {
                    plugin.logger().warn("待执行传送的世界不存在: " + target.getWorld());
                    return;
                }
                Location destination = new Location(world, target.getX(), target.getY(), target.getZ());
                player.teleportAsync(destination);
                player.sendMessage(LEGACY.deserialize(
                        plugin.msgFor(player).format(Messages.TELEPORT_ARRIVED)));
            }
        });
    }
}
