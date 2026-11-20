package cn.reportsystem.core.command;

import cn.reportsystem.core.i18n.LanguageBundle;
import cn.reportsystem.core.i18n.MessageService;
import cn.reportsystem.core.i18n.Messages;
import cn.reportsystem.core.locale.LocaleService;
import cn.reportsystem.core.platform.Sender;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class LanguageCommand {

    private static final String LIST_ARG = "list";

    private final LocaleService locales;

    public LanguageCommand(LocaleService locales) {
        this.locales = locales;
    }

    public void handle(CommandContext context) {
        Sender sender = context.sender();
        MessageService messages = locales.messagesFor(sender);

        if (!sender.hasPermission(Permissions.LANGUAGE)) {
            reply(sender, messages.format(Messages.NO_PERMISSION));
            return;
        }
        if (!sender.isPlayer()) {
            reply(sender, messages.format(Messages.CONSOLE_NOT_ALLOWED));
            return;
        }
        if (context.length() == 0) {
            String current = locales.localeOf(sender.getUuid());
            reply(sender, messages.format(Messages.LANGUAGE_CURRENT,
                    "locale", current, "name", nameOf(current)));
            return;
        }

        String arg = context.arg(0);
        if (LIST_ARG.equalsIgnoreCase(arg)) {
            list(sender, messages);
            return;
        }

        if (!locales.setLocale(sender.getUuid(), arg)) {
            reply(sender, messages.format(Messages.LANGUAGE_UNKNOWN,
                    "locale", arg, "available", String.join(", ", locales.languages().locales())));
            return;
        }

        String normalized = LanguageBundle.normalize(arg);
        reply(sender, locales.messagesFor(sender.getUuid())
                .format(Messages.LANGUAGE_CHANGED, "name", nameOf(normalized)));
    }

    /**
     * 供各平台补全使用，候选来自语言包本身，不硬编码语言代码。
     */
    public List<String> completions(String prefix) {
        String lowered = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<String>();
        if (LIST_ARG.startsWith(lowered)) {
            result.add(LIST_ARG);
        }
        for (String locale : locales.languages().locales()) {
            if (locale.toLowerCase(Locale.ROOT).startsWith(lowered)) {
                result.add(locale);
            }
        }
        return result;
    }

    private void list(Sender sender, MessageService messages) {
        StringBuilder builder = new StringBuilder(messages.format(Messages.LANGUAGE_LIST_HEADER));
        for (String locale : locales.languages().locales()) {
            builder.append('\n').append(messages.format(Messages.LANGUAGE_LIST_ENTRY,
                    "locale", locale, "name", nameOf(locale)));
        }
        reply(sender, builder.toString());
    }

    private String nameOf(String locale) {
        return locales.languages().forLocale(locale).raw(Messages.LANGUAGE_NAME);
    }

    private void reply(Sender sender, String text) {
        for (String line : text.split("\n")) {
            sender.sendMessage(line);
        }
    }
}
