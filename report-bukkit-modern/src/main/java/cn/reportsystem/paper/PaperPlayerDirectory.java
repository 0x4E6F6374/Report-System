package cn.reportsystem.paper;

import cn.reportsystem.core.model.PlayerRef;
import cn.reportsystem.core.platform.PlayerDirectory;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PaperPlayerDirectory implements PlayerDirectory {

    @Override
    public Optional<PlayerRef> findByName(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return Optional.of(of(online));
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(name);
        if (offline == null) {
            offline = Bukkit.getOfflinePlayer(name);
        }
        if (offline == null || (!offline.hasPlayedBefore() && !offline.isOnline())) {
            return Optional.empty();
        }
        String resolved = offline.getName() == null ? name : offline.getName();
        return Optional.of(new PlayerRef(offline.getUniqueId(), resolved));
    }

    @Override
    public Optional<PlayerRef> findOnline(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        return online == null ? Optional.<PlayerRef>empty() : Optional.of(of(online));
    }

    @Override
    public boolean isOnline(UUID uuid) {
        return Bukkit.getPlayer(uuid) != null;
    }

    @Override
    public Collection<PlayerRef> onlinePlayers() {
        List<PlayerRef> result = new ArrayList<PlayerRef>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            result.add(of(player));
        }
        return result;
    }

    private PlayerRef of(Player player) {
        return new PlayerRef(player.getUniqueId(), player.getName());
    }
}
