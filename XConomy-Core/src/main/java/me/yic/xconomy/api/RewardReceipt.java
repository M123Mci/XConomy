package me.yic.xconomy.api;

import java.math.BigDecimal;
import java.util.UUID;

/** 与余额在同一事务中提交的奖励回执；重复操作返回原回执。 */
public record RewardReceipt(UUID operationId, UUID receiverId, BigDecimal amount, BigDecimal balance) { }
