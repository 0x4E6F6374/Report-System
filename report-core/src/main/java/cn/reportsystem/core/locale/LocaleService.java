package cn.reportsystem.core.locale;

import cn.reportsystem.core.i18n.LanguageBundle;
import cn.reportsystem.core.i18n.MessageService;
import cn.reportsystem.core.platform.PluginLogger;
import cn.reportsystem.core.platform.Sender;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家语言的缓存与持久化。
 *
 * 读路径只碰内存，因为 localeOf 会被 GUI 点击、聊天事件这类主线程路径调用，
 * 一次数据库往返就是一次卡顿。库只在进服时异步预取。
 */
public final class LocaleService {

    private final PlayerLocaleRepository repository;
    private LanguageBundle languages;
    private String defaultLocale;
    private final PluginLogger logger;
    private final Map<UUID, String> cache = new ConcurrentHashMap<UUID, String>();

    public LocaleService(PlayerLocaleRepository repository, LanguageBundle languages,
                         String defaultLocale, PluginLogger logger) {
        this.repository = repository;
        this.languages = languages;
        this.defaultLocale = LanguageBundle.normalize(defaultLocale);
        this.logger = logger;
    }

    /**
     * 重载配置时替换语言包但保留已缓存的玩家选择，避免所有人都被打回默认语言。
     */
    public void rebind(LanguageBundle bundle) {
        this.languages = bundle;
        this.defaultLocale = bundle.defaultLocale();
    }

    public String localeOf(UUID uuid) {
        if (uuid == null) {
            return defaultLocale;
        }
        String cached = cache.get(uuid);
        return cached == null ? defaultLocale : cached;
    }

    public MessageService messagesFor(UUID uuid) {
        return languages.forLocale(localeOf(uuid));
    }

    public MessageService messagesFor(Sender sender) {
        return messagesFor(sender == null ? null : sender.getUuid());
    }

    public void refresh(UUID uuid) {
        if (uuid == null) {
            return;
        }
        try {
            Optional<String> stored = repository.find(uuid);
            if (stored.isPresent() && languages.has(stored.get())) {
                cache.put(uuid, LanguageBundle.normalize(stored.get()));
            } else {
                cache.remove(uuid);
            }
        } catch (RuntimeException e) {
            // 库不可用时沿用默认语言，不阻断玩家进服
            logger.warn("读取玩家语言失败: " + e.getMessage());
        }
    }

    public boolean setLocale(UUID uuid, String locale) {
        if (uuid == null || !languages.has(locale)) {
            return false;
        }
        String normalized = LanguageBundle.normalize(locale);
        cache.put(uuid, normalized);
        try {
            repository.save(uuid, normalized);
        } catch (RuntimeException e) {
            logger.warn("保存玩家语言失败，仅本次会话生效: " + e.getMessage());
        }
        return true;
    }

    public void forget(UUID uuid) {
        if (uuid != null) {
            cache.remove(uuid);
        }
    }

    public void clear() {
        cache.clear();
    }

    public LanguageBundle languages() {
        return languages;
    }

    public String defaultLocale() {
        return defaultLocale;
    }
}
