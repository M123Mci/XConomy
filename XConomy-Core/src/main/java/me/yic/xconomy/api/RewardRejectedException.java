package me.yic.xconomy.api;

/** 已确认未提交余额变更；与提交结果未知的数据库异常严格区分。 */
public final class RewardRejectedException extends java.sql.SQLException {
    public RewardRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
