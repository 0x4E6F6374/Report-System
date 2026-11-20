package cn.reportsystem.core.storage.dialect;

import cn.reportsystem.core.storage.SqlDialect;
import cn.reportsystem.core.storage.StorageType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SqliteDialect implements SqlDialect {

    @Override
    public StorageType type() {
        return StorageType.SQLITE;
    }

    @Override
    public List<String> schemaStatements() {
        List<String> statements = new ArrayList<String>();

        statements.add("CREATE TABLE IF NOT EXISTS rs_report ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " reporter_uuid TEXT NOT NULL,"
                + " reporter_name TEXT NOT NULL,"
                + " target_uuid TEXT NOT NULL,"
                + " target_name TEXT NOT NULL,"
                + " category TEXT NOT NULL,"
                + " reason TEXT NOT NULL,"
                + " server_name TEXT NOT NULL,"
                + " world TEXT NULL,"
                + " pos_x REAL NULL,"
                + " pos_y REAL NULL,"
                + " pos_z REAL NULL,"
                + " client_version TEXT NULL,"
                + " chat_snapshot TEXT NULL,"
                + " status INTEGER NOT NULL,"
                + " handler_uuid TEXT NULL,"
                + " handler_name TEXT NULL,"
                + " handled_at INTEGER NULL,"
                + " handle_note TEXT NULL,"
                + " created_at INTEGER NOT NULL"
                + ")");

        statements.add("CREATE INDEX IF NOT EXISTS idx_rs_report_status ON rs_report (status)");
        statements.add("CREATE INDEX IF NOT EXISTS idx_rs_report_created ON rs_report (created_at)");
        statements.add("CREATE INDEX IF NOT EXISTS idx_rs_report_target ON rs_report (target_uuid)");

        statements.add("CREATE TABLE IF NOT EXISTS rs_report_note ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " report_id INTEGER NOT NULL,"
                + " author_uuid TEXT NOT NULL,"
                + " author_name TEXT NOT NULL,"
                + " content TEXT NOT NULL,"
                + " created_at INTEGER NOT NULL"
                + ")");

        statements.add("CREATE INDEX IF NOT EXISTS idx_rs_note_report ON rs_report_note (report_id)");

        statements.add("CREATE TABLE IF NOT EXISTS rs_pending_teleport ("
                + " player_uuid TEXT NOT NULL PRIMARY KEY,"
                + " server_name TEXT NOT NULL,"
                + " world TEXT NOT NULL,"
                + " x REAL NOT NULL,"
                + " y REAL NOT NULL,"
                + " z REAL NOT NULL,"
                + " expires_at INTEGER NOT NULL"
                + ")");

        statements.add("CREATE TABLE IF NOT EXISTS rs_schema ("
                + " version INTEGER NOT NULL PRIMARY KEY,"
                + " applied_at INTEGER NOT NULL"
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
}
