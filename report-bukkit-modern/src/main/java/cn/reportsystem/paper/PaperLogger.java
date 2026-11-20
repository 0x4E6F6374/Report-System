package cn.reportsystem.paper;

import cn.reportsystem.core.platform.PluginLogger;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class PaperLogger implements PluginLogger {

    private final Logger delegate;

    public PaperLogger(Logger delegate) {
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
