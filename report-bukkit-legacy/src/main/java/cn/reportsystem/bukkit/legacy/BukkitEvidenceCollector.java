package cn.reportsystem.bukkit.legacy;

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

public final class BukkitEvidenceCollector implements EvidenceCollector {

    private final String serverId;
    private final PluginConfig config;
    private final ChatTracker chat;

    public BukkitEvidenceCollector(String serverId, PluginConfig config, ChatTracker chat) {
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

        String clientVersion = config.evidence.captureClientVersion ? protocolOf(online) : null;

        List<String> lines = new ArrayList<String>(chat.recent(target.getUuid()));
        int limit = config.evidence.chatLines;
        if (limit > 0 && lines.size() > limit) {
            lines = lines.subList(lines.size() - limit, lines.size());
        }
        return new Evidence(serverId, world, x, y, z, clientVersion, lines);
    }

    /**
     * 1.8 没有公开的客户端版本 API，只能沿 CraftPlayer 的 nms 链路取协议号，
     * 取不到就放弃这一项证据，不影响举报提交。
     */
    private String protocolOf(Player player) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object connection = handle.getClass().getField("playerConnection").get(handle);
            Object network = connection.getClass().getField("networkManager").get(connection);
            Object version = network.getClass().getMethod("getVersion").invoke(network);
            return "protocol:" + version;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
