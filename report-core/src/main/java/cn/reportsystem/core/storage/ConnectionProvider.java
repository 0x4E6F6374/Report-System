package cn.reportsystem.core.storage;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 连接来源。legacy 用 DriverManager 直连，其余平台注入 HikariCP 池。
 */
public interface ConnectionProvider extends AutoCloseable {

    Connection getConnection() throws SQLException;

    @Override
    void close();
}
