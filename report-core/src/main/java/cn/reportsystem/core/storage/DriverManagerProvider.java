package cn.reportsystem.core.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * 无池化的直连实现，供 Java 8 的 legacy 产物使用。
 */
public final class DriverManagerProvider implements ConnectionProvider {

    private final String url;
    private final Properties properties;

    public DriverManagerProvider(String url, Properties properties, String driverClassName) {
        this.url = url;
        this.properties = properties;
        try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("缺少 JDBC 驱动 " + driverClassName, e);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, properties);
    }

    @Override
    public void close() {
    }
}
