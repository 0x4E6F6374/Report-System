package cn.reportsystem.bungee;

import cn.reportsystem.core.chat.ChatInput;
import cn.reportsystem.core.chat.ChatTracker;
import cn.reportsystem.core.command.AdminCommands;
import cn.reportsystem.core.command.PlayerCommands;
import cn.reportsystem.core.config.Actions;
import cn.reportsystem.core.config.Categories;
import cn.reportsystem.core.config.ConfigLoader;
import cn.reportsystem.core.config.PluginConfig;
import cn.reportsystem.core.i18n.MessageService;
import cn.reportsystem.core.notify.WebhookNotifier;
import cn.reportsystem.core.platform.EvidenceCollector;
import cn.reportsystem.core.platform.PlayerDirectory;
import cn.reportsystem.core.platform.PluginLogger;
import cn.reportsystem.core.platform.PluginScheduler;
import cn.reportsystem.core.platform.TeleportExecutor;
import cn.reportsystem.core.service.ReportService;
import cn.reportsystem.core.storage.ConnectionProvider;
import cn.reportsystem.core.storage.DialectFactory;
import cn.reportsystem.core.storage.JdbcPendingTeleportRepository;
import cn.reportsystem.core.storage.JdbcReportRepository;
import cn.reportsystem.core.storage.ReportRepository;
import cn.reportsystem.core.storage.SchemaMigrator;
import cn.reportsystem.core.storage.SqlDialect;
import cn.reportsystem.core.storage.StorageConfig;
import cn.reportsystem.core.storage.StorageType;
import cn.reportsystem.core.util.ResourceExtractor;
import cn.reportsystem.pool.HikariProvider;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ReportSystemBungee extends Plugin {

    private ProxyServer proxy;
    private PluginLogger logger;

    private PluginConfig config;
    private MessageService messages;
    private Categories categories;
    private Actions actions;
    private SqlDialect dialect;
    private ConnectionProvider connections;
    private ReportService service;
    private JdbcPendingTeleportRepository teleports;
    private PlayerDirectory players;
    private PluginScheduler scheduler;
    private ChatTracker chatTracker;
    private ChatInput chatInput;
    private PlayerCommands playerCommands;
    private AdminCommands adminCommands;
    private TeleportExecutor teleporter;

    @Override
    public void onEnable() {
        proxy = getProxy();
        logger = new BungeeLogger(getLogger());
        try {
            loadConfig();
            openStorage();
            buildRuntime();
            new BungeeCommands(this).register();
            logger.info("已启用，存储 " + config.storage.type + "，代理端标识 " + config.serverId);
        } catch (Exception e) {
            logger.error("启用失败，请检查配置后重载", e);
        }
    }

    @Override
    public void onDisable() {
        if (connections != null) {
            connections.close();
            connections = null;
        }
    }

    void reloadAll() throws IOException {
        loadConfig();
        buildRuntime();
    }

    private void loadConfig() throws IOException {
        File folder = getDataFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IOException("无法创建插件目录 " + folder.getAbsolutePath());
        }
        ResourceExtractor.extractIfMissing("config.yml", new File(folder, "config.yml"));
        ResourceExtractor.extractIfMissing("messages.yml", new File(folder, "messages.yml"));
        ResourceExtractor.extractIfMissing("categories.yml", new File(folder, "categories.yml"));
        ResourceExtractor.extractIfMissing("actions.yml", new File(folder, "actions.yml"));

        config = ConfigLoader.readOrCreate(new File(folder, "config.yml"), PluginConfig.class);
        messages = MessageService.load(new File(folder, "messages.yml"));
        categories = ConfigLoader.readOrCreate(new File(folder, "categories.yml"), Categories.class);
        actions = ConfigLoader.readOrCreate(new File(folder, "actions.yml"), Actions.class);

        List<String> problems = new ArrayList<String>();
        problems.addAll(config.validate());
        problems.addAll(categories.validate());
        if (!problems.isEmpty()) {
            throw new IOException("配置校验未通过: " + problems);
        }
    }

    private void openStorage() throws java.sql.SQLException {
        StorageType type = StorageType.fromKey(config.storage.type);
        connections = new HikariProvider(buildStorageConfig(type));
        dialect = DialectFactory.create(type);
        new SchemaMigrator(connections, dialect, logger).migrate();
    }

    private StorageConfig buildStorageConfig(StorageType type) {
        if (type == StorageType.SQLITE) {
            File file = new File(config.storage.file);
            if (!file.isAbsolute()) {
                file = new File(getDataFolder(), config.storage.file);
            }
            return new StorageConfig(type, null, 0, null, null, null,
                    file.getAbsolutePath(), 1);
        }
        return new StorageConfig(type, config.storage.host, config.storage.port,
                config.storage.database, config.storage.username, config.storage.password,
                null, config.storage.poolSize);
    }

    private void buildRuntime() {
        ReportRepository repository = new JdbcReportRepository(connections);
        teleports = new JdbcPendingTeleportRepository(connections, dialect);
        players = new BungeePlayerDirectory(proxy);
        scheduler = new BungeeScheduler(proxy, this);
        chatTracker = new ChatTracker(Math.max(config.evidence.chatLines * 2, 16));
        chatInput = new ChatInput();

        EvidenceCollector evidence = new BungeeEvidenceCollector(proxy, config.serverId, config);
        WebhookNotifier webhook = new WebhookNotifier(config.notify.webhook, logger);
        service = new ReportService(repository, config, categories, actions, logger, scheduler,
                evidence, webhook, new BungeeConsoleRunner(proxy));
        teleporter = new BungeeTeleportExecutor(proxy, teleports);
        adminCommands = new AdminCommands(service, messages, teleporter, logger);
        playerCommands = new PlayerCommands(service, messages, players, config, logger);
    }

    ProxyServer proxy() {
        return proxy;
    }

    PluginConfig config() {
        return config;
    }

    MessageService messages() {
        return messages;
    }

    Categories categories() {
        return categories;
    }

    ReportService service() {
        return service;
    }

    JdbcPendingTeleportRepository teleports() {
        return teleports;
    }

    PlayerDirectory players() {
        return players;
    }

    PluginScheduler scheduler() {
        return scheduler;
    }

    PluginLogger logger() {
        return logger;
    }

    ChatTracker chatTracker() {
        return chatTracker;
    }

    ChatInput chatInput() {
        return chatInput;
    }

    PlayerCommands playerCommands() {
        return playerCommands;
    }

    AdminCommands adminCommands() {
        return adminCommands;
    }

    TeleportExecutor teleporter() {
        return teleporter;
    }
}
