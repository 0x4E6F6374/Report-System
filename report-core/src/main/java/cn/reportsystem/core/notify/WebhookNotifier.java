package cn.reportsystem.core.notify;

import cn.reportsystem.core.config.PluginConfig;
import cn.reportsystem.core.model.Evidence;
import cn.reportsystem.core.model.Report;
import cn.reportsystem.core.platform.PluginLogger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public final class WebhookNotifier {

    private static final int TIMEOUT_MILLIS = 5000;
    private static final int COLOR_OPEN = 0xE67E22;
    private static final int COLOR_CLOSED = 0x2ECC71;

    private final PluginConfig.Webhook config;
    private final PluginLogger logger;
    private final ObjectMapper mapper = new ObjectMapper();

    public WebhookNotifier(PluginConfig.Webhook config, PluginLogger logger) {
        this.config = config;
        this.logger = logger;
    }

    public boolean isEnabled() {
        return config.enabled && config.url != null && !config.url.trim().isEmpty();
    }

    public void reportCreated(Report report) {
        if (!isEnabled() || !config.notifyOnCreate) {
            return;
        }
        post(buildPayload(report, "新举报 #" + report.getId(), COLOR_OPEN));
    }

    public void reportClosed(Report report) {
        if (!isEnabled() || !config.notifyOnClose) {
            return;
        }
        String title = "已结案 #" + report.getId();
        post(buildPayload(report, title, COLOR_CLOSED));
    }

    private String buildPayload(Report report, String title, int color) {
        ObjectNode root = mapper.createObjectNode();
        if ("discord".equalsIgnoreCase(config.format)) {
            if (config.username != null && !config.username.isEmpty()) {
                root.put("username", config.username);
            }
            ArrayNode embeds = root.putArray("embeds");
            ObjectNode embed = embeds.addObject();
            embed.put("title", title);
            embed.put("color", color);
            embed.put("timestamp", Instant.ofEpochMilli(report.getCreatedAt()).toString());
            embed.put("description", safe(report.getReason()));

            ArrayNode fields = embed.putArray("fields");
            addField(fields, "举报人", report.getReporter().getName());
            addField(fields, "被举报人", report.getTarget().getName());
            addField(fields, "类型", report.getCategory());

            Evidence evidence = report.getEvidence();
            if (evidence != null) {
                addField(fields, "服务器", safe(evidence.getServerName()));
                if (evidence.hasPosition()) {
                    String position = evidence.getWorld() + " "
                            + round(evidence.getX()) + " " + round(evidence.getY()) + " " + round(evidence.getZ());
                    addField(fields, "位置", position);
                }
            }
        } else {
            root.put("text", title + " | " + report.getReporter().getName()
                    + " -> " + report.getTarget().getName() + " | " + report.getCategory()
                    + " | " + safe(report.getReason()));
        }
        return root.toString();
    }

    private void addField(ArrayNode fields, String name, String value) {
        ObjectNode field = fields.addObject();
        field.put("name", name);
        field.put("value", value);
        field.put("inline", true);
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }

    private String round(Double value) {
        return value == null ? "-" : String.format("%.1f", value);
    }

    private void post(String json) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(config.url).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            try (OutputStream out = connection.getOutputStream()) {
                out.write(json.getBytes(StandardCharsets.UTF_8));
            }
            int code = connection.getResponseCode();
            drain(code >= 400 ? connection.getErrorStream() : connection.getInputStream());
            if (code < 200 || code >= 300) {
                logger.warn("Webhook 返回状态码 " + code);
            }
        } catch (IOException e) {
            logger.warn("Webhook 推送失败: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void drain(InputStream stream) {
        if (stream == null) {
            return;
        }
        try (InputStream in = stream) {
            byte[] buffer = new byte[512];
            while (in.read(buffer) != -1) {
                // 不关心响应体，读空即可复用连接
            }
        } catch (IOException ignored) {
            // 响应体读取失败不影响主流程
        }
    }
}
