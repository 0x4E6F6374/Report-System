package cn.reportsystem.core.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public final class ConfigLoader {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory())
            .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final TypeReference<Map<String, String>> STRING_MAP =
            new TypeReference<Map<String, String>>() {
            };

    private ConfigLoader() {
    }

    public static <T> T readOrCreate(File file, Class<T> type) throws IOException {
        ensureParent(file);
        if (!file.exists()) {
            T defaults = instantiate(type);
            YAML.writeValue(file, defaults);
            return defaults;
        }
        return YAML.readValue(file, type);
    }

    public static <T> T read(File file, Class<T> type) throws IOException {
        return YAML.readValue(file, type);
    }

    public static Map<String, String> readStringMap(File file) throws IOException {
        return YAML.readValue(file, STRING_MAP);
    }

    public static void write(File file, Object value) throws IOException {
        ensureParent(file);
        YAML.writeValue(file, value);
    }

    private static <T> T instantiate(Class<T> type) throws IOException {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IOException("无法实例化配置类 " + type.getName(), e);
        }
    }

    private static void ensureParent(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("无法创建配置目录 " + parent.getAbsolutePath());
        }
    }
}
