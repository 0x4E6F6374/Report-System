package cn.reportsystem.core.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class ResourceExtractor {

    private static final String INDEX_PATH = "lang/index.txt";

    private ResourceExtractor() {
    }

    public static File extractIfMissing(String resourceName, File target) throws IOException {
        if (target.exists()) {
            return target;
        }
        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("无法创建目录 " + parent.getAbsolutePath());
        }
        try (InputStream stream = ResourceExtractor.class.getClassLoader()
                .getResourceAsStream(resourceName)) {
            if (stream == null) {
                throw new IOException("jar 内缺少资源 " + resourceName);
            }
            Files.copy(stream, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    /**
     * 按 lang/index.txt 释放全部语言文件。
     * jar 里无法列举目录，语言清单只能靠这份索引。
     */
    public static void extractLanguages(File langDir) throws IOException {
        if (!langDir.exists() && !langDir.mkdirs()) {
            throw new IOException("无法创建语言目录 " + langDir.getAbsolutePath());
        }
        extractIfMissing(INDEX_PATH, new File(langDir, "index.txt"));
        for (String locale : readIndex()) {
            extractIfMissing("lang/" + locale + ".yml", new File(langDir, locale + ".yml"));
        }
    }

    public static List<String> readIndex() throws IOException {
        List<String> locales = new ArrayList<String>();
        InputStream stream = ResourceExtractor.class.getClassLoader().getResourceAsStream(INDEX_PATH);
        if (stream == null) {
            return locales;
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    locales.add(trimmed);
                }
            }
        }
        return locales;
    }
}
