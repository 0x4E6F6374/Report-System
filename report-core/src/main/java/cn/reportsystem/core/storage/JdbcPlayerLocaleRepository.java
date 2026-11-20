package cn.reportsystem.core.storage;

import cn.reportsystem.core.locale.PlayerLocaleRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class JdbcPlayerLocaleRepository implements PlayerLocaleRepository {

    private final ConnectionProvider connections;
    private final SqlDialect dialect;

    public JdbcPlayerLocaleRepository(ConnectionProvider connections, SqlDialect dialect) {
        this.connections = connections;
        this.dialect = dialect;
    }

    @Override
    public Optional<String> find(UUID player) {
        String sql = "SELECT locale FROM rs_player_locale WHERE player_uuid = ?";
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, player.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                String value = result.getString(1);
                return value == null || value.isEmpty() ? Optional.<String>empty() : Optional.of(value);
            }
        } catch (SQLException e) {
            throw new StorageException("读取玩家语言失败", e);
        }
    }

    @Override
    public void save(UUID player, String locale) {
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(dialect.upsertPlayerLocale())) {
            statement.setString(1, player.toString());
            statement.setString(2, locale);
            statement.setLong(3, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("保存玩家语言失败", e);
        }
    }
}
