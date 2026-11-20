package cn.reportsystem.core.notify;

import cn.reportsystem.core.config.PluginConfig;
import cn.reportsystem.core.model.Evidence;
import cn.reportsystem.core.model.PlayerRef;
import cn.reportsystem.core.model.Report;
import cn.reportsystem.core.model.ReportStatus;
import cn.reportsystem.core.platform.PluginLogger;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookNotifierTest {

    @Test
    void sendsDiscordEmbedPayload() throws Exception {
        AtomicReference<String> body = new AtomicReference<String>();
        AtomicReference<String> contentType = new AtomicReference<String>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hook", exchange -> {
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            body.set(readAll(exchange.getRequestBody()));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();

        try {
            PluginConfig.Webhook config = new PluginConfig.Webhook();
            config.enabled = true;
            config.url = "http://127.0.0.1:" + server.getAddress().getPort() + "/hook";
            config.format = "discord";
            config.username = "ReportSystem";

            new WebhookNotifier(config, new SilentLogger()).reportCreated(sample());

            assertNotNull(body.get(), "没有收到 webhook 请求");
            assertTrue(body.get().contains("\"embeds\""), "discord 格式应包含 embeds");
            assertTrue(body.get().contains("Steve"), "载荷应包含被举报人");
            assertTrue(contentType.get() != null && contentType.get().contains("application/json"),
                    "Content-Type 应为 application/json");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void sendsPlainTextPayloadForGenericFormat() throws Exception {
        AtomicReference<String> body = new AtomicReference<String>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hook", exchange -> {
            body.set(readAll(exchange.getRequestBody()));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        try {
            PluginConfig.Webhook config = new PluginConfig.Webhook();
            config.enabled = true;
            config.url = "http://127.0.0.1:" + server.getAddress().getPort() + "/hook";
            config.format = "generic";

            new WebhookNotifier(config, new SilentLogger()).reportClosed(sample());

            assertNotNull(body.get());
            assertTrue(body.get().contains("\"text\""));
            assertTrue(!body.get().contains("embeds"));
        } finally {
            server.stop(0);
        }
    }

    private String readAll(InputStream stream) throws java.io.IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        int read;
        while ((read = stream.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private Report sample() {
        Report report = new Report();
        report.setId(7L);
        report.setReporter(new PlayerRef(UUID.randomUUID(), "Alex"));
        report.setTarget(new PlayerRef(UUID.randomUUID(), "Steve"));
        report.setCategory("cheating");
        report.setReason("飞行外挂");
        report.setEvidence(new Evidence("survival-1", "world", 10.0, 64.0, -3.0,
                "1.21.11", Arrays.asList("line")));
        report.setStatus(ReportStatus.PENDING);
        report.setCreatedAt(System.currentTimeMillis());
        return report;
    }

    private static final class SilentLogger implements PluginLogger {

        @Override
        public void info(String message) {
        }

        @Override
        public void warn(String message) {
        }

        @Override
        public void error(String message, Throwable cause) {
        }
    }
}
