package cn.reportsystem.core.locale;

import java.util.Optional;
import java.util.UUID;

public interface PlayerLocaleRepository {

    Optional<String> find(UUID player);

    void save(UUID player, String locale);
}
