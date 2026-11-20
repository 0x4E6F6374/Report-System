package cn.reportsystem.core.storage;

import java.util.List;

/**
 * 方言之间只有自增主键、UPSERT 语法和索引声明位置有差异，
 * 其余列类型统一按 BIGINT / DOUBLE / VARCHAR / TEXT 设计。
 */
public interface SqlDialect {

    StorageType type();

    List<String> schemaStatements();

    /**
     * schema v2：玩家语言偏好。
     */
    List<String> localeStatements();

    String upsertPendingTeleport();

    String upsertPlayerLocale();
}
