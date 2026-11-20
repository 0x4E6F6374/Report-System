package cn.reportsystem.pool;

import cn.reportsystem.core.storage.ConnectionProvider;
import cn.reportsystem.core.storage.StorageConfig;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public final class HikariProvider implements ConnectionProvider {

    private final HikariDataSource dataSource;

    public HikariProvider(StorageConfig config) {
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(config.buildJdbcUrl());
        hikari.setDriverClassName(config.driverClassName());
        if (config.isRemote()) {
            hikari.setUsername(config.getUsername());
            hikari.setPassword(config.getPassword());
        }
        hikari.setMaximumPoolSize(config.getPoolSize());
        hikari.setMinimumIdle(1);
        hikari.setConnectionTimeout(8000);
        hikari.setPoolName("ReportSystem");
        this.dataSource = new HikariDataSource(hikari);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
