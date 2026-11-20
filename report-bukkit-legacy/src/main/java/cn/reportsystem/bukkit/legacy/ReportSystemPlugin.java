package cn.reportsystem.bukkit.legacy;

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
import cn.reportsystem.core.storage.DriverManagerProvider;
import cn.reportsystem.core.storage.JdbcPendingTeleportRepository;
import cn.reportsystem.core.storage.JdbcPlayerLocaleRepository;
import cn.reportsystem.core.storage.JdbcReportRepository;
import cn.reportsystem.core.storage.ReportRepository;
import cn.reportsystem.core.storage.SchemaMigrator;
import cn.reportsystem.core.storage.SqlDialect;
import cn.reportsystem.core.storage.StorageConfig;
import cn.reportsystem.core.storage.StorageException;
import cn.reportsystem.core.storage.StorageType;
import cn.reportsystem.core.util.ResourceExtractor;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ReportSystemPlugin extends JavaPlugin {

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
    private PluginLogger logger;
    private ChatTracker chatTracker;
    private ChatInput chatInput;
    private ReportGui gui;
    private PlayerCommands playerCommands;
    private AdminCommands adminCommands;
    private LanguageCommand languageCommand;
    private TeleportExecutor teleporter;
    private TeleportChannelListener channelListener;

    @Override
    public void onEnable() {
        logger = new BukkitLogger(getLogger());
        try {
            saveDefaults();
            loadConfig();
            openStorage();
            buildRuntime();
            registerCommands();
            registerListeners();
            scheduleCleanup();
            logger.info("已启用，存储 " + config.storage.type + "，语言 " + config.language
                    + "，服务端标识 " + config.serverId);
        } catch (Exception e) {
            logger.error("启用失败", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (channelListener != null) {
            channelListener.unregister();
            channelListener = null;
        }
        if (locales != null) {
            locales.clear();
        }
        if (connections != null) {
            connections.close();
            connections = null;
        }
    }

    void reloadAll() throws IOException {
        loadConfig();
        buildRuntime();
    }

    private void saveDefaults() {
        saveResource("config.yml", false);
        saveResource("categories.yml", false);
        saveResource("actions.yml", false);
        try {
            ResourceExtractor.extractLanguages(new File(getDataFolder(), "lang"));
        } catch (IOException e) {
            logger.warn("释放语言文件失败: " + e.getMessage());
        }
    }

    private void loadConfig() throws IOException {
        File folder = getDataFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IOException("无法创建插件目录 " + folder.getAbsolutePath());
        }
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
        StorageConfig storageConfig = buildStorageConfig(type);
        connections = new DriverManagerProvider(storageConfig.buildJdbcUrl(),
                storageConfig.buildProperties(), storageConfig.driverClassName());
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
        players = new BukkitPlayerDirectory();
        scheduler = new BukkitSchedulerAdapter(this);
        chatTracker = new ChatTracker(Math.max(config.evidence.chatLines * 2, 16));
        chatInput = new ChatInput();

        if (locales == null) {
            localeRepository = new JdbcPlayerLocaleRepository(connections, dialect);
            locales = new LocaleService(localeRepository, languages, config.language, logger);
        } else {
            locales.rebind(languages);
        }

        gui = new ReportGui(locales, categories, players);

        EvidenceCollector evidence = new BukkitEvidenceCollector(config.serverId, config, chatTracker);
        WebhookNotifier webhook = new WebhookNotifier(config.notify.webhook, logger);
        service = new ReportService(repository, config, categories, actions, logger, scheduler,
                evidence, webhook, new BukkitConsoleRunner());
        teleporter = new BukkitTeleportExecutor(this, config.serverId, teleports);
        adminCommands = new AdminCommands(service, locales, teleporter, logger);
        playerCommands = new PlayerCommands(service, locales, players, config, logger);
        languageCommand = new LanguageCommand(locales);
    }

    private void registerCommands() {
        PluginCommand report = getCommand("report");
        if (report == null) {
            throw new IllegalStateException("plugin.yml 未声明 report 命令");
        }
        report.setExecutor(new ReportCommand(this));

        PluginCommand reports = getCommand("reports");
        if (reports == null) {
            throw new IllegalStateException("plugin.yml 未声明 reports 命令");
        }
        reports.setExecutor(new ReportsCommand(this));

        PluginCommand language = getCommand("language");
        if (language != null) {
            LanguageExecutor executor = new LanguageExecutor(this);
            language.setExecutor(executor);
            language.setTabCompleter(executor);
        }
    }

    private void registerListeners() {
        PluginManager manager = getServer().getPluginManager();
        manager.registerEvents(new GuiListener(this), this);
        manager.registerEvents(new ChatListener(this), this);
        manager.registerEvents(new PlayerSessionListener(this), this);

        channelListener = new TeleportChannelListener(this);
        channelListener.register();
    }

    private void scheduleCleanup() {
        if (!config.cleanup.enabled) {
            return;
        }
        long periodTicks = Math.max(config.cleanup.intervalHours, 1) * 60L * 60L * 20L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                try {
                    int removed = service.cleanup(config.cleanup.retainDays);
                    if (removed > 0) {
                        logger.info("已清理 " + removed + " 条历史举报");
                    }
                } catch (StorageException e) {
                    logger.warn("清理历史举报失败: " + e.getMessage());
                }
            }
        }, periodTicks, periodTicks);
    }

    MessageService msgFor(CommandSender sender) {
        UUID uuid = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
        return locales.messagesFor(uuid);
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

    ReportGui gui() {
        return gui;
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
