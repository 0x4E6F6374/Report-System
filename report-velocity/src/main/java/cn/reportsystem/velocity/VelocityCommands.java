package cn.reportsystem.velocity;

import cn.reportsystem.core.command.CommandContext;
import cn.reportsystem.core.command.Permissions;
import cn.reportsystem.core.i18n.Messages;
import cn.reportsystem.core.model.PlayerRef;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class VelocityCommands {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final ReportSystemVelocity plugin;
    private final ProxyServer proxy;

    public VelocityCommands(ReportSystemVelocity plugin, ProxyServer proxy) {
        this.plugin = plugin;
        this.proxy = proxy;
    }

    public void register() {
        CommandManager manager = proxy.getCommandManager();

        CommandMeta reportMeta = manager.metaBuilder("report").aliases("rep").plugin(plugin).build();
        manager.register(reportMeta, new ReportCommand());

        CommandMeta reportsMeta = manager.metaBuilder("reports").aliases("rpt").plugin(plugin).build();
        manager.register(reportsMeta, new ReportsCommand());
    }

    private void reply(CommandSource source, String text) {
        for (String line : text.split("\n")) {
            source.sendMessage(LEGACY.deserialize(line));
        }
    }

    private List<String> match(String prefix) {
        List<String> names = new ArrayList<String>();
        for (PlayerRef ref : plugin.players().onlinePlayers()) {
            if (prefix == null || ref.getName().toLowerCase(Locale.ROOT)
                    .startsWith(prefix.toLowerCase(Locale.ROOT))) {
                names.add(ref.getName());
            }
        }
        return names;
    }

    private final class ReportCommand implements SimpleCommand {

        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            if (!source.hasPermission(Permissions.USE)) {
                reply(source, plugin.messages().format(Messages.NO_PERMISSION));
                return;
            }
            String[] args = invocation.arguments();
            if (args.length > 0 && "gui".equalsIgnoreCase(args[0])) {
                reply(source, plugin.messages().format(Messages.GUI_UNSUPPORTED));
                return;
            }
            plugin.playerCommands().submit(new CommandContext(new VelocitySender(source), args));
        }

        @Override
        public List<String> suggest(Invocation invocation) {
            String[] args = invocation.arguments();
            if (args.length <= 1) {
                return match(args.length == 1 ? args[0] : null);
            }
            return Collections.emptyList();
        }

        @Override
        public boolean hasPermission(Invocation invocation) {
            return invocation.source().hasPermission(Permissions.USE);
        }
    }

    private final class ReportsCommand implements SimpleCommand {

        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            if (!source.hasPermission(Permissions.ADMIN)) {
                reply(source, plugin.messages().format(Messages.NO_PERMISSION));
                return;
            }
            String[] args = invocation.arguments();

            if (args.length > 0 && "reload".equalsIgnoreCase(args[0])) {
                if (!source.hasPermission(Permissions.RELOAD)) {
                    reply(source, plugin.messages().format(Messages.NO_PERMISSION));
                    return;
                }
                try {
                    plugin.reloadAll();
                    reply(source, plugin.messages().format(Messages.RELOADED));
                } catch (IOException e) {
                    plugin.logger().error("重载配置失败", e);
                    reply(source, plugin.messages().format(Messages.INTERNAL_ERROR));
                }
                return;
            }

            if (args.length > 0 && "tp".equalsIgnoreCase(args[0])
                    && !source.hasPermission(Permissions.TELEPORT)) {
                reply(source, plugin.messages().format(Messages.NO_PERMISSION));
                return;
            }

            plugin.adminCommands().handle(new CommandContext(new VelocitySender(source), args));
        }

        @Override
        public List<String> suggest(Invocation invocation) {
            String[] args = invocation.arguments();
            if (args.length == 0) {
                return List.of("list", "view", "claim", "resolve", "reject", "tp", "stats", "reload");
            }
            return Collections.emptyList();
        }
    }
}
