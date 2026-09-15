# Paper 26.2 构建与持久奖励 API

官方源码：YiC200333/XConomy，接管提交 `472e98a174a96f7eef0ec8ea4aea3cbdcdba0c03`；最新发布标签 `2.26.3`。保留 GPL-3.0 授权和原始来源。本分支只交付 Paper 26.2／Java 25。

使用 `gradlew.bat clean shadowJar` 构建，`gradlew.bat publish` 发布到 `D:/Minecraft/PluginLibs/Maven`。坐标 `me.yic:XConomy:2.26.3-mr.26.2.1`。Paper→Bukkit→Core 的源码覆盖顺序与官方 Shade 规则一致，全部交付类由当前源码编译，不拼接旧插件实现。Gradle 为维护入口，原 Maven 文件仅保留官方源码参考。

新增 `DurableRewards.deposit(Player, UUID, BigDecimal)`，必须由主线程调用，返回 `CompletableFuture<RewardReceipt>`。Future 成功表示数据库事务已提交；数据库工作在专用执行器运行。调用方不得把 Future 创建成功视为入账成功。

余额、原交易记录（开启时）及 `xconomy_reward_receipts` 回执共用一个事务。操作 UUID 是幂等键：同 UUID、同收件人和金额返回原回执；参数冲突不修改余额。使用原余额表和原字段，不导入、转换或清除余额数据。MySQL 必须为 InnoDB；新回执表启动使用时幂等创建。

原余额字段为 double，若本次变更无法精确保存到原字段，则回滚整个奖励事务，不能静默损失小数。原配置的货币精度及最大余额限制仍有效。

连接或提交响应异常时，只在读到匹配持久回执后确认成功；否则向调用方报告异常，要求使用 `DurableRewards.query(UUID)` 核对，不能盲目换 UUID 重发。回执查询须在工作线程进行。停用拒绝新操作并排空不依赖主线程回调的数据库事务。

MagicMail 使用此接口发放金币，PlayerPoints 点券仍使用其独立 API／交易记录；不会将两种余额混用。原 Vault 接口和 XConomy 命令仍保留，原第三方调用方没有被强制迁移到新增接口。

验证包括正常金币与点券到账、同操作 10 次并发、跨服再次提交不增发、参数冲突、金额精度拒绝、交易日志故障后的事务回滚。共享数据库服务从未停止，故障注入只匹配单个测试操作 UUID，验收后删除。

内部依赖：HikariCP 7.0.2、Jedis 6.2.0、bStats 3.1.0；外部编译 JAR 位于 `../../PluginLibs/Jars`。Towny／Enterprise 保留编译和可选集成，没有在本机安装它们做运行验收。
