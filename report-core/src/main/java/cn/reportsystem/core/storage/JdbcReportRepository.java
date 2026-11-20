package cn.reportsystem.core.storage;

import cn.reportsystem.core.model.Evidence;
import cn.reportsystem.core.model.Handling;
import cn.reportsystem.core.model.Page;
import cn.reportsystem.core.model.PlayerRef;
import cn.reportsystem.core.model.Report;
import cn.reportsystem.core.model.ReportNote;
import cn.reportsystem.core.model.ReportQuery;
import cn.reportsystem.core.model.ReportStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JdbcReportRepository implements ReportRepository {

    private static final String COLUMNS = "id, reporter_uuid, reporter_name, target_uuid, target_name,"
            + " category, reason, server_name, world, pos_x, pos_y, pos_z, client_version, chat_snapshot,"
            + " status, handler_uuid, handler_name, handled_at, handle_note, created_at";

    private final ConnectionProvider connections;
    private final ObjectMapper mapper = new ObjectMapper();

    public JdbcReportRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public long insert(Report report) {
        String sql = "INSERT INTO rs_report (reporter_uuid, reporter_name, target_uuid, target_name,"
                + " category, reason, server_name, world, pos_x, pos_y, pos_z, client_version,"
                + " chat_snapshot, status, created_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            Evidence evidence = report.getEvidence();
            statement.setString(1, report.getReporter().getUuid().toString());
            statement.setString(2, report.getReporter().getName());
            statement.setString(3, report.getTarget().getUuid().toString());
            statement.setString(4, report.getTarget().getName());
            statement.setString(5, report.getCategory());
            statement.setString(6, report.getReason());
            statement.setString(7, evidence.getServerName());
            statement.setString(8, evidence.getWorld());
            setNullableDouble(statement, 9, evidence.getX());
            setNullableDouble(statement, 10, evidence.getY());
            setNullableDouble(statement, 11, evidence.getZ());
            statement.setString(12, evidence.getClientVersion());
            statement.setString(13, serializeChat(evidence.getChat()));
            statement.setShort(14, report.getStatus().code());
            statement.setLong(15, report.getCreatedAt());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new StorageException("插入举报后没有拿到自增主键");
        } catch (SQLException e) {
            throw new StorageException("写入举报失败", e);
        }
    }

    @Override
    public Optional<Report> findById(long id) {
        String sql = "SELECT " + COLUMNS + " FROM rs_report WHERE id = ?";
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapReport(result));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new StorageException("读取举报失败: " + id, e);
        }
    }

    @Override
    public Page<Report> query(ReportQuery query) {
        String where = query.hasStatusFilter() ? " WHERE status = ?" : "";
        String sql = "SELECT " + COLUMNS + " FROM rs_report" + where
                + " ORDER BY created_at DESC LIMIT ? OFFSET ?";
        String countSql = "SELECT COUNT(*) FROM rs_report" + where;

        try (Connection connection = connections.getConnection()) {
            long total;
            try (PreparedStatement count = connection.prepareStatement(countSql)) {
                if (query.hasStatusFilter()) {
                    count.setShort(1, query.getStatus().code());
                }
                try (ResultSet result = count.executeQuery()) {
                    total = result.next() ? result.getLong(1) : 0L;
                }
            }
            if (total == 0) {
                return Page.empty(query.getPage(), query.getPageSize());
            }

            List<Report> items = new ArrayList<Report>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                int index = 1;
                if (query.hasStatusFilter()) {
                    statement.setShort(index++, query.getStatus().code());
                }
                statement.setInt(index++, query.getPageSize());
                statement.setInt(index, query.getOffset());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        items.add(mapReport(result));
                    }
                }
            }
            return new Page<Report>(items, query.getPage(), query.getPageSize(), total);
        } catch (SQLException e) {
            throw new StorageException("查询举报列表失败", e);
        }
    }

    @Override
    public boolean claim(long id, PlayerRef handler) {
        String sql = "UPDATE rs_report SET status = ?, handler_uuid = ?, handler_name = ?"
                + " WHERE id = ? AND status = ?";
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setShort(1, ReportStatus.CLAIMED.code());
            statement.setString(2, handler.getUuid().toString());
            statement.setString(3, handler.getName());
            statement.setLong(4, id);
            statement.setShort(5, ReportStatus.PENDING.code());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new StorageException("认领举报失败: " + id, e);
        }
    }

    @Override
    public boolean resolve(long id, PlayerRef handler, String note) {
        return close(id, handler, note, ReportStatus.RESOLVED);
    }

    @Override
    public boolean reject(long id, PlayerRef handler, String note) {
        return close(id, handler, note, ReportStatus.REJECTED);
    }

    private boolean close(long id, PlayerRef handler, String note, ReportStatus target) {
        String sql = "UPDATE rs_report SET status = ?, handler_uuid = ?, handler_name = ?,"
                + " handled_at = ?, handle_note = ? WHERE id = ? AND status IN (?, ?)";
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setShort(1, target.code());
            statement.setString(2, handler.getUuid().toString());
            statement.setString(3, handler.getName());
            statement.setLong(4, System.currentTimeMillis());
            statement.setString(5, note);
            statement.setLong(6, id);
            statement.setShort(7, ReportStatus.PENDING.code());
            statement.setShort(8, ReportStatus.CLAIMED.code());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new StorageException("结案失败: " + id, e);
        }
    }

    @Override
    public long countByStatus(ReportStatus status) {
        String sql = "SELECT COUNT(*) FROM rs_report WHERE status = ?";
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setShort(1, status.code());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new StorageException("统计举报数量失败", e);
        }
    }

    @Override
    public long countByReporter(UUID reporterUuid, long sinceMillis) {
        String sql = "SELECT COUNT(*) FROM rs_report WHERE reporter_uuid = ? AND created_at >= ?";
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, reporterUuid.toString());
            statement.setLong(2, sinceMillis);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new StorageException("统计玩家举报次数失败", e);
        }
    }

    @Override
    public Optional<Long> findLatestCreatedAt(UUID reporterUuid, UUID targetUuid, long sinceMillis) {
        String sql = "SELECT MAX(created_at) FROM rs_report"
                + " WHERE reporter_uuid = ? AND target_uuid = ? AND created_at >= ?";
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, reporterUuid.toString());
            statement.setString(2, targetUuid.toString());
            statement.setLong(3, sinceMillis);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                long value = result.getLong(1);
                return result.wasNull() ? Optional.<Long>empty() : Optional.of(value);
            }
        } catch (SQLException e) {
            throw new StorageException("查询重复举报失败", e);
        }
    }

    @Override
    public void addNote(ReportNote note) {
        String sql = "INSERT INTO rs_report_note (report_id, author_uuid, author_name, content, created_at)"
                + " VALUES (?,?,?,?,?)";
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, note.getReportId());
            statement.setString(2, note.getAuthor().getUuid().toString());
            statement.setString(3, note.getAuthor().getName());
            statement.setString(4, note.getContent());
            statement.setLong(5, note.getCreatedAt());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("写入处理记录失败", e);
        }
    }

    @Override
    public List<ReportNote> findNotes(long reportId) {
        String sql = "SELECT id, report_id, author_uuid, author_name, content, created_at"
                + " FROM rs_report_note WHERE report_id = ? ORDER BY created_at ASC";
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, reportId);
            try (ResultSet result = statement.executeQuery()) {
                List<ReportNote> notes = new ArrayList<ReportNote>();
                while (result.next()) {
                    notes.add(new ReportNote(
                            result.getLong("id"),
                            result.getLong("report_id"),
                            new PlayerRef(UUID.fromString(result.getString("author_uuid")),
                                    result.getString("author_name")),
                            result.getString("content"),
                            result.getLong("created_at")));
                }
                return notes;
            }
        } catch (SQLException e) {
            throw new StorageException("读取处理记录失败", e);
        }
    }

    @Override
    public int deleteClosedBefore(long beforeMillis) {
        String sql = "DELETE FROM rs_report WHERE status IN (?, ?) AND created_at < ?";
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setShort(1, ReportStatus.RESOLVED.code());
            statement.setShort(2, ReportStatus.REJECTED.code());
            statement.setLong(3, beforeMillis);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("清理历史举报失败", e);
        }
    }

    private Report mapReport(ResultSet result) throws SQLException {
        Report report = new Report();
        report.setId(result.getLong("id"));
        report.setReporter(new PlayerRef(
                UUID.fromString(result.getString("reporter_uuid")), result.getString("reporter_name")));
        report.setTarget(new PlayerRef(
                UUID.fromString(result.getString("target_uuid")), result.getString("target_name")));
        report.setCategory(result.getString("category"));
        report.setReason(result.getString("reason"));
        report.setEvidence(new Evidence(
                result.getString("server_name"),
                result.getString("world"),
                nullableDouble(result, "pos_x"),
                nullableDouble(result, "pos_y"),
                nullableDouble(result, "pos_z"),
                result.getString("client_version"),
                deserializeChat(result.getString("chat_snapshot"))));
        report.setStatus(ReportStatus.fromCode(result.getShort("status")));
        report.setCreatedAt(result.getLong("created_at"));

        String handlerUuid = result.getString("handler_uuid");
        if (handlerUuid != null) {
            String handlerName = result.getString("handler_name");
            long handledAt = result.getLong("handled_at");
            boolean handledAtMissing = result.wasNull();
            report.setHandling(new Handling(
                    new PlayerRef(UUID.fromString(handlerUuid), handlerName),
                    handledAtMissing ? 0L : handledAt,
                    result.getString("handle_note")));
        }
        return report;
    }

    private void setNullableDouble(PreparedStatement statement, int index, Double value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.DOUBLE);
        } else {
            statement.setDouble(index, value);
        }
    }

    private Double nullableDouble(ResultSet result, String column) throws SQLException {
        double value = result.getDouble(column);
        return result.wasNull() ? null : Double.valueOf(value);
    }

    private String serializeChat(List<String> chat) {
        if (chat == null || chat.isEmpty()) {
            return null;
        }
        try {
            return mapper.writeValueAsString(chat);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private List<String> deserializeChat(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return mapper.readValue(raw, new TypeReference<List<String>>() {
            });
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }
}
