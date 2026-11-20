package cn.reportsystem.paper;

import cn.reportsystem.core.model.PendingTeleport;
import cn.reportsystem.core.model.PlayerRef;
import cn.reportsystem.core.model.TeleportTarget;
import cn.reportsystem.core.platform.PluginLogger;
import cn.reportsystem.core.platform.TeleportExecutor;
import cn.reportsystem.core.storage.JdbcPendingTeleportRepository;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class PaperTeleportExecutor implements TeleportExecutor {

    private static final long TTL_MILLIS = 5L * 60L * 1000L;

    private final String serverId;
    private final JdbcPendingTeleportRepository teleports;
    private final PluginLogger logger;

    public PaperTeleportExecutor(String serverId, JdbcPendingTeleportRepository teleports,
                                 PluginLogger logger) {
        this.serverId = serverId;
        this.teleports = teleports;
        this.logger = logger;
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
        Player player = Bukkit.getPlayer(admin.getUuid());
        if (player == null) {
            return Result.NO_POSITION;
        }

        Location destination = new Location(world, target.getX(), target.getY(), target.getZ());
        player.teleportAsync(destination).thenAccept(new java.util.function.Consumer<Boolean>() {
            @Override
            public void accept(Boolean success) {
                if (!Boolean.TRUE.equals(success)) {
                    logger.warn("传送到举报现场失败，目标位置可能未加载");
                }
            }
        });
        return Result.STARTED;
    }
}
