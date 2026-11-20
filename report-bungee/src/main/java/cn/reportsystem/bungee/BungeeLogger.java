package cn.reportsystem.bungee;

import cn.reportsystem.core.platform.PluginLogger;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class BungeeLogger implements PluginLogger {

    private final Logger delegate;

    public BungeeLogger(Logger delegate) {
        this.delegate = delegate;
    }

    @Override
    public void info(String message) {
        delegate.info(message);
    }

    @Override
    public void warn(String message) {
        delegate.warning(message);
    }

    @Override
    public void error(String message, Throwable cause) {
        delegate.log(Level.SEVERE, message, cause);
    }
}
