package cn.reportsystem.paper;

import cn.reportsystem.core.platform.ConsoleCommandRunner;

import org.bukkit.Bukkit;

public final class PaperConsoleRunner implements ConsoleCommandRunner {

    @Override
    public void run(String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}
