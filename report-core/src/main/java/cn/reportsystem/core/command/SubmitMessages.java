package cn.reportsystem.core.command;

import cn.reportsystem.core.config.PluginConfig;
import cn.reportsystem.core.i18n.MessageService;
import cn.reportsystem.core.i18n.Messages;
import cn.reportsystem.core.service.SubmitOutcome;
import cn.reportsystem.core.service.SubmitResult;

public final class SubmitMessages {

    private SubmitMessages() {
    }

    public static String describe(MessageService messages, PluginConfig config, SubmitOutcome outcome) {
        SubmitResult result = outcome.getResult();
        switch (result) {
            case OK:
                return messages.format(Messages.SUBMITTED, "id", outcome.getReportId());
            case SELF:
                return messages.format(Messages.CANNOT_REPORT_SELF);
            case COOLDOWN:
                return messages.format(Messages.COOLDOWN, "seconds", outcome.getCooldownSeconds());
            case DAILY_LIMIT:
                return messages.format(Messages.DAILY_LIMIT);
            case DUPLICATE:
                return messages.format(Messages.DUPLICATE);
            case REASON_MISSING:
                return messages.format(Messages.REASON_REQUIRED);
            case REASON_TOO_LONG:
                return messages.format(Messages.REASON_TOO_LONG, "limit", config.limits.maxReasonLength);
            default:
                return messages.format(Messages.INTERNAL_ERROR);
        }
    }
}
