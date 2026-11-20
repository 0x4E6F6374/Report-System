package cn.reportsystem.paper;

import cn.reportsystem.core.command.CommandContext;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

public final class LanguageExecutor implements CommandExecutor, TabCompleter {

    private final PaperPlugin plugin;

    public LanguageExecutor(PaperPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        plugin.languageCommand().handle(new CommandContext(new PaperSender(sender), args));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return plugin.languageCommand().completions(args[0]);
        }
        return Collections.emptyList();
    }
}
