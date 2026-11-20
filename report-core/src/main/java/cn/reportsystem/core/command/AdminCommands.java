package cn.reportsystem.core.command;

import cn.reportsystem.core.i18n.MessageService;
import cn.reportsystem.core.i18n.Messages;
import cn.reportsystem.core.model.Evidence;
import cn.reportsystem.core.model.Page;
import cn.reportsystem.core.model.PlayerRef;
import cn.reportsystem.core.model.Report;
import cn.reportsystem.core.model.ReportNote;
import cn.reportsystem.core.model.ReportStatus;
import cn.reportsystem.core.model.TeleportTarget;
import cn.reportsystem.core.platform.PluginLogger;
import cn.reportsystem.core.platform.Sender;
import cn.reportsystem.core.platform.TeleportExecutor;
import cn.reportsystem.core.service.ReportService;
import cn.reportsystem.core.storage.StorageException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class AdminCommands {

    private static final int PAGE_SIZE = 10;
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final ReportService service;
    private final MessageService messages;
    private final TeleportExecutor teleporter;
    private final PluginLogger logger;

    public AdminCommands(ReportService service, MessageService messages,
                         TeleportExecutor teleporter, PluginLogger logger) {
        this.service = service;
        this.messages = messages;
        this.teleporter = teleporter;
        this.logger = logger;
    }

    public void handle(CommandContext context) {
        if (context.length() == 0) {
            reply(context.sender(), messages.format(Messages.USAGE_ADMIN));
            return;
        }
        String sub = context.arg(0).toLowerCase(Locale.ROOT);
        try {
            switch (sub) {
                case "list":
                    list(context);
                    break;
                case "view":
                    view(context);
                    break;
                case "claim":
                    claim(context);
                    break;
                case "resolve":
                    close(context, true);
                    break;
                case "reject":
                    close(context, false);
                    break;
                case "tp":
                    teleport(context);
                    break;
                case "stats":
                    stats(context);
                    break;
                default:
                    reply(context.sender(), messages.format(Messages.USAGE_ADMIN));
            }
        } catch (StorageException e) {
            logger.error("执行管理命令失败", e);
            reply(context.sender(), messages.format(Messages.INTERNAL_ERROR));
        }
    }

    private void list(CommandContext context) {
        ReportStatus status = parseStatus(context.arg(1));
        int page = parsePage(context.arg(2));
        Page<Report> result = service.list(status, page, PAGE_SIZE);
        if (result.isEmpty()) {
            reply(context.sender(), messages.format(Messages.LIST_EMPTY));
            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append(messages.format(Messages.LIST_HEADER,
                "page", result.getPage(), "total", Math.max(result.getTotalPages(), 1)));
        for (Report report : result.getItems()) {
            builder.append('\n').append(messages.format(Messages.LIST_ENTRY,
                    "id", report.getId(),
                    "target", report.getTarget().getName(),
                    "category", report.getCategory(),
                    "status", statusLabel(report.getStatus()),
                    "time", TIME_FORMAT.format(Instant.ofEpochMilli(report.getCreatedAt()))));
        }
        builder.append('\n').append(messages.format(Messages.LIST_FOOTER, "total", result.getTotal()));
        reply(context.sender(), builder.toString());
    }

    private void view(CommandContext context) {
        Optional<Report> found = findById(context);
        if (!found.isPresent()) {
            return;
        }
        Report report = found.get();
        Evidence evidence = report.getEvidence();

        StringBuilder builder = new StringBuilder();
        builder.append(messages.format(Messages.VIEW_HEADER, "id", report.getId()));
        appendLine(builder, Messages.VIEW_LINE, "key", "被举报人", "value", report.getTarget().getName());
        appendLine(builder, Messages.VIEW_LINE, "key", "类型", "value", report.getCategory());
        appendLine(builder, Messages.VIEW_LINE, "key", "状态", "value", statusLabel(report.getStatus()));
        appendLine(builder, Messages.VIEW_LINE, "key", "提交时间",
                "value", TIME_FORMAT.format(Instant.ofEpochMilli(report.getCreatedAt())));
        appendLine(builder, Messages.VIEW_LINE, "key", "举报人", "value", report.getReporter().getName());
        appendLine(builder, Messages.VIEW_LINE, "key", "理由", "value", report.getReason());

        if (evidence != null) {
            appendLine(builder, Messages.VIEW_EVIDENCE,
                    "server", evidence.getServerName(),
                    "world", evidence.getWorld() == null ? "-" : evidence.getWorld());
            if (evidence.hasPosition()) {
                appendLine(builder, Messages.VIEW_LINE, "key", "坐标", "value",
                        round(evidence.getX()) + " " + round(evidence.getY()) + " " + round(evidence.getZ()));
            }
        } else {
            appendLine(builder, Messages.VIEW_NO_EVIDENCE);
        }

        if (report.getHandling() != null) {
            appendLine(builder, Messages.VIEW_LINE, "key", "处理人", "value",
                    report.getHandling().getHandler().getName());
            if (report.getHandling().getNote() != null) {
                appendLine(builder, Messages.VIEW_LINE, "key", "备注", "value",
                        report.getHandling().getNote());
            }
        }

        List<ReportNote> notes = service.notes(report.getId());
        for (ReportNote note : notes) {
            appendLine(builder, Messages.VIEW_NOTE,
                    "author", note.getAuthor().getName(),
                    "content", note.getContent(),
                    "time", TIME_FORMAT.format(Instant.ofEpochMilli(note.getCreatedAt())));
        }
        reply(context.sender(), builder.toString());
    }

    private void claim(CommandContext context) {
        Optional<Report> found = findById(context);
        if (!found.isPresent()) {
            return;
        }
        long id = found.get().getId();
        boolean claimed = service.claim(id, actorOf(context.sender()));
        reply(context.sender(), claimed
                ? messages.format(Messages.CLAIMED, "id", id)
                : messages.format(Messages.CLAIMED_BY_OTHER, "id", id));
    }

    private void close(CommandContext context, boolean resolved) {
        Optional<Report> found = findById(context);
        if (!found.isPresent()) {
            return;
        }
        long id = found.get().getId();
        if (!found.get().getStatus().isOpen()) {
            reply(context.sender(), messages.format(Messages.REPORT_ALREADY_CLOSED, "id", id));
            return;
        }
        String note = context.arg(2) == null ? "" : context.joinFrom(2);
        boolean updated = resolved
                ? service.resolve(id, actorOf(context.sender()), note)
                : service.reject(id, actorOf(context.sender()), note);
        if (!updated) {
            reply(context.sender(), messages.format(Messages.REPORT_ALREADY_CLOSED, "id", id));
            return;
        }
        reply(context.sender(), messages.format(resolved ? Messages.RESOLVED : Messages.REJECTED, "id", id));
    }

    private void teleport(CommandContext context) {
        Optional<Report> found = findById(context);
        if (!found.isPresent()) {
            return;
        }
        Evidence evidence = found.get().getEvidence();
        if (evidence == null || !evidence.hasPosition()) {
            reply(context.sender(), messages.format(Messages.TELEPORT_NO_POSITION));
            return;
        }

        TeleportTarget target = new TeleportTarget(evidence.getServerName(), evidence.getWorld(),
                evidence.getX(), evidence.getY(), evidence.getZ());
        TeleportExecutor.Result result = teleporter.teleport(actorOf(context.sender()), target);
        switch (result) {
            case STARTED:
                reply(context.sender(), messages.format(Messages.TELEPORT_STARTED,
                        "server", evidence.getServerName()));
                break;
            case SAME_SERVER:
                reply(context.sender(), messages.format(Messages.TELEPORT_SAME_SERVER));
                break;
            case QUEUED:
                reply(context.sender(), messages.format(Messages.TELEPORT_QUEUED));
                break;
            case SERVER_MISSING:
                reply(context.sender(), messages.format(Messages.TELEPORT_SERVER_MISSING,
                        "server", evidence.getServerName()));
                break;
            default:
                reply(context.sender(), messages.format(Messages.TELEPORT_NO_POSITION));
        }
    }

    private void stats(CommandContext context) {
        Map<ReportStatus, Long> counts = service.stats();
        reply(context.sender(), messages.format(Messages.STATS,
                "pending", counts.get(ReportStatus.PENDING),
                "claimed", counts.get(ReportStatus.CLAIMED),
                "resolved", counts.get(ReportStatus.RESOLVED),
                "rejected", counts.get(ReportStatus.REJECTED)));
    }

    private Optional<Report> findById(CommandContext context) {
        Long id = context.argAsLong(1);
        if (id == null) {
            reply(context.sender(), messages.format(Messages.USAGE_ADMIN));
            return Optional.empty();
        }
        Optional<Report> found = service.find(id);
        if (!found.isPresent()) {
            reply(context.sender(), messages.format(Messages.REPORT_NOT_FOUND, "id", id));
        }
        return found;
    }

    private String statusLabel(ReportStatus status) {
        switch (status) {
            case PENDING:
                return messages.format(Messages.STATUS_PENDING);
            case CLAIMED:
                return messages.format(Messages.STATUS_CLAIMED);
            case RESOLVED:
                return messages.format(Messages.STATUS_RESOLVED);
            default:
                return messages.format(Messages.STATUS_REJECTED);
        }
    }

    private ReportStatus parseStatus(String raw) {
        if (raw == null || "all".equalsIgnoreCase(raw)) {
            return null;
        }
        for (ReportStatus status : ReportStatus.values()) {
            if (status.name().equalsIgnoreCase(raw)) {
                return status;
            }
        }
        return null;
    }

    private int parsePage(String raw) {
        if (raw == null) {
            return 1;
        }
        try {
            return Math.max(Integer.parseInt(raw), 1);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private PlayerRef actorOf(Sender sender) {
        UUID uuid = sender.getUuid();
        return new PlayerRef(uuid == null ? new UUID(0L, 0L) : uuid, sender.getName());
    }

    private void appendLine(StringBuilder builder, String key, Object... placeholders) {
        builder.append('\n').append(messages.format(key, placeholders));
    }

    private String round(Double value) {
        return value == null ? "-" : String.format("%.1f", value);
    }

    private void reply(Sender sender, String text) {
        for (String line : text.split("\n")) {
            sender.sendMessage(line);
        }
    }
}
