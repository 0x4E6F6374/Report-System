package cn.reportsystem.bukkit.legacy;

import cn.reportsystem.core.platform.ConsoleCommandRunner;

import org.bukkit.Bukkit;

public final class BukkitConsoleRunner implements ConsoleCommandRunner {

    @Override
    public void run(String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}
