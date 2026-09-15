package me.yic.xconomy.data.sql;

import me.yic.xconomy.api.RewardReceipt;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/** 奖励回执和余额共用事务，网络中断时以回执查询核对，绝不假报未执行。 */
public final class RewardLedger {
    private static volatile boolean initialized;
    private RewardLedger() { }

    public static synchronized void initialize() throws SQLException {
        if (initialized) return;
        try (Connection connection = SQL.database.openDedicatedConnection(); var statement = connection.createStatement()) {
            if (me.yic.xconomy.XConomyLoad.DConfig.isMySQL()) {
                try (PreparedStatement engine = connection.prepareStatement("SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?")) {
                    engine.setString(1, SQL.tableName);
                    try (ResultSet rows = engine.executeQuery()) {
                        if (!rows.next() || !"InnoDB".equalsIgnoreCase(rows.getString(1))) throw new SQLException("奖励事务要求现用余额表为 InnoDB");
                    }
                }
            }
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + table()
                    + " (operation_id VARCHAR(36) PRIMARY KEY, receiver_id VARCHAR(36) NOT NULL,"
                    + " amount DECIMAL(30,2) NOT NULL, balance DECIMAL(30,2) NOT NULL)"
                    + (me.yic.xconomy.XConomyLoad.DConfig.isMySQL() ? " ENGINE=InnoDB" : ""));
            initialized = true;
        }
    }

    public static RewardReceipt find(UUID operation) throws SQLException {
        try (Connection connection = SQL.database.openDedicatedConnection()) {
            return find(connection, operation);
        }
    }

    private static RewardReceipt find(Connection connection, UUID operation) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement("SELECT receiver_id,amount,balance FROM " + table() + " WHERE operation_id=?")) {
            query.setString(1, operation.toString());
            try (ResultSet rows = query.executeQuery()) {
                return rows.next() ? new RewardReceipt(operation, UUID.fromString(rows.getString(1)), rows.getBigDecimal(2), rows.getBigDecimal(3)) : null;
            }
        }
    }

    public static RewardReceipt deposit(UUID operation, UUID receiver, BigDecimal amount) throws SQLException {
        try (Connection connection = SQL.database.openDedicatedConnection()) {
            connection.setAutoCommit(false);
            try {
                // 操作键先取得事务锁，重复请求不会修改余额。
                RewardReceipt existing = find(connection, operation);
                if (existing != null) return verify(existing, receiver, amount);
                try (PreparedStatement reserve = connection.prepareStatement("INSERT INTO " + table() + " (operation_id,receiver_id,amount,balance) VALUES (?,?,?,0)")) {
                    reserve.setString(1, operation.toString()); reserve.setString(2, receiver.toString()); reserve.setBigDecimal(3, amount);
                    reserve.executeUpdate();
                }
                BigDecimal previous;
                String playerName;
                try (PreparedStatement query = connection.prepareStatement("SELECT balance,player FROM " + SQL.tableName + " WHERE UID=?"
                        + (me.yic.xconomy.XConomyLoad.DConfig.isMySQL() ? " FOR UPDATE" : ""))) {
                    query.setString(1, receiver.toString());
                    try (ResultSet rows = query.executeQuery()) {
                        if (!rows.next()) throw new SQLException("奖励账户不存在");
                        previous = rows.getBigDecimal(1);
                        playerName = rows.getString(2);
                    }
                }
                try (PreparedStatement update = connection.prepareStatement("UPDATE " + SQL.tableName + " SET balance=balance+? WHERE UID=?")) {
                    update.setBigDecimal(1, amount); update.setString(2, receiver.toString());
                    if (update.executeUpdate() != 1) throw new SQLException("奖励收件人账户不存在");
                }
                BigDecimal balance;
                try (PreparedStatement query = connection.prepareStatement("SELECT balance FROM " + SQL.tableName + " WHERE UID=?")) {
                    query.setString(1, receiver.toString());
                    try (ResultSet rows = query.executeQuery()) {
                        if (!rows.next()) throw new SQLException("奖励账户不存在");
                        balance = rows.getBigDecimal(1);
                        if (balance.compareTo(previous.add(amount)) != 0) throw new SQLException("现有余额字段无法精确保存该奖励，事务已拒绝");
                        if (me.yic.xconomy.data.DataFormat.isMAX(balance)) throw new SQLException("奖励超出余额上限");
                    }
                }
                try (PreparedStatement update = connection.prepareStatement("UPDATE " + table() + " SET balance=? WHERE operation_id=?")) {
                    update.setBigDecimal(1, balance); update.setString(2, operation.toString()); update.executeUpdate();
                }
                SQL.recordConfirmed(connection, new me.yic.xconomy.data.syncdata.PlayerData(receiver, playerName, balance),
                        true, amount, balance, new me.yic.xconomy.info.RecordInfo("PLUGIN_API", operation.toString(), "DurableReward"));
                connection.commit();
                return new RewardReceipt(operation, receiver, amount, balance);
            } catch (SQLException failure) {
                try { connection.rollback(); } catch (SQLException rollback) { failure.addSuppressed(rollback); }
                throw failure;
            } finally {
                connection.rollback();
            }
        } catch (SQLException failure) {
            // 先归还原连接再核对，避免并发失败占满连接池后互相等待第二条连接。
            RewardReceipt receipt;
            try { receipt = find(operation); } catch (SQLException lookup) { failure.addSuppressed(lookup); throw failure; }
            if (receipt != null) return verify(receipt, receiver, amount);
            throw failure;
        }
    }

    private static RewardReceipt verify(RewardReceipt receipt, UUID receiver, BigDecimal amount) throws SQLException {
        if (!receipt.receiverId().equals(receiver) || receipt.amount().compareTo(amount) != 0) throw new SQLException("奖励操作 ID 参数冲突");
        return receipt;
    }

    private static String table() {
        if (!SQL.tableName.matches("[A-Za-z0-9_]+")) throw new IllegalStateException("经济数据表名无效");
        return SQL.tableName + "_reward_receipts";
    }
}
