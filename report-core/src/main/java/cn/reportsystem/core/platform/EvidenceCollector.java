package cn.reportsystem.core.platform;

import cn.reportsystem.core.model.Evidence;
import cn.reportsystem.core.model.PlayerRef;

public interface EvidenceCollector {

    /**
     * 采集被举报人当前的现场信息。玩家已离线时返回仅含服务器名的快照。
     */
    Evidence collect(PlayerRef target);
}
