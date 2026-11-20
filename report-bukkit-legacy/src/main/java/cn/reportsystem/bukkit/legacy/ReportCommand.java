package cn.reportsystem.bukkit.legacy;

import cn.reportsystem.core.command.CommandContext;
import cn.reportsystem.core.command.Permissions;
import cn.reportsystem.core.i18n.Messages;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ReportCommand implements CommandExecutor {

    private final ReportSystemPlugin plugin;

    public ReportCommand(ReportSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(Permissions.USE)) {
            sender.sendMessage(plugin.msgFor(sender).format(Messages.NO_PERMISSION));
            return true;
        }
        if (args.length > 0 && "gui".equalsIgnoreCase(args[0])) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.msgFor(sender).format(Messages.CONSOLE_NOT_ALLOWED));
                return true;
            }
            plugin.gui().openTargets((Player) sender);
            return true;
        }
        plugin.playerCommands().submit(new CommandContext(new BukkitSender(sender), args));
        return true;
    }
}
