package cn.reportsystem.core.i18n;

import cn.reportsystem.core.config.ConfigLoader;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageBundleTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("%([a-zA-Z0-9_-]+)%");
    private static final int LEGACY_TITLE_LIMIT = 32;

    private static File langDir() {
        return new File("src/main/resources/lang");
    }

    private static File[] yamlFiles() {
        File[] files = langDir().listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile() && file.getName().endsWith(".yml");
            }
        });
        return files == null ? new File[0] : files;
    }

    @Test
    void everyLanguageFileParses() throws IOException {
        File[] files = yamlFiles();
        assertTrue(files.length >= 2, "至少要有两份语言文件");

        for (File file : files) {
            Map<String, String> content = ConfigLoader.readStringMap(file);
            assertFalse(content.isEmpty(), file.getName() + " 解析结果为空");
            assertTrue(content.containsKey(Messages.PREFIX), file.getName() + " 缺少 prefix");
        }
    }

    @Test
    void keySetsAreIdentical() throws IOException {
        Map<String, String> zh = ConfigLoader.readStringMap(new File(langDir(), "zh_CN.yml"));
        Map<String, String> en = ConfigLoader.readStringMap(new File(langDir(), "en_US.yml"));
        assertEquals(new TreeSet<String>(zh.keySet()), new TreeSet<String>(en.keySet()));
    }

    /**
     * key 一致但占位符被翻译成别的名字，玩家会直接看到 %id% 这样的字面量。
     */
    @Test
    void placeholdersMatchPerKey() throws IOException {
        Map<String, String> zh = ConfigLoader.readStringMap(new File(langDir(), "zh_CN.yml"));
        Map<String, String> en = ConfigLoader.readStringMap(new File(langDir(), "en_US.yml"));

        for (Map.Entry<String, String> entry : zh.entrySet()) {
            String other = en.get(entry.getKey());
            assertEquals(extract(entry.getValue()), extract(other),
                    "key " + entry.getKey() + " 的占位符不一致");
        }
    }

    @Test
    void guiTitlesFitLegacyLimit() throws IOException {
        for (File file : yamlFiles()) {
            Map<String, String> content = ConfigLoader.readStringMap(file);
            String title = MessageService.of(content).format(Messages.GUI_TARGET_TITLE);
            assertTrue(title.length() <= LEGACY_TITLE_LIMIT,
                    file.getName() + " 的 gui-target-title 渲染后有 " + title.length()
                            + " 字符，超过 1.8.8 的 " + LEGACY_TITLE_LIMIT + " 字符上限");
        }
    }

    @Test
    void fallbackFillsMissingKeys() {
        Map<String, String> base = new LinkedHashMap<String, String>();
        base.put("only-in-base", "&abase");
        base.put("shared", "&abase");

        Map<String, String> primary = new LinkedHashMap<String, String>();
        primary.put("shared", "&bprimary");

        MessageService service = MessageService.of(primary, base);
        assertEquals("\u00a7bprimary", service.format("shared"));
        assertEquals("\u00a7abase", service.format("only-in-base"));
        assertTrue(service.has("only-in-base"));
    }

    @Test
    void normalizeAcceptsCommonForms() {
        assertEquals("zh_CN", LanguageBundle.normalize("zh_CN"));
        assertEquals("zh_CN", LanguageBundle.normalize("zh-cn"));
        assertEquals("en_US", LanguageBundle.normalize(" en_us "));
        assertEquals("", LanguageBundle.normalize(null));
    }

    @Test
    void bundleFallsBackToDefaultLocale() {
        Map<String, Map<String, String>> raw = new LinkedHashMap<String, Map<String, String>>();
        Map<String, String> zh = new LinkedHashMap<String, String>();
        zh.put("prefix", "&8[zh]");
        raw.put("zh_CN", zh);

        LanguageBundle bundle = LanguageBundle.of("zh_CN", raw);
        assertEquals("&8[zh]", bundle.forLocale("fr_FR").raw("prefix"));
        assertEquals("zh_CN", bundle.defaultLocale());
    }

    private Set<String> extract(String value) {
        Set<String> result = new TreeSet<String>();
        if (value == null) {
            return result;
        }
        Matcher matcher = PLACEHOLDER.matcher(value);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }
}
