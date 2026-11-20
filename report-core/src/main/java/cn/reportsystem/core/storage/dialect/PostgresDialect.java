package cn.reportsystem.core.storage.dialect;

import cn.reportsystem.core.storage.SqlDialect;
import cn.reportsystem.core.storage.StorageType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PostgresDialect implements SqlDialect {

    @Override
    public StorageType type() {
        return StorageType.POSTGRESQL;
    }

    @Override
    public List<String> schemaStatements() {
        List<String> statements = new ArrayList<String>();

        statements.add("CREATE TABLE IF NOT EXISTS rs_report ("
                + " id BIGSERIAL PRIMARY KEY,"
                + " reporter_uuid CHAR(36) NOT NULL,"
                + " reporter_name VARCHAR(16) NOT NULL,"
                + " target_uuid CHAR(36) NOT NULL,"
                + " target_name VARCHAR(16) NOT NULL,"
                + " category VARCHAR(32) NOT NULL,"
                + " reason VARCHAR(255) NOT NULL,"
                + " server_name VARCHAR(64) NOT NULL,"
                + " world VARCHAR(64) NULL,"
                + " pos_x DOUBLE PRECISION NULL,"
                + " pos_y DOUBLE PRECISION NULL,"
                + " pos_z DOUBLE PRECISION NULL,"
                + " client_version VARCHAR(32) NULL,"
                + " chat_snapshot TEXT NULL,"
                + " status SMALLINT NOT NULL,"
                + " handler_uuid CHAR(36) NULL,"
                + " handler_name VARCHAR(16) NULL,"
                + " handled_at BIGINT NULL,"
                + " handle_note VARCHAR(255) NULL,"
                + " created_at BIGINT NOT NULL"
                + ")");

        statements.add("CREATE INDEX IF NOT EXISTS idx_rs_report_status ON rs_report (status)");
        statements.add("CREATE INDEX IF NOT EXISTS idx_rs_report_created ON rs_report (created_at)");
        statements.add("CREATE INDEX IF NOT EXISTS idx_rs_report_target ON rs_report (target_uuid)");

        statements.add("CREATE TABLE IF NOT EXISTS rs_report_note ("
                + " id BIGSERIAL PRIMARY KEY,"
                + " report_id BIGINT NOT NULL,"
                + " author_uuid CHAR(36) NOT NULL,"
                + " author_name VARCHAR(16) NOT NULL,"
                + " content VARCHAR(255) NOT NULL,"
                + " created_at BIGINT NOT NULL"
                + ")");

        statements.add("CREATE INDEX IF NOT EXISTS idx_rs_note_report ON rs_report_note (report_id)");

        statements.add("CREATE TABLE IF NOT EXISTS rs_pending_teleport ("
                + " player_uuid CHAR(36) NOT NULL PRIMARY KEY,"
                + " server_name VARCHAR(64) NOT NULL,"
                + " world VARCHAR(64) NOT NULL,"
                + " x DOUBLE PRECISION NOT NULL,"
                + " y DOUBLE PRECISION NOT NULL,"
                + " z DOUBLE PRECISION NOT NULL,"
                + " expires_at BIGINT NOT NULL"
                + ")");

        statements.add("CREATE TABLE IF NOT EXISTS rs_schema ("
                + " version INT NOT NULL PRIMARY KEY,"
                + " applied_at BIGINT NOT NULL"
                + ")");

        return Collections.unmodifiableList(statements);
    }

    @Override
    public String upsertPendingTeleport() {
        return "INSERT INTO rs_pending_teleport"
                + " (player_uuid, server_name, world, x, y, z, expires_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?)"
                + " ON CONFLICT (player_uuid) DO UPDATE SET"
                + " server_name = excluded.server_name,"
                + " world = excluded.world,"
                + " x = excluded.x,"
                + " y = excluded.y,"
                + " z = excluded.z,"
                + " expires_at = excluded.expires_at";
    }

    @Override
    public List<String> localeStatements() {
        List<String> statements = new ArrayList<String>();
        statements.add("CREATE TABLE IF NOT EXISTS rs_player_locale ("
                + " player_uuid CHAR(36) NOT NULL PRIMARY KEY,"
                + " locale VARCHAR(16) NOT NULL,"
                + " updated_at BIGINT NOT NULL"
                + ")");
        return Collections.unmodifiableList(statements);
    }

    @Override
    public String upsertPlayerLocale() {
        return "INSERT INTO rs_player_locale (player_uuid, locale, updated_at)"
                + " VALUES (?, ?, ?)"
                + " ON CONFLICT (player_uuid) DO UPDATE SET"
                + " locale = excluded.locale,"
                + " updated_at = excluded.updated_at";
    }
}
