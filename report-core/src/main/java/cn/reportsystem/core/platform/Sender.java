package cn.reportsystem.core.platform;

import java.util.UUID;

/**
 * 命令发送者的统一视图。控制台发送者没有 UUID，{@link #getUuid()} 会返回 null。
 */
public interface Sender {

    boolean isPlayer();

    UUID getUuid();

    String getName();

    boolean hasPermission(String permission);

    void sendMessage(String message);
}
