package cn.reportsystem.bungee;

import cn.reportsystem.core.platform.ConsoleCommandRunner;

import net.md_5.bungee.api.ProxyServer;

public final class BungeeConsoleRunner implements ConsoleCommandRunner {

    private final ProxyServer proxy;

    public BungeeConsoleRunner(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void run(String command) {
        proxy.getPluginManager().dispatchCommand(proxy.getConsole(), command);
    }
}
