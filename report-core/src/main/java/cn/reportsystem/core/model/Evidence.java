package cn.reportsystem.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Evidence {

    private final String serverName;
    private final String world;
    private final Double x;
    private final Double y;
    private final Double z;
    private final String clientVersion;
    private final List<String> chat;

    public Evidence(String serverName, String world, Double x, Double y, Double z,
                    String clientVersion, List<String> chat) {
        this.serverName = serverName;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.clientVersion = clientVersion;
        this.chat = chat == null ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(chat));
    }

    /**
     * 代理端拿不到世界和坐标，只能提供所在子服与客户端版本。
     */
    public static Evidence ofServer(String serverName, String clientVersion) {
        return new Evidence(serverName, null, null, null, null, clientVersion, null);
    }

    public String getServerName() {
        return serverName;
    }

    public String getWorld() {
        return world;
    }

    public Double getX() {
        return x;
    }

    public Double getY() {
        return y;
    }

    public Double getZ() {
        return z;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public List<String> getChat() {
        return chat;
    }

    public boolean hasPosition() {
        return x != null && y != null && z != null;
    }
}
