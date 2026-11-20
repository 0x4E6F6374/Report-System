package cn.reportsystem.paper;

import cn.reportsystem.core.chat.ChatInput;
import cn.reportsystem.core.command.Permissions;
import cn.reportsystem.core.command.SubmitMessages;
import cn.reportsystem.core.i18n.Messages;
import cn.reportsystem.core.model.PlayerRef;
import cn.reportsystem.core.service.SubmitOutcome;
import cn.reportsystem.core.storage.StorageException;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class ChatListener implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final PaperPlugin plugin;

    public ChatListener(PaperPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        ChatInput.Pending pending = plugin.chatInput().take(player.getUniqueId());
        if (pending == null) {
            plugin.chatTracker().record(player.getUniqueId(), event.getMessage());
            return;
        }

        event.setCancelled(true);
        String reason = event.getMessage().trim();
        String cancelKeyword = plugin.msgFor(player).raw("gui-cancel-keyword");
        if (!cancelKeyword.isEmpty() && reason.equalsIgnoreCase(cancelKeyword)) {
            send(player, plugin.msgFor(player).format(Messages.GUI_CLOSED));
            return;
        }
        submit(player, pending, reason);
    }

    private void submit(Player player, ChatInput.Pending pending, String reason) {
        PlayerRef reporter = new PlayerRef(player.getUniqueId(), player.getName());
        boolean bypass = player.hasPermission(Permissions.BYPASS_COOLDOWN);
        try {
            SubmitOutcome outcome = plugin.service().submit(reporter, pending.getTarget(),
                    pending.getCategoryKey(), reason, bypass);
            send(player, SubmitMessages.describe(plugin.msgFor(player), plugin.config(), outcome));
        } catch (StorageException e) {
            plugin.logger().error("处理界面提交的举报失败", e);
            send(player, plugin.msgFor(player).format(Messages.INTERNAL_ERROR));
        }
    }

    private void send(final Player player, final String text) {
        plugin.scheduler().runGlobal(new Runnable() {
            @Override
            public void run() {
                for (String line : text.split("\n")) {
                    player.sendMessage(LEGACY.deserialize(line));
                }
            }
        });
    }
}
