package cn.reportsystem.core.model;

public class Report {

    private long id;
    private PlayerRef reporter;
    private PlayerRef target;
    private String category;
    private String reason;
    private Evidence evidence;
    private ReportStatus status;
    private Handling handling;
    private long createdAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public PlayerRef getReporter() {
        return reporter;
    }

    public void setReporter(PlayerRef reporter) {
        this.reporter = reporter;
    }

    public PlayerRef getTarget() {
        return target;
    }

    public void setTarget(PlayerRef target) {
        this.target = target;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Evidence getEvidence() {
        return evidence;
    }

    public void setEvidence(Evidence evidence) {
        this.evidence = evidence;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public Handling getHandling() {
        return handling;
    }

    public void setHandling(Handling handling) {
        this.handling = handling;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isOpen() {
        return status != null && status.isOpen();
    }
}
