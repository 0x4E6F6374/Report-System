package cn.reportsystem.core.model;

import java.util.Objects;
import java.util.UUID;

public final class PlayerRef {

    private final UUID uuid;
    private final String name;

    public PlayerRef(UUID uuid, String name) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.name = Objects.requireNonNull(name, "name");
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PlayerRef)) {
            return false;
        }
        return uuid.equals(((PlayerRef) other).uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
