package cn.reportsystem.core.model;

public final class Handling {

    private final PlayerRef handler;
    private final long handledAt;
    private final String note;

    public Handling(PlayerRef handler, long handledAt, String note) {
        this.handler = handler;
        this.handledAt = handledAt;
        this.note = note;
    }

    public PlayerRef getHandler() {
        return handler;
    }

    public long getHandledAt() {
        return handledAt;
    }

    public String getNote() {
        return note;
    }
}
