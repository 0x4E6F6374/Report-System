package cn.reportsystem.core.service;

public enum SubmitResult {

    OK,
    SELF,
    COOLDOWN,
    DAILY_LIMIT,
    DUPLICATE,
    REASON_TOO_LONG,
    REASON_MISSING,
    UNKNOWN_CATEGORY
}
