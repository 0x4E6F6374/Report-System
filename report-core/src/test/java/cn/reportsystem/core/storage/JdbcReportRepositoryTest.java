package cn.reportsystem.core.storage;

import cn.reportsystem.core.model.Evidence;
import cn.reportsystem.core.model.Page;
import cn.reportsystem.core.model.PendingTeleport;
import cn.reportsystem.core.model.PlayerRef;
import cn.reportsystem.core.model.Report;
import cn.reportsystem.core.model.ReportQuery;
import cn.reportsystem.core.model.ReportStatus;
import cn.reportsystem.core.platform.PluginLogger;
import cn.reportsystem.core.storage.dialect.SqliteDialect;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcReportRepositoryTest {

    @TempDir
    Path tempDir;

    private ConnectionProvider provider;
    private JdbcReportRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        Class.forName("org.sqlite.JDBC");
        provider = new DirectProvider("jdbc:sqlite:" + tempDir.resolve("test.db").toAbsolutePath());
        new SchemaMigrator(provider, new SqliteDialect(), new SilentLogger()).migrate();
        repository = new JdbcReportRepository(provider);
    }

    @Test
    void insertReturnsGeneratedIdAndRoundTripsEvidence() {
        Report report = sample("Alex");
        long id = repository.insert(report);

        assertTrue(id > 0);
        Optional<Report> loaded = repository.findById(id);
        assertTrue(loaded.isPresent());

        Report stored = loaded.get();
        assertEquals("Alex", stored.getTarget().getName());
        assertEquals("cheating", stored.getCategory());
        assertEquals(ReportStatus.PENDING, stored.getStatus());
        assertEquals("survival-1", stored.getEvidence().getServerName());
        assertEquals("world", stored.getEvidence().getWorld());
        assertEquals(Double.valueOf(64.0), stored.getEvidence().getY());
        assertEquals(Arrays.asList("line1", "line2"), stored.getEvidence().getChat());
    }

    @Test
    void evidenceWithoutPositionKeepsCoordinatesNull() {
        Report report = sample("Alex");
        report.setEvidence(Evidence.ofServer("proxy-1", "1.21.11"));
        long id = repository.insert(report);

        Report stored = repository.findById(id).get();
        assertFalse(stored.getEvidence().hasPosition());
        assertEquals(null, stored.getEvidence().getX());
    }

    @Test
    void claimSucceedsOnlyOnce() {
        long id = repository.insert(sample("Alex"));
        PlayerRef first = new PlayerRef(UUID.randomUUID(), "admin1");
        PlayerRef second = new PlayerRef(UUID.randomUUID(), "admin2");

        assertTrue(repository.claim(id, first));
        assertFalse(repository.claim(id, second));
    }

    @Test
    void resolvedReportCannotBeClaimedAgain() {
        long id = repository.insert(sample("Alex"));
        PlayerRef admin = new PlayerRef(UUID.randomUUID(), "admin");

        assertTrue(repository.resolve(id, admin, "已核对录像"));
        assertFalse(repository.claim(id, admin));

        Report stored = repository.findById(id).get();
        assertEquals(ReportStatus.RESOLVED, stored.getStatus());
        assertNotNull(stored.getHandling());
        assertEquals("已核对录像", stored.getHandling().getNote());
    }

    @Test
    void queryFiltersByStatusAndCountsTotal() {
        repository.insert(sample("Alex"));
        long closed = repository.insert(sample("Steve"));
        repository.reject(closed, new PlayerRef(UUID.randomUUID(), "admin"), null);

        Page<Report> pending = repository.query(ReportQuery.of(ReportStatus.PENDING, 1, 10));
        assertEquals(1, pending.getItems().size());
        assertEquals(1L, pending.getTotal());

        Page<Report> all = repository.query(ReportQuery.of(null, 1, 10));
        assertEquals(2, all.getItems().size());
        assertEquals(2L, all.getTotal());
    }

    @Test
    void pendingTeleportIsConsumedOnce() {
        JdbcPendingTeleportRepository teleports =
                new JdbcPendingTeleportRepository(provider, new SqliteDialect());
        UUID player = UUID.randomUUID();
        teleports.save(new PendingTeleport(player, "survival-1", "world",
                12.5, 70.0, -8.25, System.currentTimeMillis() + 60_000L));

        Optional<PendingTeleport> first = teleports.consume(player);
        assertTrue(first.isPresent());
        assertEquals("survival-1", first.get().getServerName());
        assertEquals(12.5, first.get().getX(), 0.001);

        assertFalse(teleports.consume(player).isPresent());
    }

    private Report sample(String targetName) {
        Report report = new Report();
        report.setReporter(new PlayerRef(UUID.randomUUID(), "reporter"));
        report.setTarget(new PlayerRef(UUID.randomUUID(), targetName));
        report.setCategory("cheating");
        report.setReason("测试理由");
        report.setEvidence(new Evidence("survival-1", "world", 10.5, 64.0, -20.25,
                "1.21.11", Arrays.asList("line1", "line2")));
        report.setStatus(ReportStatus.PENDING);
        report.setCreatedAt(System.currentTimeMillis());
        return report;
    }

    private static final class DirectProvider implements ConnectionProvider {

        private final String url;

        private DirectProvider(String url) {
            this.url = url;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(url);
        }

        @Override
        public void close() {
        }
    }

    private static final class SilentLogger implements PluginLogger {

        @Override
        public void info(String message) {
        }

        @Override
        public void warn(String message) {
        }

        @Override
        public void error(String message, Throwable cause) {
        }
    }
}
