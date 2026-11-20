package cn.reportsystem.core.platform;

import cn.reportsystem.core.model.PlayerRef;
import cn.reportsystem.core.model.TeleportTarget;

public interface TeleportExecutor {

    enum Result {
        STARTED,
        SAME_SERVER,
        QUEUED,
        SERVER_MISSING,
        NO_POSITION
    }

    Result teleport(PlayerRef admin, TeleportTarget target);
}
