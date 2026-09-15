package me.yic.xconomy.api;

import me.yic.xconomy.XConomy;
import me.yic.xconomy.data.DataCon;
import me.yic.xconomy.data.DataFormat;
import me.yic.xconomy.data.caches.Cache;
import me.yic.xconomy.data.sql.RewardLedger;
import me.yic.xconomy.api.event.PlayerAccountEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** 主线程提交，工作线程原子入账；Future 成功仅表示持久事务确已提交。 */
public final class DurableRewards {
    private static final java.util.concurrent.ExecutorService WRITES = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
    private static volatile boolean accepting = true;
    private DurableRewards() { }

    public static CompletableFuture<RewardReceipt> deposit(Player player, UUID operation, BigDecimal amount) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("奖励必须由主线程提交");
        if (!accepting) throw new IllegalStateException("经济服务正在停用");
        Objects.requireNonNull(operation);
        if (!player.isOnline() || amount == null || amount.signum() <= 0
                || DataFormat.formatString(amount.toPlainString()).compareTo(amount) != 0) {
            throw new IllegalArgumentException("玩家未在线或奖励金额不符合货币精度");
        }
        UUID receiver = player.getUniqueId();
        String receiverName = player.getName();
        var current = DataCon.getPlayerData(receiver);
        if (current == null || !receiver.equals(current.getUniqueId())) throw new IllegalArgumentException("经济账户身份不一致");
        Bukkit.getPluginManager().callEvent(new PlayerAccountEvent(receiver, player.getName(), current.getBalance(),
                amount, true, operation.toString(), "PLUGIN_API"));
        CompletableFuture<RewardReceipt> result = new CompletableFuture<>();
        WRITES.execute(() -> {
            try {
                RewardLedger.initialize();
                RewardReceipt receipt = RewardLedger.deposit(operation, receiver, amount);
                // 缓存仅作失效，避免覆盖其他插件在事务期间发生的余额变化。
                Cache.deleteDataFromCache(receiver);
                result.complete(receipt);
                try {
                    Bukkit.getScheduler().runTask(XConomy.getInstance(), () -> {
                        var snapshot = new me.yic.xconomy.data.syncdata.PlayerData(receiver, receiverName, receipt.balance());
                        snapshot.setVerifyBalance(receipt.balance().subtract(amount));
                        if (me.yic.xconomy.XConomyLoad.getSyncData_Enable()) DataCon.SendMessTask(snapshot);
                    });
                } catch (RuntimeException notificationFailure) {
                    XConomy.getInstance().getLogger().warning("奖励已提交，跨服余额通知未发送: " + operation);
                }
            } catch (Throwable failure) {
                result.completeExceptionally(failure);
            }
        });
        return result;
    }

    /** 工作线程查询持久回执；返回 null 表示尚无可确认提交，不代表可以盲目重发。 */
    public static RewardReceipt query(UUID operation) throws java.sql.SQLException {
        return RewardLedger.find(operation);
    }

    /** 停用先拒绝新奖励，再排空无需主线程回调的数据库事务。 */
    public static void shutdown() {
        accepting = false;
        WRITES.shutdown();
        try {
            if (!WRITES.awaitTermination(15, java.util.concurrent.TimeUnit.SECONDS)) {
                XConomy.getInstance().getLogger().warning("奖励事务未全部排空，必须通过持久回执核对");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
