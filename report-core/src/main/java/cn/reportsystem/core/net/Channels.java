package cn.reportsystem.core.net;

public final class Channels {

    /**
     * 代理端与后端之间传递"立刻执行待传送"的通知，载荷是管理员 UUID 的 UTF-8 字节。
     */
    public static final String TELEPORT = "reportsystem:teleport";

    private Channels() {
    }
}
