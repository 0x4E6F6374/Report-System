package cn.reportsystem.bukkit.legacy;

import cn.reportsystem.core.platform.Sender;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class BukkitSender implements Sender {

    private final CommandSender handle;

    public BukkitSender(CommandSender handle) {
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
        handle.sendMessage(message);
    }
}
