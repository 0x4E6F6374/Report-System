package cn.reportsystem.velocity;

import cn.reportsystem.core.platform.Sender;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.UUID;

public final class VelocitySender implements Sender {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final CommandSource handle;

    public VelocitySender(CommandSource handle) {
        this.handle = handle;
    }

    @Override
    public boolean isPlayer() {
        return handle instanceof Player;
    }

    @Override
    public UUID getUuid() {
        return handle instanceof Player ? ((Player) handle).getUniqueId() : null;
    }

    @Override
    public String getName() {
        if (handle instanceof Player) {
            return ((Player) handle).getUsername();
        }
        return "CONSOLE";
    }

    @Override
    public boolean hasPermission(String permission) {
        return handle.hasPermission(permission);
    }

    @Override
    public void sendMessage(String message) {
        handle.sendMessage(LEGACY.deserialize(message));
    }
}
