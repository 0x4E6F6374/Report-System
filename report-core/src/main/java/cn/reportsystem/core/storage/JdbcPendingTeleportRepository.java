package cn.reportsystem.core.storage;

import cn.reportsystem.core.model.PendingTeleport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class JdbcPendingTeleportRepository {

    private final ConnectionProvider connections;
    private final SqlDialect dialect;

    public JdbcPendingTeleportRepository(ConnectionProvider connections, SqlDialect dialect) {
        this.connections = connections;
        this.dialect = dialect;
    }

    public void save(PendingTeleport pending) {
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(dialect.upsertPendingTeleport())) {
            statement.setString(1, pending.getPlayerUuid().toString());
            statement.setString(2, pending.getServerName());
            statement.setString(3, pending.getWorld());
            statement.setDouble(4, pending.getX());
            statement.setDouble(5, pending.getY());
            statement.setDouble(6, pending.getZ());
            statement.setLong(7, pending.getExpiresAt());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("写入待执行传送失败", e);
        }
    }

    /**
     * 读出即删除，避免同一条记录被重复消费。
     */
    public Optional<PendingTeleport> consume(UUID playerUuid) {
        String select = "SELECT player_uuid, server_name, world, x, y, z, expires_at"
                + " FROM rs_pending_teleport WHERE player_uuid = ?";
        String delete = "DELETE FROM rs_pending_teleport WHERE player_uuid = ?";

        try (Connection connection = connections.getConnection()) {
            connection.setAutoCommit(false);
            try {
                PendingTeleport found = null;
                try (PreparedStatement statement = connection.prepareStatement(select)) {
                    statement.setString(1, playerUuid.toString());
                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next()) {
                            found = new PendingTeleport(
                                    UUID.fromString(result.getString("player_uuid")),
                                    result.getString("server_name"),
                                    result.getString("world"),
                                    result.getDouble("x"),
                                    result.getDouble("y"),
                                    result.getDouble("z"),
                                    result.getLong("expires_at"));
                        }
                    }
                }
                if (found != null) {
                    try (PreparedStatement statement = connection.prepareStatement(delete)) {
                        statement.setString(1, playerUuid.toString());
                        statement.executeUpdate();
                    }
                }
                connection.commit();
                return Optional.ofNullable(found);
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new StorageException("读取待执行传送失败", e);
        }
    }
}
