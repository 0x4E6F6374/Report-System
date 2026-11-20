package cn.reportsystem.core.model;

public final class ReportNote {

    private final long id;
    private final long reportId;
    private final PlayerRef author;
    private final String content;
    private final long createdAt;

    public ReportNote(long id, long reportId, PlayerRef author, String content, long createdAt) {
        this.id = id;
        this.reportId = reportId;
        this.author = author;
        this.content = content;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public long getReportId() {
        return reportId;
    }

    public PlayerRef getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
