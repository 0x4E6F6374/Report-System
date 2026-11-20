package cn.reportsystem.core.model;

public final class TeleportTarget {

    private final String serverName;
    private final String world;
    private final double x;
    private final double y;
    private final double z;

    public TeleportTarget(String serverName, String world, double x, double y, double z) {
        this.serverName = serverName;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
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
}
