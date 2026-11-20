package cn.reportsystem.core.service;

public final class SubmitOutcome {

    private final SubmitResult result;
    private final long reportId;
    private final long cooldownSeconds;

    private SubmitOutcome(SubmitResult result, long reportId, long cooldownSeconds) {
        this.result = result;
        this.reportId = reportId;
        this.cooldownSeconds = cooldownSeconds;
    }

    public static SubmitOutcome ok(long reportId) {
        return new SubmitOutcome(SubmitResult.OK, reportId, 0L);
    }

    public static SubmitOutcome failed(SubmitResult result) {
        return new SubmitOutcome(result, 0L, 0L);
    }

    public static SubmitOutcome coolingDown(long seconds) {
        return new SubmitOutcome(SubmitResult.COOLDOWN, 0L, seconds);
    }

    public boolean isSuccess() {
        return result == SubmitResult.OK;
    }

    public SubmitResult getResult() {
        return result;
    }

    public long getReportId() {
        return reportId;
    }

    public long getCooldownSeconds() {
        return cooldownSeconds;
    }
}
