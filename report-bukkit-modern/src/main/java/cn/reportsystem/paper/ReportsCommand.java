package cn.reportsystem.paper;

import cn.reportsystem.core.command.CommandContext;
import cn.reportsystem.core.command.Permissions;
import cn.reportsystem.core.i18n.Messages;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.IOException;

public final class ReportsCommand implements CommandExecutor {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final PaperPlugin plugin;

    public ReportsCommand(PaperPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(Permissions.ADMIN)) {
            sender.sendMessage(LEGACY.deserialize(plugin.messages().format(Messages.NO_PERMISSION)));
            return true;
        }

        if (args.length > 0 && "reload".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission(Permissions.RELOAD)) {
                sender.sendMessage(LEGACY.deserialize(plugin.messages().format(Messages.NO_PERMISSION)));
                return true;
            }
            try {
                plugin.reloadAll();
                sender.sendMessage(LEGACY.deserialize(plugin.messages().format(Messages.RELOADED)));
            } catch (IOException e) {
                plugin.logger().error("重载配置失败", e);
                sender.sendMessage(LEGACY.deserialize(plugin.messages().format(Messages.INTERNAL_ERROR)));
            }
            return true;
        }

        if (args.length > 0 && "tp".equalsIgnoreCase(args[0])
                && !sender.hasPermission(Permissions.TELEPORT)) {
            sender.sendMessage(LEGACY.deserialize(plugin.messages().format(Messages.NO_PERMISSION)));
            return true;
        }

        plugin.adminCommands().handle(new CommandContext(new PaperSender(sender), args));
        return true;
    }
}
