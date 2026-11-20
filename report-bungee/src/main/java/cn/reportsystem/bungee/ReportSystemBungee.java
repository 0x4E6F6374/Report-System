package cn.reportsystem.bungee;

import cn.reportsystem.core.chat.ChatInput;
import cn.reportsystem.core.chat.ChatTracker;
import cn.reportsystem.core.command.AdminCommands;
import cn.reportsystem.core.command.LanguageCommand;
import cn.reportsystem.core.command.PlayerCommands;
import cn.reportsystem.core.config.Actions;
import cn.reportsystem.core.config.Categories;
import cn.reportsystem.core.config.ConfigLoader;
import cn.reportsystem.core.config.PluginConfig;
import cn.reportsystem.core.i18n.LanguageBundle;
import cn.reportsystem.core.i18n.MessageService;
import cn.reportsystem.core.locale.LocaleService;
import cn.reportsystem.core.locale.PlayerLocaleRepository;
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
import cn.reportsystem.core.storage.JdbcPlayerLocaleRepository;
import cn.reportsystem.core.storage.JdbcReportRepository;
import cn.reportsystem.core.storage.ReportRepository;
import cn.reportsystem.core.storage.SchemaMigrator;
import cn.reportsystem.core.storage.SqlDialect;
import cn.reportsystem.core.storage.StorageConfig;
import cn.reportsystem.core.storage.StorageType;
import cn.reportsystem.core.util.ResourceExtractor;
import cn.reportsystem.pool.HikariProvider;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ReportSystemBungee extends Plugin implements Listener {

    private ProxyServer proxy;
    private PluginLogger logger;

    private PluginConfig config;
    private LanguageBundle languages;
    private MessageService messages;
    private Categories categories;
    private Actions actions;
    private SqlDialect dialect;
    private ConnectionProvider connections;
    private ReportService service;
    private JdbcPendingTeleportRepository teleports;
    private PlayerLocaleRepository localeRepository;
    private LocaleService locales;
    private PlayerDirectory players;
    private PluginScheduler scheduler;
    private ChatTracker chatTracker;
    private ChatInput chatInput;
    private PlayerCommands playerCommands;
    private AdminCommands adminCommands;
    private LanguageCommand languageCommand;
    private TeleportExecutor teleporter;

    @Override
    public void onEnable() {
        proxy = getProxy();
        logger = new BungeeLogger(getLogger());
        try {
            loadConfig();
            openStorage();
            buildRuntime();
            proxy.getPluginManager().registerListener(this, this);
            new BungeeCommands(this).register();
            logger.info("已启用，存储 " + config.storage.type + "，语言 " + config.language
                    + "，代理端标识 " + config.serverId);
        } catch (Exception e) {
            logger.error("启用失败，请检查配置后重载", e);
        }
    }

    @Override
    public void onDisable() {
        if (locales != null) {
            locales.clear();
        }
        if (connections != null) {
            connections.close();
            connections = null;
        }
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        proxy.getScheduler().runAsync(this, new Runnable() {
            @Override
            public void run() {
                locales.refresh(uuid);
            }
        });
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        locales.forget(event.getPlayer().getUniqueId());
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
        ResourceExtractor.extractIfMissing("categories.yml", new File(folder, "categories.yml"));
        ResourceExtractor.extractIfMissing("actions.yml", new File(folder, "actions.yml"));
        ResourceExtractor.extractLanguages(new File(folder, "lang"));

        config = ConfigLoader.readOrCreate(new File(folder, "config.yml"), PluginConfig.class);
        languages = LanguageBundle.load(new File(folder, "lang"), config.language);
        messages = languages.forLocale(config.language);
        categories = ConfigLoader.readOrCreate(new File(folder, "categories.yml"), Categories.class);
        actions = ConfigLoader.readOrCreate(new File(folder, "actions.yml"), Actions.class);

        for (String warning : languages.warnings()) {
            logger.warn("语言包: " + warning);
        }

        List<String> problems = new ArrayList<String>();
        problems.addAll(config.validate());
        problems.addAll(categories.validate());
        problems.addAll(languages.validate());
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

        if (locales == null) {
            localeRepository = new JdbcPlayerLocaleRepository(connections, dialect);
            locales = new LocaleService(localeRepository, languages, config.language, logger);
        } else {
            locales.rebind(languages);
        }

        EvidenceCollector evidence = new BungeeEvidenceCollector(proxy, config.serverId, config);
        WebhookNotifier webhook = new WebhookNotifier(config.notify.webhook, logger);
        service = new ReportService(repository, config, categories, actions, logger, scheduler,
                evidence, webhook, new BungeeConsoleRunner(proxy));
        teleporter = new BungeeTeleportExecutor(proxy, teleports);
        adminCommands = new AdminCommands(service, locales, teleporter, logger);
        playerCommands = new PlayerCommands(service, locales, players, config, logger);
        languageCommand = new LanguageCommand(locales);
    }

    MessageService msgFor(CommandSender sender) {
        UUID uuid = sender instanceof ProxiedPlayer
                ? ((ProxiedPlayer) sender).getUniqueId() : null;
        return locales.messagesFor(uuid);
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

    LocaleService locales() {
        return locales;
    }

    LanguageCommand languageCommand() {
        return languageCommand;
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
