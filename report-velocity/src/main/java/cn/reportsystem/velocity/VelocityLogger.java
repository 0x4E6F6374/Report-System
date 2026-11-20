package cn.reportsystem.velocity;

import cn.reportsystem.core.platform.PluginLogger;

import org.slf4j.Logger;

public final class VelocityLogger implements PluginLogger {

    private final Logger delegate;

    public VelocityLogger(Logger delegate) {
        this.delegate = delegate;
    }

    @Override
    public void info(String message) {
        delegate.info(message);
    }

    @Override
    public void warn(String message) {
        delegate.warn(message);
    }

    @Override
    public void error(String message, Throwable cause) {
        delegate.error(message, cause);
    }
}
