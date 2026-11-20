package cn.reportsystem.core.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.ArrayList;
import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class PluginConfig {

    public String serverId = "server";
    public String defaultCategory = "general";
    public String language = "zh_CN";
    public Storage storage = new Storage();
    public Limits limits = new Limits();
    public Evidence evidence = new Evidence();
    public Notify notify = new Notify();
    public Cleanup cleanup = new Cleanup();

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static final class Storage {
        public String type = "sqlite";
        public String host = "127.0.0.1";
        public int port = 3306;
        public String database = "reportsystem";
        public String username = "root";
        public String password = "";
        public String file = "reports.db";
        public int poolSize = 4;
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static final class Limits {
        public int cooldownSeconds = 60;
        public int dailyLimit = 10;
        public int duplicateWindowMinutes = 30;
        public int maxReasonLength = 200;
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static final class Evidence {
        public boolean capturePosition = true;
        public boolean captureClientVersion = true;
        public int chatLines = 10;
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static final class Notify {
        public boolean onJoinPending = true;
        public Webhook webhook = new Webhook();
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static final class Webhook {
        public boolean enabled = false;
        public String url = "";
        public String format = "discord";
        public String username = "ReportSystem";
        public boolean notifyOnCreate = true;
        public boolean notifyOnClose = true;
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static final class Cleanup {
        public boolean enabled = true;
        public int retainDays = 30;
        public int intervalHours = 12;
    }

    public List<String> validate() {
        List<String> problems = new ArrayList<String>();
        if (serverId == null || serverId.trim().isEmpty()) {
            problems.add("server-id 不能为空");
        }
        if (language == null || language.trim().isEmpty()) {
            problems.add("language 不能为空");
        }
        if (storage == null) {
            problems.add("缺少 storage 配置");
        } else if ("sqlite".equalsIgnoreCase(storage.type)
                && (storage.file == null || storage.file.trim().isEmpty())) {
            problems.add("sqlite 模式必须指定 storage.file");
        }
        if (limits != null && limits.maxReasonLength <= 0) {
            problems.add("limits.max-reason-length 必须大于 0");
        }
        if (cleanup != null && cleanup.enabled && cleanup.retainDays <= 0) {
            problems.add("cleanup.retain-days 必须大于 0");
        }
        return problems;
    }
}
