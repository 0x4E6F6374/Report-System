package cn.reportsystem.core.chat;

import cn.reportsystem.core.model.PlayerRef;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 记录"已选好目标与分类，等待玩家在聊天栏输入理由"的中间状态。
 */
public final class ChatInput {

    private final Map<UUID, Pending> pending = new ConcurrentHashMap<UUID, Pending>();

    public void expect(UUID player, PlayerRef target, String categoryKey) {
        pending.put(player, new Pending(target, categoryKey));
    }

    public Pending take(UUID player) {
        return pending.remove(player);
    }

    public void cancel(UUID player) {
        pending.remove(player);
    }

    public boolean isWaiting(UUID player) {
        return pending.containsKey(player);
    }

    public static final class Pending {

        private final PlayerRef target;
        private final String categoryKey;

        Pending(PlayerRef target, String categoryKey) {
            this.target = target;
            this.categoryKey = categoryKey;
        }

        public PlayerRef getTarget() {
            return target;
        }

        public String getCategoryKey() {
            return categoryKey;
        }
    }
}
