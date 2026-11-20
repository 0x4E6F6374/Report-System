package cn.reportsystem.bukkit.legacy;

import cn.reportsystem.core.command.CommandContext;
import cn.reportsystem.core.command.Permissions;
import cn.reportsystem.core.i18n.Messages;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.IOException;

public final class ReportsCommand implements CommandExecutor {

    private final ReportSystemPlugin plugin;

    public ReportsCommand(ReportSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(Permissions.ADMIN)) {
            sender.sendMessage(plugin.messages().format(Messages.NO_PERMISSION));
            return true;
        }

        if (args.length > 0 && "reload".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission(Permissions.RELOAD)) {
                sender.sendMessage(plugin.messages().format(Messages.NO_PERMISSION));
                return true;
            }
            try {
                plugin.reloadAll();
                sender.sendMessage(plugin.messages().format(Messages.RELOADED));
            } catch (IOException e) {
                plugin.logger().error("重载配置失败", e);
                sender.sendMessage(plugin.messages().format(Messages.INTERNAL_ERROR));
            }
            return true;
        }

        if (args.length > 0 && "tp".equalsIgnoreCase(args[0])
                && !sender.hasPermission(Permissions.TELEPORT)) {
            sender.sendMessage(plugin.messages().format(Messages.NO_PERMISSION));
            return true;
        }

        plugin.adminCommands().handle(new CommandContext(new BukkitSender(sender), args));
        return true;
    }
}
