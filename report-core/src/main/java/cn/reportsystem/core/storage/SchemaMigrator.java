package cn.reportsystem.core.storage;

import cn.reportsystem.core.platform.PluginLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class SchemaMigrator {

    private static final int CURRENT_VERSION = 1;

    private final ConnectionProvider connections;
    private final SqlDialect dialect;
    private final PluginLogger logger;

    public SchemaMigrator(ConnectionProvider connections, SqlDialect dialect, PluginLogger logger) {
        this.connections = connections;
        this.dialect = dialect;
        this.logger = logger;
    }

    public void migrate() throws SQLException {
        try (Connection connection = connections.getConnection()) {
            if (dialect.type() == StorageType.SQLITE) {
                applySqlitePragmas(connection);
            }
            for (String statement : dialect.schemaStatements()) {
                execute(connection, statement);
            }
            recordVersion(connection);
        }
        logger.info("数据库结构已就绪，方言 " + dialect.type().key());
    }

    /**
     * WAL 让读写不再互斥，busy_timeout 避免并发写直接抛 SQLITE_BUSY。
     */
    private void applySqlitePragmas(Connection connection) throws SQLException {
        execute(connection, "PRAGMA journal_mode=WAL");
        execute(connection, "PRAGMA busy_timeout=5000");
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            boolean hasResultSet = statement.execute(sql);
            if (hasResultSet) {
                try (ResultSet ignored = statement.getResultSet()) {
                    while (ignored != null && ignored.next()) {
                        // PRAGMA 会返回一行结果，读掉以释放游标
                    }
                }
            }
        }
    }

    private void recordVersion(Connection connection) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT version FROM rs_schema WHERE version = ?")) {
            query.setInt(1, CURRENT_VERSION);
            try (ResultSet result = query.executeQuery()) {
                if (result.next()) {
                    return;
                }
            }
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO rs_schema (version, applied_at) VALUES (?, ?)")) {
            insert.setInt(1, CURRENT_VERSION);
            insert.setLong(2, System.currentTimeMillis());
            insert.executeUpdate();
        }
    }
}
