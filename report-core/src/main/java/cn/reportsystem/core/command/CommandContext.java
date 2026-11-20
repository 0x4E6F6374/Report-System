package cn.reportsystem.core.command;

import cn.reportsystem.core.platform.Sender;

public final class CommandContext {

    private final Sender sender;
    private final String[] args;

    public CommandContext(Sender sender, String[] args) {
        this.sender = sender;
        this.args = args == null ? new String[0] : args.clone();
    }

    public Sender sender() {
        return sender;
    }

    public int length() {
        return args.length;
    }

    public String arg(int index) {
        if (index < 0 || index >= args.length) {
            return null;
        }
        return args[index];
    }

    public String joinFrom(int index) {
        if (index >= args.length) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = index; i < args.length; i++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }

    public Long argAsLong(int index) {
        String raw = arg(index);
        if (raw == null) {
            return null;
        }
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
