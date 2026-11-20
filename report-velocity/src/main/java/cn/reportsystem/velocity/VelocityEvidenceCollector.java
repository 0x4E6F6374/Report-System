package cn.reportsystem.velocity;

import cn.reportsystem.core.config.PluginConfig;
import cn.reportsystem.core.model.Evidence;
import cn.reportsystem.core.model.PlayerRef;
import cn.reportsystem.core.platform.EvidenceCollector;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.Optional;

public final class VelocityEvidenceCollector implements EvidenceCollector {

    private final ProxyServer proxy;
    private final String fallbackServerId;
    private final PluginConfig config;

    public VelocityEvidenceCollector(ProxyServer proxy, String fallbackServerId, PluginConfig config) {
        this.proxy = proxy;
        this.fallbackServerId = fallbackServerId;
        this.config = config;
    }

    @Override
    public Evidence collect(PlayerRef target) {
        Optional<Player> found = proxy.getPlayer(target.getUuid());
        if (!found.isPresent()) {
            return Evidence.ofServer(fallbackServerId, null);
        }
        Player player = found.get();
        String server = player.getCurrentServer()
                .map(connection -> connection.getServerInfo().getName())
                .orElse(fallbackServerId);
        String clientVersion = config.evidence.captureClientVersion
                ? String.valueOf(player.getProtocolVersion().getProtocol())
                : null;
        return Evidence.ofServer(server, clientVersion);
    }
}
