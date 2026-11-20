package cn.reportsystem.core.storage;

import cn.reportsystem.core.model.Page;
import cn.reportsystem.core.model.PlayerRef;
import cn.reportsystem.core.model.Report;
import cn.reportsystem.core.model.ReportNote;
import cn.reportsystem.core.model.ReportQuery;
import cn.reportsystem.core.model.ReportStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportRepository {

    long insert(Report report);

    Optional<Report> findById(long id);

    Page<Report> query(ReportQuery query);

    /**
     * 以下三个状态流转都是条件更新，返回 false 表示记录已被别人抢先处理。
     */
    boolean claim(long id, PlayerRef handler);

    boolean resolve(long id, PlayerRef handler, String note);

    boolean reject(long id, PlayerRef handler, String note);

    long countByStatus(ReportStatus status);

    long countByReporter(UUID reporterUuid, long sinceMillis);

    Optional<Long> findLatestCreatedAt(UUID reporterUuid, UUID targetUuid, long sinceMillis);

    void addNote(ReportNote note);

    List<ReportNote> findNotes(long reportId);

    int deleteClosedBefore(long beforeMillis);
}
