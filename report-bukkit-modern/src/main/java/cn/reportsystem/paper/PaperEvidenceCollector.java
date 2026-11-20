package cn.reportsystem.paper;

import cn.reportsystem.core.chat.ChatTracker;
import cn.reportsystem.core.config.PluginConfig;
import cn.reportsystem.core.model.Evidence;
import cn.reportsystem.core.model.PlayerRef;
import cn.reportsystem.core.platform.EvidenceCollector;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class PaperEvidenceCollector implements EvidenceCollector {

    private final String serverId;
    private final PluginConfig config;
    private final ChatTracker chat;

    public PaperEvidenceCollector(String serverId, PluginConfig config, ChatTracker chat) {
        this.serverId = serverId;
        this.config = config;
        this.chat = chat;
    }

    @Override
    public Evidence collect(PlayerRef target) {
        Player online = Bukkit.getPlayer(target.getUuid());
        if (online == null) {
            return Evidence.ofServer(serverId, null);
        }

        Double x = null;
        Double y = null;
        Double z = null;
        String world = null;
        if (config.evidence.capturePosition) {
            Location location = online.getLocation();
            x = location.getX();
            y = location.getY();
            z = location.getZ();
            world = location.getWorld().getName();
        }

        // Paper 只暴露客户端品牌，拿不到协议版本，这里如实记录品牌名
        String clientVersion = config.evidence.captureClientVersion ? online.getClientBrandName() : null;

        List<String> lines = new ArrayList<String>(chat.recent(target.getUuid()));
        int limit = config.evidence.chatLines;
        if (limit > 0 && lines.size() > limit) {
            lines = lines.subList(lines.size() - limit, lines.size());
        }
        return new Evidence(serverId, world, x, y, z, clientVersion, lines);
    }
}
