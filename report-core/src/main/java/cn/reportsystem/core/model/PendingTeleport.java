package cn.reportsystem.core.model;

import java.util.UUID;

public final class PendingTeleport {

    private final UUID playerUuid;
    private final String serverName;
    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final long expiresAt;

    public PendingTeleport(UUID playerUuid, String serverName, String world,
                           double x, double y, double z, long expiresAt) {
        this.playerUuid = playerUuid;
        this.serverName = serverName;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.expiresAt = expiresAt;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getServerName() {
        return serverName;
    }

    public String getWorld() {
        return world;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public long getExpiresAt() {
        return expiresAt;
    }
}
