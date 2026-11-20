package cn.reportsystem.core.model;

public enum ReportStatus {

    PENDING(0),
    CLAIMED(1),
    RESOLVED(2),
    REJECTED(3);

    private final short code;

    ReportStatus(int code) {
        this.code = (short) code;
    }

    public short code() {
        return code;
    }

    public static ReportStatus fromCode(short code) {
        for (ReportStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("unknown status code: " + code);
    }

    public boolean isOpen() {
        return this == PENDING || this == CLAIMED;
    }
}
