package cn.reportsystem.core.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceExtractorTest {

    @Test
    void discoversBundledLocalesWithoutIndexFile() {
        List<String> locales = ResourceExtractor.discoverBundledLocales();
        assertTrue(locales.contains("zh_CN"), "应发现 zh_CN，实际: " + locales);
        assertTrue(locales.contains("en_US"), "应发现 en_US，实际: " + locales);
        assertFalse(locales.contains("index"), "index.txt 不应被当成语言");
    }

    @Test
    void extractsLanguagesToDisk(@TempDir Path tempDir) throws IOException {
        File langDir = tempDir.resolve("lang").toFile();

        ResourceExtractor.extractLanguages(langDir);

        assertTrue(new File(langDir, "zh_CN.yml").isFile());
        assertTrue(new File(langDir, "en_US.yml").isFile());
        assertFalse(new File(langDir, "index.txt").exists(), "不应再释放 index.txt");
    }

    @Test
    void keepsExistingFileUntouched(@TempDir Path tempDir) throws IOException {
        File langDir = tempDir.resolve("lang").toFile();
        assertTrue(langDir.mkdirs());
        File existing = new File(langDir, "zh_CN.yml");
        Files.write(existing.toPath(), "prefix: \"自定义\"".getBytes("UTF-8"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);

        ResourceExtractor.extractLanguages(langDir);

        String content = new String(Files.readAllBytes(existing.toPath()), "UTF-8");
        assertTrue(content.contains("自定义"), "已存在的语言文件不应被覆盖");
    }
}
