package cn.reportsystem.bukkit.legacy;

import cn.reportsystem.core.model.PendingTeleport;
import cn.reportsystem.core.model.PlayerRef;
import cn.reportsystem.core.model.TeleportTarget;
import cn.reportsystem.core.platform.TeleportExecutor;
import cn.reportsystem.core.storage.JdbcPendingTeleportRepository;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class BukkitTeleportExecutor implements TeleportExecutor {

    private static final long TTL_MILLIS = 5L * 60L * 1000L;

    private final Plugin plugin;
    private final String serverId;
    private final JdbcPendingTeleportRepository teleports;

    public BukkitTeleportExecutor(Plugin plugin, String serverId,
                                  JdbcPendingTeleportRepository teleports) {
        this.plugin = plugin;
        this.serverId = serverId;
        this.teleports = teleports;
    }

    @Override
    public Result teleport(PlayerRef admin, TeleportTarget target) {
        if (target.getWorld() == null || target.getServerName() == null) {
            return Result.NO_POSITION;
        }
        if (!serverId.equalsIgnoreCase(target.getServerName())) {
            teleports.save(new PendingTeleport(admin.getUuid(), target.getServerName(),
                    target.getWorld(), target.getX(), target.getY(), target.getZ(),
                    System.currentTimeMillis() + TTL_MILLIS));
            return Result.QUEUED;
        }

        World world = Bukkit.getWorld(target.getWorld());
        if (world == null) {
            return Result.SERVER_MISSING;
        }
        final Player player = Bukkit.getPlayer(admin.getUuid());
        if (player == null) {
            return Result.NO_POSITION;
        }
        final Location destination = new Location(world, target.getX(), target.getY(), target.getZ());
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                player.teleport(destination);
            }
        });
        return Result.STARTED;
    }
}
