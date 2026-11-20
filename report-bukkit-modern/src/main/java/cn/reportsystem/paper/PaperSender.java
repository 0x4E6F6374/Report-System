package cn.reportsystem.paper;

import cn.reportsystem.core.platform.Sender;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class PaperSender implements Sender {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final CommandSender handle;

    public PaperSender(CommandSender handle) {
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
        return handle.getName();
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
