package cn.reportsystem.core.storage;

import java.util.Properties;

public final class StorageConfig {

    private final StorageType type;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final String file;
    private final int poolSize;

    public StorageConfig(StorageType type, String host, int port, String database,
                         String username, String password, String file, int poolSize) {
        this.type = type;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.file = file;
        this.poolSize = poolSize;
    }

    public StorageType getType() {
        return type;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getFile() {
        return file;
    }

    public int getPoolSize() {
        return poolSize <= 0 ? 1 : poolSize;
    }

    public String buildJdbcUrl() {
        switch (type) {
            case SQLITE:
                return "jdbc:sqlite:" + file;
            case MYSQL:
                return "jdbc:mysql://" + host + ":" + port + "/" + database
                        + "?useSSL=false&allowPublicKeyRetrieval=true"
                        + "&characterEncoding=utf8&useUnicode=true";
            case MARIADB:
                return "jdbc:mariadb://" + host + ":" + port + "/" + database
                        + "?useSsl=false&useUnicode=true&characterEncoding=utf8";
            case POSTGRESQL:
                return "jdbc:postgresql://" + host + ":" + port + "/" + database;
            default:
                throw new IllegalStateException("unsupported storage type: " + type);
        }
    }

    public Properties buildProperties() {
        Properties properties = new Properties();
        if (type != StorageType.SQLITE) {
            properties.setProperty("user", username == null ? "" : username);
            properties.setProperty("password", password == null ? "" : password);
        }
        return properties;
    }

    public String driverClassName() {
        switch (type) {
            case SQLITE:
                return "org.sqlite.JDBC";
            case MYSQL:
                return "com.mysql.cj.jdbc.Driver";
            case MARIADB:
                return "org.mariadb.jdbc.Driver";
            case POSTGRESQL:
                return "org.postgresql.Driver";
            default:
                throw new IllegalStateException("unsupported storage type: " + type);
        }
    }

    public boolean isRemote() {
        return type != StorageType.SQLITE;
    }
}
