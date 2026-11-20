package cn.reportsystem.bukkit.legacy;

import cn.reportsystem.core.i18n.Messages;
import cn.reportsystem.core.model.PlayerRef;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

public final class GuiListener implements Listener {

    private final ReportSystemPlugin plugin;

    public GuiListener(ReportSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof ReportGui.GuiHolder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return;
        }
        handleClick((Player) event.getWhoClicked(), (ReportGui.GuiHolder) holder, slot);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ReportGui.GuiHolder) {
            event.setCancelled(true);
        }
    }

    private void handleClick(final Player player, ReportGui.GuiHolder holder, int slot) {
        if (holder.getStage() == ReportGui.GuiHolder.Stage.TARGET) {
            final PlayerRef target = holder.targetAt(slot);
            if (target == null) {
                return;
            }
            // 1.8 不允许在点击事件里直接 openInventory，下一 tick 再开
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    plugin.gui().openCategories(player, target);
                }
            });
            return;
        }

        String categoryKey = plugin.categories().keyBySlot(slot);
        if (categoryKey == null || holder.getTarget() == null) {
            return;
        }
        plugin.chatInput().expect(player.getUniqueId(), holder.getTarget(), categoryKey);
        player.closeInventory();
        player.sendMessage(plugin.msgFor(player).format(Messages.GUI_REASON_PROMPT));
    }
}
