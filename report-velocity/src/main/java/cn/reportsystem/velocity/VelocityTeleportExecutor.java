package cn.reportsystem.velocity;

import cn.reportsystem.core.model.PendingTeleport;
import cn.reportsystem.core.model.PlayerRef;
import cn.reportsystem.core.model.TeleportTarget;
import cn.reportsystem.core.platform.TeleportExecutor;
import cn.reportsystem.core.storage.JdbcPendingTeleportRepository;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class VelocityTeleportExecutor implements TeleportExecutor {

    private static final long TTL_MILLIS = 5L * 60L * 1000L;
    private static final MinecraftChannelIdentifier CHANNEL =
            MinecraftChannelIdentifier.create("reportsystem", "teleport");

    private final ProxyServer proxy;
    private final JdbcPendingTeleportRepository teleports;

    public VelocityTeleportExecutor(ProxyServer proxy, JdbcPendingTeleportRepository teleports) {
        this.proxy = proxy;
        this.teleports = teleports;
    }

    @Override
    public Result teleport(PlayerRef admin, TeleportTarget target) {
        Optional<Player> found = proxy.getPlayer(admin.getUuid());
        if (!found.isPresent()) {
            return Result.NO_POSITION;
        }
        Player player = found.get();
        String current = player.getCurrentServer()
                .map(connection -> connection.getServerInfo().getName())
                .orElse("");

        final String uuidText = admin.getUuid().toString();
        teleports.save(new PendingTeleport(admin.getUuid(), target.getServerName(), target.getWorld(),
                target.getX(), target.getY(), target.getZ(), System.currentTimeMillis() + TTL_MILLIS));

        if (current.equalsIgnoreCase(target.getServerName())) {
            // 已经在同一子服，代理改不了坐标，交给后端立刻执行
            player.getCurrentServer().ifPresent(connection -> connection.sendPluginMessage(
                    CHANNEL, uuidText.getBytes(StandardCharsets.UTF_8)));
            return Result.SAME_SERVER;
        }

        Optional<RegisteredServer> server = proxy.getServer(target.getServerName());
        if (!server.isPresent()) {
            return Result.SERVER_MISSING;
        }
        player.createConnectionRequest(server.get()).connectWithIndication();
        return Result.STARTED;
    }
}
