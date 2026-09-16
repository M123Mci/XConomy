# MagicGames 持久扣款联动

版本：`2.26.3-mr.26.2.3`，Paper 26.2／Java 25。

`DurableRewards.withdraw(Player, UUID, BigDecimal)` 在主线程提交，返回 `CompletableFuture<RewardReceipt>`。输入金额必须为正数；成功回执的 `amount` 为负数。`deposit` 保持原 API，回执金额为正。`query` 可用原操作 ID 查询两种交易。

扣款复用既有独立连接、账户行锁、回执唯一键与同事务提交路径，余额不足拒绝并回滚。重复操作 ID 只返回相同接收者、相同有符号金额的既有回执；不能把扣款 ID 再用作退款 ID。余额缓存失效与通知仍在事务完成后处理。通知的旧余额根据回执有符号金额计算。

调用方必须在提交前持久保存操作 ID。超时或结果未知时先查询，不得生成新 ID 重试；退款是另一笔有独立稳定 ID 的入账。`RewardRejectedException` 表示确认未提交，不确定异常仍须对账。

## 2026-09-16 验证

- `clean shadowJar` 与 `publishPaperPublicationToSharedRepository` 通过。
- 在独立 MySQL 库、真实 MRVerifyA 账户提交同 ID 的并发扣款，仅产生一次负金额回执并扣款一次。
- 同 ID 两次退款仅入账一次，余额恢复；不足余额扣款无成功回执。
- MagicGames 真实 10 元入场、失败准备退款、跨重启流水与一次性奖励均核对数据库及客户端结果。
- 构建、Maven、隔离服务端完整 JAR 一致；测试后恢复原实例，删除独立数据库及一次性验证插件。
- 未保留验证类、玩家数据或数据库认证，不改动其他插件的经济语义。

完整插件 SHA-256：`983f1642bb5e990fb5b7cbec6c6281fbc01cf268486fc2514cb56e5825a5bda7`。
