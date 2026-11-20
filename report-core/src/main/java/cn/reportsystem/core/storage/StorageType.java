package cn.reportsystem.core.storage;

public enum StorageType {

    SQLITE("sqlite"),
    MYSQL("mysql"),
    MARIADB("mariadb"),
    POSTGRESQL("postgresql");

    private final String key;

    StorageType(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static StorageType fromKey(String key) {
        for (StorageType type : values()) {
            if (type.key.equalsIgnoreCase(key)) {
                return type;
            }
        }
        throw new IllegalArgumentException("unknown storage type: " + key);
    }
}
