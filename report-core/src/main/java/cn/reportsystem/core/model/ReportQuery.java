package cn.reportsystem.core.model;

public final class ReportQuery {

    private final ReportStatus status;
    private final int page;
    private final int pageSize;

    public ReportQuery(ReportStatus status, int page, int pageSize) {
        this.status = status;
        this.page = page < 1 ? 1 : page;
        this.pageSize = pageSize < 1 ? 10 : pageSize;
    }

    public static ReportQuery of(ReportStatus status, int page, int pageSize) {
        return new ReportQuery(status, page, pageSize);
    }

    public ReportStatus getStatus() {
        return status;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getOffset() {
        return (page - 1) * pageSize;
    }

    public boolean hasStatusFilter() {
        return status != null;
    }
}
