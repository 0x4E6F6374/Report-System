package cn.reportsystem.core.storage.dialect;

import cn.reportsystem.core.storage.SqlDialect;
import cn.reportsystem.core.storage.StorageType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MySqlDialect implements SqlDialect {

    private final StorageType type;

    public MySqlDialect(StorageType type) {
        this.type = type;
    }

    @Override
    public StorageType type() {
        return type;
    }

    @Override
    public List<String> schemaStatements() {
        List<String> statements = new ArrayList<String>();

        statements.add("CREATE TABLE IF NOT EXISTS rs_report ("
                + " id BIGINT NOT NULL AUTO_INCREMENT,"
                + " reporter_uuid CHAR(36) NOT NULL,"
                + " reporter_name VARCHAR(16) NOT NULL,"
                + " target_uuid CHAR(36) NOT NULL,"
                + " target_name VARCHAR(16) NOT NULL,"
                + " category VARCHAR(32) NOT NULL,"
                + " reason VARCHAR(255) NOT NULL,"
                + " server_name VARCHAR(64) NOT NULL,"
                + " world VARCHAR(64) NULL,"
                + " pos_x DOUBLE NULL,"
                + " pos_y DOUBLE NULL,"
                + " pos_z DOUBLE NULL,"
                + " client_version VARCHAR(32) NULL,"
                + " chat_snapshot TEXT NULL,"
                + " status SMALLINT NOT NULL,"
                + " handler_uuid CHAR(36) NULL,"
                + " handler_name VARCHAR(16) NULL,"
                + " handled_at BIGINT NULL,"
                + " handle_note VARCHAR(255) NULL,"
                + " created_at BIGINT NOT NULL,"
                + " PRIMARY KEY (id),"
                + " KEY idx_rs_report_status (status),"
                + " KEY idx_rs_report_created (created_at),"
                + " KEY idx_rs_report_target (target_uuid)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        statements.add("CREATE TABLE IF NOT EXISTS rs_report_note ("
                + " id BIGINT NOT NULL AUTO_INCREMENT,"
                + " report_id BIGINT NOT NULL,"
                + " author_uuid CHAR(36) NOT NULL,"
                + " author_name VARCHAR(16) NOT NULL,"
                + " content VARCHAR(255) NOT NULL,"
                + " created_at BIGINT NOT NULL,"
                + " PRIMARY KEY (id),"
                + " KEY idx_rs_note_report (report_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        statements.add("CREATE TABLE IF NOT EXISTS rs_pending_teleport ("
                + " player_uuid CHAR(36) NOT NULL,"
                + " server_name VARCHAR(64) NOT NULL,"
                + " world VARCHAR(64) NOT NULL,"
                + " x DOUBLE NOT NULL,"
                + " y DOUBLE NOT NULL,"
                + " z DOUBLE NOT NULL,"
                + " expires_at BIGINT NOT NULL,"
                + " PRIMARY KEY (player_uuid)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        statements.add("CREATE TABLE IF NOT EXISTS rs_schema ("
                + " version INT NOT NULL,"
                + " applied_at BIGINT NOT NULL,"
                + " PRIMARY KEY (version)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        return Collections.unmodifiableList(statements);
    }

    @Override
    public List<String> localeStatements() {
        List<String> statements = new ArrayList<String>();
        statements.add("CREATE TABLE IF NOT EXISTS rs_player_locale ("
                + " player_uuid CHAR(36) NOT NULL,"
                + " locale VARCHAR(16) NOT NULL,"
                + " updated_at BIGINT NOT NULL,"
                + " PRIMARY KEY (player_uuid)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        return Collections.unmodifiableList(statements);
    }

    @Override
    public String upsertPlayerLocale() {
        return "INSERT INTO rs_player_locale (player_uuid, locale, updated_at)"
                + " VALUES (?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE locale = VALUES(locale), updated_at = VALUES(updated_at)";
    }

    @Override
    public String upsertPendingTeleport() {
        return "INSERT INTO rs_pending_teleport"
                + " (player_uuid, server_name, world, x, y, z, expires_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE"
                + " server_name = VALUES(server_name),"
                + " world = VALUES(world),"
                + " x = VALUES(x),"
                + " y = VALUES(y),"
                + " z = VALUES(z),"
                + " expires_at = VALUES(expires_at)";
    }
}
