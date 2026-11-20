package cn.reportsystem.bungee;

import cn.reportsystem.core.model.PlayerRef;
import cn.reportsystem.core.platform.PlayerDirectory;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class BungeePlayerDirectory implements PlayerDirectory {

    private final ProxyServer proxy;

    public BungeePlayerDirectory(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public Optional<PlayerRef> findByName(String name) {
        ProxiedPlayer player = proxy.getPlayer(name);
        return player == null ? Optional.<PlayerRef>empty() : Optional.of(of(player));
    }

    @Override
    public Optional<PlayerRef> findOnline(UUID uuid) {
        ProxiedPlayer player = proxy.getPlayer(uuid);
        return player == null ? Optional.<PlayerRef>empty() : Optional.of(of(player));
    }

    @Override
    public boolean isOnline(UUID uuid) {
        return proxy.getPlayer(uuid) != null;
    }

    @Override
    public Collection<PlayerRef> onlinePlayers() {
        List<PlayerRef> result = new ArrayList<PlayerRef>();
        for (ProxiedPlayer player : proxy.getPlayers()) {
            result.add(of(player));
        }
        return result;
    }

    private PlayerRef of(ProxiedPlayer player) {
        return new PlayerRef(player.getUniqueId(), player.getName());
    }
}
