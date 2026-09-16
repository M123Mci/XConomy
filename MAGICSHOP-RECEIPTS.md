# MagicShop 回执联动补丁

版本：2.26.3-mr.26.2.2。新增 RewardRejectedException，仅在尚未尝试 COMMIT 且 ROLLBACK 已成功时报告明确拒绝。提交调用开始后的错误仍须查询持久回执，不能把超时当作未到账。

RewardLedger 的余额与回执仍共用原 SQL 事务；MagicShop 库存和 Mongo 不在该事务中。补丁未改变经济表结构。旧 RewardReceipt/DurableRewards API 保留。

本轮已进行 Java 编译和 clean shadowJar；服务端部署与故障注入尚未进行，等待共享验收环境释放。回退前须核对 MagicShop pending，禁止对未知结果盲目重复发放。
