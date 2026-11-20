package cn.reportsystem.bungee;

import cn.reportsystem.core.model.PendingTeleport;
import cn.reportsystem.core.model.PlayerRef;
import cn.reportsystem.core.model.TeleportTarget;
import cn.reportsystem.core.net.Channels;
import cn.reportsystem.core.platform.TeleportExecutor;
import cn.reportsystem.core.storage.JdbcPendingTeleportRepository;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

import java.nio.charset.StandardCharsets;

public final class BungeeTeleportExecutor implements TeleportExecutor {

    private static final long TTL_MILLIS = 5L * 60L * 1000L;

    private final ProxyServer proxy;
    private final JdbcPendingTeleportRepository teleports;

    public BungeeTeleportExecutor(ProxyServer proxy, JdbcPendingTeleportRepository teleports) {
        this.proxy = proxy;
        this.teleports = teleports;
    }

    @Override
    public Result teleport(PlayerRef admin, TeleportTarget target) {
        ProxiedPlayer player = proxy.getPlayer(admin.getUuid());
        if (player == null) {
            return Result.NO_POSITION;
        }

        Server current = player.getServer();
        String currentName = current == null ? "" : current.getInfo().getName();
        byte[] payload = admin.getUuid().toString().getBytes(StandardCharsets.UTF_8);

        teleports.save(new PendingTeleport(admin.getUuid(), target.getServerName(), target.getWorld(),
                target.getX(), target.getY(), target.getZ(), System.currentTimeMillis() + TTL_MILLIS));

        if (currentName.equalsIgnoreCase(target.getServerName())) {
            if (current != null) {
                current.sendData(Channels.TELEPORT, payload);
            }
            return Result.SAME_SERVER;
        }

        ServerInfo destination = proxy.getServerInfo(target.getServerName());
        if (destination == null) {
            return Result.SERVER_MISSING;
        }
        player.connect(destination);
        return Result.STARTED;
    }
}
