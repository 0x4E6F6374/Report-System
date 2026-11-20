package cn.reportsystem.core.command;

import cn.reportsystem.core.i18n.LanguageBundle;
import cn.reportsystem.core.locale.LocaleService;
import cn.reportsystem.core.locale.PlayerLocaleRepository;
import cn.reportsystem.core.platform.PluginLogger;
import cn.reportsystem.core.platform.Sender;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageCommandTest {

    private LanguageBundle bundle;
    private LocaleService locales;
    private LanguageCommand command;
    private UUID playerId;

    @BeforeEach
    void setUp() throws IOException {
        playerId = UUID.randomUUID();
        bundle = LanguageBundle.load(new File("src/main/resources/lang"), "zh_CN");
        locales = new LocaleService(new MemoryRepository(), bundle, "zh_CN", new SilentLogger());
        command = new LanguageCommand(locales);
    }

    @Test
    void switchesLanguageAndPersists() {
        FakeSender sender = new FakeSender(playerId, "Steve");

        command.handle(new CommandContext(sender, new String[]{"en_US"}));
        assertEquals("en_US", locales.localeOf(playerId));
        assertEquals("English", locales.messagesFor(playerId).raw("language-name"));

        command.handle(new CommandContext(sender, new String[]{"zh-cn"}));
        assertEquals("zh_CN", locales.localeOf(playerId));
        assertEquals("简体中文", locales.messagesFor(playerId).raw("language-name"));
    }

    @Test
    void rejectsUnknownLocaleWithoutChangingState() {
        FakeSender sender = new FakeSender(playerId, "Steve");

        command.handle(new CommandContext(sender, new String[]{"fr_FR"}));

        assertEquals("zh_CN", locales.localeOf(playerId));
        String joined = String.join("\n", sender.received);
        assertTrue(joined.contains("fr_FR"), "应提示不支持的语言");
    }

    @Test
    void printsCurrentLanguageWhenNoArgument() {
        FakeSender sender = new FakeSender(playerId, "Steve");
        command.handle(new CommandContext(sender, new String[0]));

        String joined = String.join("\n", sender.received);
        assertTrue(joined.contains("zh_CN"), "应显示当前语言代码");
    }

    @Test
    void consoleIsRejected() {
        FakeSender console = new FakeSender(null, "CONSOLE");
        command.handle(new CommandContext(console, new String[0]));

        assertFalse(console.received.isEmpty());
    }

    @Test
    void completionsComeFromBundle() {
        List<String> all = command.completions("");
        assertTrue(all.contains("list"));
        assertTrue(all.contains("zh_CN"));
        assertTrue(all.contains("en_US"));
        assertTrue(command.completions("en").contains("en_US"));
        assertFalse(command.completions("en").contains("zh_CN"));
    }

    private static final class FakeSender implements Sender {

        private final UUID uuid;
        private final String name;
        private final List<String> received = new ArrayList<String>();

        private FakeSender(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        @Override
        public boolean isPlayer() {
            return uuid != null;
        }

        @Override
        public UUID getUuid() {
            return uuid;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean hasPermission(String permission) {
            return true;
        }

        @Override
        public void sendMessage(String message) {
            received.add(message);
        }
    }

    private static final class MemoryRepository implements PlayerLocaleRepository {

        private final Map<UUID, String> store = new HashMap<UUID, String>();

        @Override
        public Optional<String> find(UUID player) {
            return Optional.ofNullable(store.get(player));
        }

        @Override
        public void save(UUID player, String locale) {
            store.put(player, locale);
        }
    }

    private static final class SilentLogger implements PluginLogger {

        @Override
        public void info(String message) {
        }

        @Override
        public void warn(String message) {
        }

        @Override
        public void error(String message, Throwable cause) {
        }
    }
}
