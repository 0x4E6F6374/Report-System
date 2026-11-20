package cn.reportsystem.bukkit.legacy;

import cn.reportsystem.core.config.Categories;
import cn.reportsystem.core.i18n.MessageService;
import cn.reportsystem.core.i18n.Messages;
import cn.reportsystem.core.model.PlayerRef;
import cn.reportsystem.core.platform.PlayerDirectory;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ReportGui {

    private static final int TARGET_SIZE = 54;

    private final MessageService messages;
    private final Categories categories;
    private final PlayerDirectory players;

    public ReportGui(MessageService messages, Categories categories, PlayerDirectory players) {
        this.messages = messages;
        this.categories = categories;
        this.players = players;
    }

    public boolean openTargets(Player viewer) {
        List<PlayerRef> candidates = new ArrayList<PlayerRef>();
        for (PlayerRef ref : players.onlinePlayers()) {
            if (!ref.getUuid().equals(viewer.getUniqueId())) {
                candidates.add(ref);
            }
        }
        if (candidates.size() > TARGET_SIZE) {
            viewer.sendMessage(messages.format(Messages.GUI_TOO_MANY_TARGETS));
            return false;
        }

        GuiHolder holder = new GuiHolder(GuiHolder.Stage.TARGET, null, candidates);
        Inventory inventory = Bukkit.createInventory(holder, TARGET_SIZE,
                messages.format(Messages.GUI_TARGET_TITLE));
        holder.attach(inventory);

        for (int slot = 0; slot < candidates.size(); slot++) {
            inventory.setItem(slot, head(candidates.get(slot)));
        }
        viewer.openInventory(inventory);
        return true;
    }

    public void openCategories(Player viewer, PlayerRef target) {
        int rows = Math.min(Math.max(categories.gui.rows, 1), 6);
        GuiHolder holder = new GuiHolder(GuiHolder.Stage.CATEGORY, target, Collections.<PlayerRef>emptyList());
        Inventory inventory = Bukkit.createInventory(holder, rows * 9,
                messages.color(categories.gui.title));
        holder.attach(inventory);

        int capacity = rows * 9;
        for (Categories.Category category : categories.categories.values()) {
            if (category.slot >= 0 && category.slot < capacity) {
                inventory.setItem(category.slot, icon(category));
            }
        }
        viewer.openInventory(inventory);
    }

    private ItemStack head(PlayerRef ref) {
        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta) {
            SkullMeta skull = (SkullMeta) meta;
            skull.setOwner(ref.getName());
            skull.setDisplayName(messages.color("&f" + ref.getName()));
            skull.setLore(Collections.singletonList(messages.format(Messages.GUI_TARGET_LORE)));
            item.setItemMeta(skull);
        }
        return item;
    }

    private ItemStack icon(Categories.Category category) {
        Material material = Material.matchMaterial(category.material);
        if (material == null) {
            material = Material.PAPER;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(messages.color(category.display));
            if (category.description != null && !category.description.isEmpty()) {
                List<String> lore = new ArrayList<String>();
                for (String line : category.description) {
                    lore.add(messages.color(line));
                }
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static final class GuiHolder implements InventoryHolder {

        public enum Stage {
            TARGET,
            CATEGORY
        }

        private final Stage stage;
        private final PlayerRef target;
        private final List<PlayerRef> targets;
        private Inventory inventory;

        GuiHolder(Stage stage, PlayerRef target, List<PlayerRef> targets) {
            this.stage = stage;
            this.target = target;
            this.targets = targets;
        }

        void attach(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        public Stage getStage() {
            return stage;
        }

        public PlayerRef getTarget() {
            return target;
        }

        public PlayerRef targetAt(int slot) {
            return slot >= 0 && slot < targets.size() ? targets.get(slot) : null;
        }
    }
}
