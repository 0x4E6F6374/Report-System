package cn.reportsystem.velocity;

import cn.reportsystem.core.platform.ConsoleCommandRunner;

import com.velocitypowered.api.proxy.ProxyServer;

public final class VelocityConsoleRunner implements ConsoleCommandRunner {

    private final ProxyServer proxy;

    public VelocityConsoleRunner(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void run(String command) {
        proxy.getCommandManager().executeAsync(proxy.getConsoleCommandSource(), command);
    }
}
