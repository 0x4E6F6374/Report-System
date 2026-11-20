package cn.reportsystem.bungee;

import cn.reportsystem.core.config.PluginConfig;
import cn.reportsystem.core.model.Evidence;
import cn.reportsystem.core.model.PlayerRef;
import cn.reportsystem.core.platform.EvidenceCollector;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

public final class BungeeEvidenceCollector implements EvidenceCollector {

    private final ProxyServer proxy;
    private final String fallbackServerId;
    private final PluginConfig config;

    public BungeeEvidenceCollector(ProxyServer proxy, String fallbackServerId, PluginConfig config) {
        this.proxy = proxy;
        this.fallbackServerId = fallbackServerId;
        this.config = config;
    }

    @Override
    public Evidence collect(PlayerRef target) {
        ProxiedPlayer player = proxy.getPlayer(target.getUuid());
        if (player == null) {
            return Evidence.ofServer(fallbackServerId, null);
        }
        Server server = player.getServer();
        String serverName = server == null ? fallbackServerId : server.getInfo().getName();
        String clientVersion = config.evidence.captureClientVersion
                ? String.valueOf(player.getPendingConnection().getVersion())
                : null;
        return Evidence.ofServer(serverName, clientVersion);
    }
}
