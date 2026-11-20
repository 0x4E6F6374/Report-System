package cn.reportsystem.core.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class ResourceExtractor {

    private static final String LANG_PREFIX = "lang/";
    private static final String YAML_SUFFIX = ".yml";

    /**
     * 最后手段。正常情况能从 jar 条目枚举到真实清单，只有加载器拿不到 jar 路径时才用到这里。
     */
    private static final List<String> FALLBACK_LOCALES =
            Collections.unmodifiableList(Arrays.asList("zh_CN", "en_US"));

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
     * 把 jar 内自带的语言文件释放到磁盘，已存在的跳过。
     */
    public static void extractLanguages(File langDir) throws IOException {
        if (!langDir.exists() && !langDir.mkdirs()) {
            throw new IOException("无法创建语言目录 " + langDir.getAbsolutePath());
        }
        for (String locale : discoverBundledLocales()) {
            extractIfMissing(LANG_PREFIX + locale + YAML_SUFFIX,
                    new File(langDir, locale + YAML_SUFFIX));
        }
    }

    /**
     * 找出 jar 内实际存在的语言文件。
     *
     * getResource("lang") 在打包后拿不到目录内容，所以先定位自身的 Jar 文件逐条枚举；
     * 开发环境（classes 目录）走目录分支；两者都拿不到时才退回内置列表。
     */
    public static List<String> discoverBundledLocales() {
        List<String> fromJar = scanJar();
        if (!fromJar.isEmpty()) {
            return fromJar;
        }
        List<String> fromDirectory = scanClasspathDirectory();
        if (!fromDirectory.isEmpty()) {
            return fromDirectory;
        }
        return FALLBACK_LOCALES;
    }

    private static List<String> scanJar() {
        File self = locateCodeSourceFile();
        if (self == null || !self.isFile()) {
            return Collections.emptyList();
        }

        List<String> locales = new ArrayList<String>();
        try (JarFile jar = new JarFile(self)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                collect(locales, entries.nextElement().getName());
            }
        } catch (IOException e) {
            return Collections.emptyList();
        }
        Collections.sort(locales);
        return locales;
    }

    private static List<String> scanClasspathDirectory() {
        URL url = ResourceExtractor.class.getClassLoader().getResource(LANG_PREFIX);
        if (url == null || !"file".equals(url.getProtocol())) {
            return Collections.emptyList();
        }
        File directory;
        try {
            directory = new File(url.toURI());
        } catch (URISyntaxException | IllegalArgumentException e) {
            return Collections.emptyList();
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }

        List<String> locales = new ArrayList<String>();
        for (File file : files) {
            if (file.isFile()) {
                collect(locales, LANG_PREFIX + file.getName());
            }
        }
        Collections.sort(locales);
        return locales;
    }

    private static void collect(List<String> locales, String entryName) {
        if (!entryName.startsWith(LANG_PREFIX) || !entryName.endsWith(YAML_SUFFIX)) {
            return;
        }
        String locale = entryName.substring(LANG_PREFIX.length(),
                entryName.length() - YAML_SUFFIX.length());
        // 只要 lang 下一层的文件，子目录里的不算
        if (!locale.isEmpty() && locale.indexOf('/') < 0) {
            locales.add(locale);
        }
    }

    private static File locateCodeSourceFile() {
        try {
            CodeSource source = ResourceExtractor.class.getProtectionDomain().getCodeSource();
            if (source == null || source.getLocation() == null) {
                return null;
            }
            return new File(source.getLocation().toURI());
        } catch (URISyntaxException | IllegalArgumentException | SecurityException e) {
            return null;
        }
    }
}
