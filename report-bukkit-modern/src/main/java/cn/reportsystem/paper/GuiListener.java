package cn.reportsystem.paper;

import cn.reportsystem.core.i18n.Messages;
import cn.reportsystem.core.model.PlayerRef;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

public final class GuiListener implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final PaperPlugin plugin;

    public GuiListener(PaperPlugin plugin) {
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

    private void handleClick(Player player, ReportGui.GuiHolder holder, int slot) {
        if (holder.getStage() == ReportGui.GuiHolder.Stage.TARGET) {
            final PlayerRef target = holder.targetAt(slot);
            if (target == null) {
                return;
            }
            final Player viewer = player;
            plugin.scheduler().runGlobal(new Runnable() {
                @Override
                public void run() {
                    plugin.gui().openCategories(viewer, target);
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
        player.sendMessage(LEGACY.deserialize(plugin.msgFor(player).format(Messages.GUI_REASON_PROMPT)));
    }
}
