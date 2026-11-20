package cn.reportsystem.core.storage;

import cn.reportsystem.core.storage.dialect.MySqlDialect;
import cn.reportsystem.core.storage.dialect.PostgresDialect;
import cn.reportsystem.core.storage.dialect.SqliteDialect;

public final class DialectFactory {

    private DialectFactory() {
    }

    public static SqlDialect create(StorageType type) {
        switch (type) {
            case MYSQL:
                return new MySqlDialect(StorageType.MYSQL);
            case MARIADB:
                return new MySqlDialect(StorageType.MARIADB);
            case SQLITE:
                return new SqliteDialect();
            case POSTGRESQL:
                return new PostgresDialect();
            default:
                throw new IllegalArgumentException("unsupported storage type: " + type);
        }
    }
}
