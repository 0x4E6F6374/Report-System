package cn.reportsystem.core.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class ResourceExtractor {

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
}
