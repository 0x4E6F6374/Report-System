package cn.reportsystem.core.service;

import cn.reportsystem.core.config.Actions;
import cn.reportsystem.core.config.Categories;
import cn.reportsystem.core.config.PluginConfig;
import cn.reportsystem.core.model.Evidence;
import cn.reportsystem.core.model.Page;
import cn.reportsystem.core.model.PlayerRef;
import cn.reportsystem.core.model.Report;
import cn.reportsystem.core.model.ReportNote;
import cn.reportsystem.core.model.ReportQuery;
import cn.reportsystem.core.model.ReportStatus;
import cn.reportsystem.core.notify.WebhookNotifier;
import cn.reportsystem.core.platform.ConsoleCommandRunner;
import cn.reportsystem.core.platform.ConsoleCommandRunner;
import cn.reportsystem.core.platform.EvidenceCollector;
import cn.reportsystem.core.platform.PluginLogger;
import cn.reportsystem.core.platform.PluginScheduler;
import cn.reportsystem.core.storage.ReportRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ReportService {

    private final ReportRepository reports;
    private final PluginConfig config;
    private final Categories categories;
    private final Actions actions;
    private final PluginLogger logger;
    private final PluginScheduler scheduler;
    private final EvidenceCollector evidence;
    private final WebhookNotifier webhook;
    private final ConsoleCommandRunner console;

    public ReportService(ReportRepository reports,
                         PluginConfig config, Categories categories, Actions actions,
                         PluginLogger logger, PluginScheduler scheduler,
                         EvidenceCollector evidence, WebhookNotifier webhook,
                         ConsoleCommandRunner console) {
        this.reports = reports;
        this.config = config;
        this.categories = categories;
        this.actions = actions;
        this.logger = logger;
        this.scheduler = scheduler;
        this.evidence = evidence;
        this.webhook = webhook;
        this.console = console;
    }

    public SubmitOutcome submit(PlayerRef reporter, PlayerRef target, String categoryKey,
                                String reason, boolean bypassLimits) {
        if (reporter.getUuid().equals(target.getUuid())) {
            return SubmitOutcome.failed(SubmitResult.SELF);
        }

        String trimmed = reason == null ? "" : reason.trim();
        if (trimmed.isEmpty()) {
            return SubmitOutcome.failed(SubmitResult.REASON_MISSING);
        }
        if (trimmed.length() > config.limits.maxReasonLength) {
            return SubmitOutcome.failed(SubmitResult.REASON_TOO_LONG);
        }

        Categories.Category category = categories.find(categoryKey);
        if (category == null) {
            return SubmitOutcome.failed(SubmitResult.UNKNOWN_CATEGORY);
        }

        long now = System.currentTimeMillis();

        if (!bypassLimits) {
            long cooldownSince = now - config.limits.cooldownSeconds * 1000L;
            if (reports.countByReporter(reporter.getUuid(), cooldownSince) > 0) {
                return SubmitOutcome.coolingDown(config.limits.cooldownSeconds);
            }
            if (reports.countByReporter(reporter.getUuid(), startOfToday()) >= config.limits.dailyLimit) {
                return SubmitOutcome.failed(SubmitResult.DAILY_LIMIT);
            }
            long duplicateSince = now - config.limits.duplicateWindowMinutes * 60_000L;
            if (reports.findLatestCreatedAt(reporter.getUuid(), target.getUuid(), duplicateSince).isPresent()) {
                return SubmitOutcome.failed(SubmitResult.DUPLICATE);
            }
        }

        Evidence snapshot = evidence.collect(target);

        Report report = new Report();
        report.setReporter(reporter);
        report.setTarget(target);
        report.setCategory(categories.keyOf(category));
        report.setReason(trimmed);
        report.setEvidence(snapshot);
        report.setStatus(ReportStatus.PENDING);
        report.setCreatedAt(now);

        long id = reports.insert(report);
        report.setId(id);

        scheduler.runAsync(new Runnable() {
            @Override
            public void run() {
                webhook.reportCreated(report);
            }
        });
        return SubmitOutcome.ok(id);
    }

    public boolean claim(long id, PlayerRef handler) {
        return reports.claim(id, handler);
    }

    public boolean resolve(long id, PlayerRef handler, String note) {
        return close(id, handler, note, true);
    }

    public boolean reject(long id, PlayerRef handler, String note) {
        return close(id, handler, note, false);
    }

    private boolean close(long id, PlayerRef handler, String note, boolean resolved) {
        boolean updated = resolved ? reports.resolve(id, handler, note) : reports.reject(id, handler, note);
        if (!updated) {
            return false;
        }
        Optional<Report> stored = reports.findById(id);
        if (stored.isPresent()) {
            Report report = stored.get();
            if (note != null && !note.trim().isEmpty()) {
                reports.addNote(new ReportNote(0L, id, handler, note.trim(), System.currentTimeMillis()));
            }
            scheduler.runAsync(new Runnable() {
                @Override
                public void run() {
                    webhook.reportClosed(report);
                }
            });
            runActions(resolved ? actions.forResolve() : actions.forReject(), report, handler);
        }
        return true;
    }

    private void runActions(List<String> templates, Report report, PlayerRef handler) {
        for (String template : templates) {
            String command = template
                    .replace("%id%", String.valueOf(report.getId()))
                    .replace("%target%", report.getTarget().getName())
                    .replace("%reporter%", report.getReporter().getName())
                    .replace("%handler%", handler.getName())
                    .replace("%reason%", report.getReason());
            if (command.trim().isEmpty()) {
                continue;
            }
            // 命令必须回主线程执行，不同平台的调度语义不同
            final String resolved = command;
            scheduler.runGlobal(new Runnable() {
                @Override
                public void run() {
                    logger.info("执行处理动作: " + resolved);
                    console.run(resolved);
                }
            });
        }
    }

    public Page<Report> list(ReportStatus status, int page, int pageSize) {
        return reports.query(ReportQuery.of(status, page, pageSize));
    }

    public Optional<Report> find(long id) {
        return reports.findById(id);
    }

    public List<ReportNote> notes(long id) {
        return reports.findNotes(id);
    }

    public Map<ReportStatus, Long> stats() {
        Map<ReportStatus, Long> result = new EnumMap<ReportStatus, Long>(ReportStatus.class);
        for (ReportStatus status : ReportStatus.values()) {
            result.put(status, reports.countByStatus(status));
        }
        return result;
    }

    public int cleanup(int retainDays) {
        long before = System.currentTimeMillis() - retainDays * 24L * 60L * 60L * 1000L;
        return reports.deleteClosedBefore(before);
    }

    private long startOfToday() {
        return LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
