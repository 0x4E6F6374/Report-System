package cn.reportsystem.bungee;

import cn.reportsystem.core.command.CommandContext;
import cn.reportsystem.core.command.Permissions;
import cn.reportsystem.core.i18n.Messages;
import cn.reportsystem.core.model.PlayerRef;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class BungeeCommands {

    private final ReportSystemBungee plugin;

    public BungeeCommands(ReportSystemBungee plugin) {
        this.plugin = plugin;
    }

    public void register() {
        ProxyServer proxy = plugin.proxy();
        proxy.getPluginManager().registerCommand(plugin, new ReportCommand());
        proxy.getPluginManager().registerCommand(plugin, new ReportsCommand());
        proxy.getPluginManager().registerCommand(plugin, new LanguageCmd());
    }

    private void reply(CommandSender sender, String text) {
        for (String line : text.split("\n")) {
            sender.sendMessage(TextComponent.fromLegacyText(line));
        }
    }

    private List<String> match(String prefix) {
        List<String> names = new ArrayList<String>();
        for (PlayerRef ref : plugin.players().onlinePlayers()) {
            if (prefix.isEmpty() || ref.getName().toLowerCase(Locale.ROOT)
                    .startsWith(prefix.toLowerCase(Locale.ROOT))) {
                names.add(ref.getName());
            }
        }
        return names;
    }

    private final class ReportCommand extends Command implements TabExecutor {

        ReportCommand() {
            super("report", null, "rep");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (!sender.hasPermission(Permissions.USE)) {
                reply(sender, plugin.msgFor(sender).format(Messages.NO_PERMISSION));
                return;
            }
            if (args.length > 0 && "gui".equalsIgnoreCase(args[0])) {
                reply(sender, plugin.msgFor(sender).format(Messages.GUI_UNSUPPORTED));
                return;
            }
            plugin.playerCommands().submit(new CommandContext(new BungeeSender(sender), args));
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            if (args.length == 1) {
                return match(args[0]);
            }
            return Collections.emptyList();
        }
    }

    private final class ReportsCommand extends Command implements TabExecutor {

        ReportsCommand() {
            super("reports", null, "rpt");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (!sender.hasPermission(Permissions.ADMIN)) {
                reply(sender, plugin.msgFor(sender).format(Messages.NO_PERMISSION));
                return;
            }

            if (args.length > 0 && "reload".equalsIgnoreCase(args[0])) {
                if (!sender.hasPermission(Permissions.RELOAD)) {
                    reply(sender, plugin.msgFor(sender).format(Messages.NO_PERMISSION));
                    return;
                }
                try {
                    plugin.reloadAll();
                    reply(sender, plugin.msgFor(sender).format(Messages.RELOADED));
                } catch (IOException e) {
                    plugin.logger().error("重载配置失败", e);
                    reply(sender, plugin.msgFor(sender).format(Messages.INTERNAL_ERROR));
                }
                return;
            }

            if (args.length > 0 && "tp".equalsIgnoreCase(args[0])
                    && !sender.hasPermission(Permissions.TELEPORT)) {
                reply(sender, plugin.msgFor(sender).format(Messages.NO_PERMISSION));
                return;
            }

            plugin.adminCommands().handle(new CommandContext(new BungeeSender(sender), args));
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            if (args.length == 1) {
                List<String> subs = new ArrayList<String>();
                for (String candidate : new String[]{"list", "view", "claim", "resolve",
                        "reject", "tp", "stats", "reload"}) {
                    if (candidate.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                        subs.add(candidate);
                    }
                }
                return subs;
            }
            return Collections.emptyList();
        }
    }

    private final class LanguageCmd extends Command implements TabExecutor {

        LanguageCmd() {
            super("language", null, "lang");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            plugin.languageCommand().handle(new CommandContext(new BungeeSender(sender), args));
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            if (args.length == 1) {
                return plugin.languageCommand().completions(args[0]);
            }
            return Collections.emptyList();
        }
    }
}
