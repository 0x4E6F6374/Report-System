package cn.reportsystem.velocity;

import cn.reportsystem.core.model.PlayerRef;
import cn.reportsystem.core.platform.PlayerDirectory;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 代理端只能看到在线玩家，离线玩家需要玩家登录过代理才能查询。
 */
public final class VelocityPlayerDirectory implements PlayerDirectory {

    private final ProxyServer proxy;

    public VelocityPlayerDirectory(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public Optional<PlayerRef> findByName(String name) {
        Optional<Player> player = proxy.getPlayer(name);
        return player.isPresent() ? Optional.of(of(player.get())) : Optional.<PlayerRef>empty();
    }

    @Override
    public Optional<PlayerRef> findOnline(UUID uuid) {
        Optional<Player> player = proxy.getPlayer(uuid);
        return player.isPresent() ? Optional.of(of(player.get())) : Optional.<PlayerRef>empty();
    }

    @Override
    public boolean isOnline(UUID uuid) {
        return proxy.getPlayer(uuid).isPresent();
    }

    @Override
    public Collection<PlayerRef> onlinePlayers() {
        List<PlayerRef> result = new ArrayList<PlayerRef>();
        for (Player player : proxy.getAllPlayers()) {
            result.add(of(player));
        }
        return result;
    }

    private PlayerRef of(Player player) {
        return new PlayerRef(player.getUniqueId(), player.getUsername());
    }
}
