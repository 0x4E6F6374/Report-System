package cn.reportsystem.core.i18n;

import cn.reportsystem.core.config.ConfigLoader;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 文案统一用 & 前缀颜色码书写，渲染后转成 § 段落符，
 * 各平台再决定直接用还是转成 Component。
 */
public final class MessageService {

    private static final String COLOR_CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRr";

    private final Map<String, String> templates;
    private final String prefix;

    private MessageService(Map<String, String> templates) {
        this.templates = Collections.unmodifiableMap(templates);
        String raw = templates.get(Messages.PREFIX);
        this.prefix = raw == null ? "" : colorize(raw);
    }

    public static MessageService load(File file) throws IOException {
        Map<String, String> raw = ConfigLoader.readStringMap(file);
        return new MessageService(new LinkedHashMap<String, String>(raw));
    }

    public static MessageService of(Map<String, String> raw) {
        return new MessageService(new LinkedHashMap<String, String>(raw));
    }

    /**
     * 把语言文件合并到基准之上，缺失的 key 自然由基准补齐。
     * 合并成单一 Map 而不是链式回退，是为了让 format/raw/has/prefix 共用同一条查找路径。
     */
    public static MessageService of(Map<String, String> primary, Map<String, String> base) {
        Map<String, String> merged = new LinkedHashMap<String, String>();
        if (base != null) {
            merged.putAll(base);
        }
        if (primary != null) {
            merged.putAll(primary);
        }
        return new MessageService(merged);
    }

    public String prefix() {
        return prefix;
    }

    /**
     * 参数按 key, value, key, value 成对传入。
     */
    public String format(String key, Object... placeholders) {
        String template = templates.get(key);
        if (template == null) {
            return colorize("missing message: " + key);
        }
        String rendered = template.replace("%prefix%", prefix);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            rendered = rendered.replace("%" + placeholders[i] + "%", String.valueOf(placeholders[i + 1]));
        }
        return colorize(rendered);
    }

    public String raw(String key) {
        String template = templates.get(key);
        return template == null ? "" : template;
    }

    /**
     * 用于渲染来自配置文件的独立文本，例如界面标题。
     */
    public String color(String rawText) {
        if (rawText == null) {
            return "";
        }
        return colorize(rawText.replace("%prefix%", prefix));
    }

    public boolean has(String key) {
        return templates.containsKey(key);
    }

    private String colorize(String input) {
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && COLOR_CODES.indexOf(chars[i + 1]) >= 0) {
                chars[i] = '\u00a7';
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }
}
