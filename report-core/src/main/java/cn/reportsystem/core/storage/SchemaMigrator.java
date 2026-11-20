package cn.reportsystem.core.storage;

import cn.reportsystem.core.platform.PluginLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * 按版本号递增执行增量脚本。rs_schema 里每一行代表一次已应用的升级，
 * 全新库从 0 开始，已有的库只补跑缺的版本。
 */
public final class SchemaMigrator {

    private static final int CURRENT_VERSION = 2;

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

            int current = readVersion(connection);
            if (current < 1) {
                apply(connection, dialect.schemaStatements());
            }
            if (current < 2) {
                apply(connection, dialect.localeStatements());
            }
            recordVersion(connection, CURRENT_VERSION);

            if (current < CURRENT_VERSION) {
                logger.info("数据库结构已从 v" + current + " 升级到 v" + CURRENT_VERSION
                        + "，方言 " + dialect.type().key());
            } else {
                logger.info("数据库结构已就绪，方言 " + dialect.type().key()
                        + "，schema v" + CURRENT_VERSION);
            }
        }
    }

    /**
     * WAL 让读写不再互斥，busy_timeout 避免并发写直接抛 SQLITE_BUSY。
     */
    private void applySqlitePragmas(Connection connection) throws SQLException {
        execute(connection, "PRAGMA journal_mode=WAL");
        execute(connection, "PRAGMA busy_timeout=5000");
    }

    private void apply(Connection connection, List<String> statements) throws SQLException {
        for (String statement : statements) {
            execute(connection, statement);
        }
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

    /**
     * rs_schema 在全新库上还不存在，这种情况按 0 处理。
     */
    private int readVersion(Connection connection) {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT MAX(version) FROM rs_schema");
             ResultSet result = query.executeQuery()) {
            if (result.next()) {
                int value = result.getInt(1);
                return result.wasNull() ? 0 : value;
            }
            return 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    private void recordVersion(Connection connection, int version) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT version FROM rs_schema WHERE version = ?")) {
            query.setInt(1, version);
            try (ResultSet result = query.executeQuery()) {
                if (result.next()) {
                    return;
                }
            }
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO rs_schema (version, applied_at) VALUES (?, ?)")) {
            insert.setInt(1, version);
            insert.setLong(2, System.currentTimeMillis());
            insert.executeUpdate();
        }
    }
}
