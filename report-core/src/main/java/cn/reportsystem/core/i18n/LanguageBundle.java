package cn.reportsystem.core.i18n;

import cn.reportsystem.core.config.ConfigLoader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 管理 lang 目录下的全部语言。
 *
 * 语言清单直接扫描磁盘上的 *.yml 得到，不需要索引文件；
 * jar 内自带的语言文件由 ResourceExtractor 枚举自身条目后发现并释放。
 */
public final class LanguageBundle {

    private final Map<String, MessageService> byLocale;
    private final Map<String, Map<String, String>> rawByLocale;
    private final String defaultLocale;
    private final List<String> warnings;

    private LanguageBundle(Map<String, MessageService> byLocale,
                           Map<String, Map<String, String>> rawByLocale,
                           String defaultLocale, List<String> warnings) {
        this.byLocale = Collections.unmodifiableMap(byLocale);
        this.rawByLocale = Collections.unmodifiableMap(rawByLocale);
        this.defaultLocale = defaultLocale;
        this.warnings = Collections.unmodifiableList(warnings);
    }

    public static LanguageBundle load(File langDir, String defaultLocale) throws IOException {
        List<String> warnings = new ArrayList<String>();
        Map<String, Map<String, String>> raw = new LinkedHashMap<String, Map<String, String>>();

        for (String candidate : discover(langDir, warnings)) {
            String locale = normalize(candidate);
            if (locale.isEmpty()) {
                continue;
            }
            File file = new File(langDir, candidate + ".yml");
            if (!file.isFile()) {
                warnings.add("找不到语言文件 " + file.getName());
                continue;
            }
            try {
                Map<String, String> content = ConfigLoader.readStringMap(file);
                if (content == null || content.isEmpty()) {
                    warnings.add(file.getName() + " 没有可用内容，已跳过");
                    continue;
                }
                raw.put(locale, content);
            } catch (IOException e) {
                // 单个文件写坏不应该拖垮整个插件
                warnings.add(file.getName() + " 解析失败，已跳过: " + e.getMessage());
            }
        }

        if (raw.isEmpty()) {
            throw new IOException("lang 目录下没有可用的语言文件: " + langDir.getAbsolutePath());
        }

        String requested = normalize(defaultLocale);
        String effective = raw.containsKey(requested) ? requested : raw.keySet().iterator().next();
        if (!effective.equals(requested)) {
            warnings.add("默认语言 " + defaultLocale + " 不存在，改用 " + effective);
        }
        return build(effective, raw, warnings);
    }

    public static LanguageBundle of(String defaultLocale, Map<String, Map<String, String>> raw) {
        return build(normalize(defaultLocale), new LinkedHashMap<String, Map<String, String>>(raw),
                new ArrayList<String>());
    }

    private static LanguageBundle build(String defaultLocale, Map<String, Map<String, String>> raw,
                                        List<String> warnings) {
        String effective = raw.containsKey(defaultLocale) ? defaultLocale : raw.keySet().iterator().next();
        Map<String, String> base = raw.get(effective);

        Map<String, MessageService> byLocale = new LinkedHashMap<String, MessageService>();
        for (Map.Entry<String, Map<String, String>> entry : raw.entrySet()) {
            byLocale.put(entry.getKey(), MessageService.of(entry.getValue(), base));
        }
        return new LanguageBundle(byLocale, raw, effective, warnings);
    }

    /**
     * 语言清单直接取自磁盘上实际存在的 .yml，不再依赖索引文件。
     */
    private static List<String> discover(File langDir, List<String> warnings) {
        List<String> locales = new ArrayList<String>();
        File[] files = langDir.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (file.isFile() && name.endsWith(".yml")) {
                    locales.add(name.substring(0, name.length() - 4));
                }
            }
        }
        Collections.sort(locales);
        if (locales.isEmpty()) {
            warnings.add("lang 目录里没有 .yml 文件: " + langDir.getAbsolutePath());
        }
        return locales;
    }

    public MessageService forLocale(String locale) {
        MessageService service = byLocale.get(normalize(locale));
        return service == null ? byLocale.get(defaultLocale) : service;
    }

    public Set<String> locales() {
        return byLocale.keySet();
    }

    public String defaultLocale() {
        return defaultLocale;
    }

    public List<String> warnings() {
        return warnings;
    }

    public boolean has(String locale) {
        return byLocale.containsKey(normalize(locale));
    }

    /**
     * 与默认语言逐 key 比对，返回可直接写进日志的差异描述。
     */
    public List<String> validate() {
        List<String> problems = new ArrayList<String>();
        Map<String, String> base = rawByLocale.get(defaultLocale);
        if (base == null) {
            return problems;
        }
        Set<String> baseKeys = base.keySet();
        for (Map.Entry<String, Map<String, String>> entry : rawByLocale.entrySet()) {
            if (entry.getKey().equals(defaultLocale)) {
                continue;
            }
            Set<String> keys = entry.getValue().keySet();
            for (String key : baseKeys) {
                if (!keys.contains(key)) {
                    problems.add(entry.getKey() + " 缺少 key: " + key);
                }
            }
            for (String key : keys) {
                if (!baseKeys.contains(key)) {
                    problems.add(entry.getKey() + " 多出 key: " + key);
                }
            }
        }
        return problems;
    }

    public static String normalize(String locale) {
        if (locale == null) {
            return "";
        }
        String cleaned = locale.trim().replace('-', '_');
        if (cleaned.isEmpty()) {
            return "";
        }
        String[] parts = cleaned.split("_");
        if (parts.length == 1) {
            return parts[0].toLowerCase(Locale.ROOT);
        }
        return parts[0].toLowerCase(Locale.ROOT) + "_" + parts[1].toUpperCase(Locale.ROOT);
    }
}
