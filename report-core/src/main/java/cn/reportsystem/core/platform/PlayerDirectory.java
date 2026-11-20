package cn.reportsystem.core.platform;

import cn.reportsystem.core.model.PlayerRef;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface PlayerDirectory {

    Optional<PlayerRef> findByName(String name);

    Optional<PlayerRef> findOnline(UUID uuid);

    boolean isOnline(UUID uuid);

    Collection<PlayerRef> onlinePlayers();
}
