package cn.reportsystem.core.command;

import cn.reportsystem.core.config.PluginConfig;
import cn.reportsystem.core.i18n.MessageService;
import cn.reportsystem.core.i18n.Messages;
import cn.reportsystem.core.locale.LocaleService;
import cn.reportsystem.core.model.PlayerRef;
import cn.reportsystem.core.platform.PluginLogger;
import cn.reportsystem.core.platform.PlayerDirectory;
import cn.reportsystem.core.platform.Sender;
import cn.reportsystem.core.service.ReportService;
import cn.reportsystem.core.service.SubmitOutcome;
import cn.reportsystem.core.storage.StorageException;

import java.util.Optional;

public final class PlayerCommands {

    private final ReportService service;
    private final LocaleService locales;
    private final PlayerDirectory players;
    private final PluginConfig config;
    private final PluginLogger logger;

    public PlayerCommands(ReportService service, LocaleService locales, PlayerDirectory players,
                          PluginConfig config, PluginLogger logger) {
        this.service = service;
        this.locales = locales;
        this.players = players;
        this.config = config;
        this.logger = logger;
    }

    public void submit(CommandContext context) {
        Sender sender = context.sender();
        MessageService messages = locales.messagesFor(sender);

        if (!sender.isPlayer()) {
            reply(sender, messages.format(Messages.CONSOLE_NOT_ALLOWED));
            return;
        }
        if (context.length() < 2) {
            reply(sender, messages.format(Messages.USAGE_REPORT));
            return;
        }

        Optional<PlayerRef> target = players.findByName(context.arg(0));
        if (!target.isPresent()) {
            reply(sender, messages.format(Messages.PLAYER_NOT_FOUND, "player", context.arg(0)));
            return;
        }

        PlayerRef reporter = new PlayerRef(sender.getUuid(), sender.getName());
        boolean bypass = sender.hasPermission(Permissions.BYPASS_COOLDOWN);
        try {
            SubmitOutcome outcome = service.submit(reporter, target.get(),
                    config.defaultCategory, context.joinFrom(1), bypass);
            reply(sender, SubmitMessages.describe(messages, config, outcome));
        } catch (StorageException e) {
            logger.error("提交举报失败", e);
            reply(sender, messages.format(Messages.INTERNAL_ERROR));
        }
    }

    private void reply(Sender sender, String text) {
        for (String line : text.split("\n")) {
            sender.sendMessage(line);
        }
    }
}
