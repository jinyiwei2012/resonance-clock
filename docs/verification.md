# 验证摘要

## 已完成

- 0.2.2 在 Java 17 下实际执行 `gradlew.bat --offline --no-daemon runGameTestServer`。日志 `verification/V022_GAMETEST_FINAL.log` 记录发现并执行 7 个 GameTest，`All 7 required tests passed`、`BUILD SUCCESSFUL` 和命令退出码 `0`。覆盖合法 `fireTicks=-1`、负 air、food/saturation 21、pitch 91、旧 version 1 Twist/Curse 根 remaining=200 的 ACTIVE 状态及校验前后 NBT 不变，以及 NaN/Infinity、错误类型与有符号极值拒绝。
- 该 GameTest 使用直接构造的 `ServerPlayer(server, level, GameProfile)`，显式设置 `fireTicks=-1`、负 air、food/saturation 21 与 pitch 91，验证 `capture -> save -> load` 并逐项对照捕获字段。它证明 capture 保留这些合法值，不证明构造玩家的默认值，也不证明有连接的真人客户端请求、倒计时或传送流程。
- 先前红测日志 `verification/V022_GAMETEST_OLD_RED.log` 如实保留：旧校验拒绝 `fireTicks=-1`；`makeMockServerPlayerInLevel` fixture 因无 Netty channel 失败，不作为传送结论。0.2.2 改用无连接需求的直接 ServerPlayer capture fixture。
- 0.2.2 最终 `gradlew.bat --offline --no-daemon build` 成功，日志 `verification/V022_BUILD_FINAL.log` 记录 `BUILD SUCCESSFUL` 和退出码 `0`。产物 `build/libs/resonance_clock-0.2.2.jar` 的 SHA-256 为 `B1E258FBAA788F4770BC4BA715C6265742710C63A1D6DACCB80EABE7C15FB4BE`，CRC-32 为 `8A0230E0`；`META-INF/mods.toml` 声明版本 `0.2.2`、许可证 `MIT`、Curios `[5.14.1,6)`。完整元数据证据在 `verification/V022_FINAL_JAR_EVIDENCE.txt`。
- 0.2.2 的最小服务端装备门槛已通过：实际 GameTest 发现并执行 8 个测试，记录 `All 8 required tests passed`、`BUILD SUCCESSFUL` 与退出码 `0`，日志保存在忽略的 `verification/V022_SLOT_GAMETEST_FINAL.log`。独立槽测试读取 Curios entity manager 的 player slot（size 1），从直接构造的服务端玩家取得实际 capability handler，验证 `isItemValid` 接受 relic、拒绝 purifier，`insertItem` 后 `RelicState.isEquipped` 为真。它不证明 Curios GUI、真人网络或任意第三方槽位 Mod 并存；本轮不发布，待独立复核。
- 0.2.2 新增数据包资源 `data/resonance_clock/curios/slots/resonance_clock.json`、`data/resonance_clock/curios/entities/player.json` 与 `data/curios/tags/items/resonance_clock.json`，为玩家注册一个仅接受共鸣遗物的独立 Curios 槽，且不替换 `charm`。本地 Curios 5.14.1 字节码确认 reload 目录为 `curios/slots` 与 `curios/entities`，其中资源命名空间决定数据文件实际路径。最小服务端槽链已通过；测试环境未配置 `charm`，所以不能将其缺失误判为本 Mod 覆盖。GUI 操作仍未验证，但不阻止预发布测试版；正式发布仍应完成独立复核。
- 0.2.1 在 Java 17 下执行 `gradlew build --offline --no-daemon`；本轮独立构建日志保存在忽略的 `verification/V021_RELEASE_BUILD.log`。发布 JAR 元数据声明版本 `0.2.1`、许可证 `MIT`、Curios 依赖范围 `[5.14.1,6)`。
- 0.2.1 的 OP 2 会话调试默认关闭，可使用 `/resonance_clock debug on|off|status [target]`。开启后，服务端会记录采样后的本地现场 stack、请求 ID、线程、时钟、装备/锚点/冷却/校验状态、实际 Temporal Twist start result，以及带 `feature=twist` 或 `feature=curse` 的 restore result。异常以原始 Throwable 写入，保留 cause 与 suppressed 链。
- 诊断 callback 的 `RuntimeException` 被隔离，不影响玩法提交、锚点清理、冷却写入或原 gameplay exception 的 identity；不吞 VM fatal errors。每 UUID、每 stage 独立使用 20 tick 采样并在下一次放行时汇总略过数量；生产请求限流仍为 10 tick。
- ignored 的独立 SLF4J/Log4j probe 验证了与生产相同 logger 调用形式输出现场 Throwable、cause 和 suppressed；它不是真人生产 hook 测试。纯 helper harness 覆盖诊断隔离、异常 identity、stage 分类采样、回拨和清理。
- Forge GameTest Server 实际执行并通过 5/5 个既有状态测试。调试 hook 不改变网络消息 ID 或线格式，通道协议仍为 `1`。
- 0.1.0 曾完成 Java 17 构建和开发专服启动验证；其旧产物哈希已被 0.2.0 产物取代。
- 开发专服使用 Forge 47.4.5 和 Curios 5.14.1+1.20.1 启动。为 userdev 环境配置了 Mixin refmap 重映射，日志显示 Curios refmap 已重映射，服务器到达 `Done`。
- 此次服务器启动在隔离工作目录、环回地址和独立测试世界中进行，未使用生产世界数据。

## 0.2.0 继承验证边界

- GameTest 测试源已编译，覆盖严格字段类型和必填校验、NaN/Infinity/越界坐标拒绝、未知状态版本、诅咒 clone/清除，以及周期冻结和失败重置。
- Java 17 下最终 `gradlew build --offline --no-daemon` 成功，日志记录 `BUILD SUCCESSFUL in 26s`；日志保存在忽略的 `verification` 目录，不记录下载代理。
- 隔离 GameTest Server 实际发现并执行 5 个测试，日志报告 `All 5 required tests passed` 及 `BUILD SUCCESSFUL in 1m 10s`。此前显示“0 tests passed”的运行不计为通过。
- 持久数据版本策略：仅接受当前版本 `1`。未知版本、错误根类型、错误 snapshot 类型或缺损字段均冻结，不恢复；每次登录/在线会话最多提示一次。管理员 clear 或成功净化可删除异常根并重建当前版本空状态，不自动猜测迁移。
- 自动 mock player 没有真实网络连接，不能据此声称真实传送成功；安全落点、跨维度传送、Curios 装备竞争和多人同 tick 仍需真实玩家验证。
- 0.2.0 未修改网络消息 ID 或线格式，通道协议保持 `1`；请求处理器增加了受限调试观察，客户端和服务器的 Mod 版本仍应一致。
- 时光扭结请求限流改用主世界 `gameTime` 的长整型服务端时钟，纯 Java harness 已覆盖旧实体 tick 重生回零复现、请求间隔、玩家隔离、clear 和时钟回拨重置。尚未用真实玩家完成死亡重生后的端到端复测，因此不能声称该实机场景已验证修复。
- 完成只读 status 与调试采样修正后的产物 `resonance_clock-0.2.0.jar` 大小为 `80642` 字节，SHA-256 为 `40C960036B2150494553009CD1B465FAEE7C2E6E6CE254E6D7A788293627CA20`。JAR 元数据声明版本 `0.2.0`、许可证 `MIT`、Curios 依赖范围 `[5.14.1,6)`，并包含净化物品模型/纹理、双语文本、调试命令类和请求限流类。
- 普通 `/resonance_clock status` 现在只读取已存在的 Curse NBT，不会因查询创建持久化根。调试请求日志对 accepted/rejected 使用独立的每玩家 20 tick 采样窗口；生产请求限流仍保持原定 10 tick。
- Apollon 兼容研究目前不足以确定精准且安全的适配点；0.2.0 不实现全箭拦截，留待后续取得可靠 API/行为证据后单独设计与验证。
- 调试命令要求权限等级 2，开关仅对当前服务端会话有效；`debug status` 为只读观察，不是修复或清理入口。详见 [运行时调试](debug.md)。

## 停服证据限制

本次启动到 `Done` 后，自动化环境未能取得通过受控 stdin 正常执行 `stop` 的证据。验证进程随后被清理；因此不能把该次运行描述为“已取得正常停服验证”。

## 尚未验证

- 发布 JAR 的完整安装和加载
- 客户端加载、实际渲染和视觉验收
- 真人客户端按键/网络请求、死亡重生、多人与完整整合包中的 0.2.1 诊断 hook；服务端现场 stack 不能证明客户端调用链或形成完整跨异步调用链
- Curios 实际槽位行为
- 多人状态同步和核心玩法场景
- 现有客户端状态同步 packet/API seam 尚未接入实际发送和客户端 sink 注册；当前客户端功能不依赖该 seam，但不得将其描述为已验证的同步功能
- 第三方整合包兼容性与性能

开发专服到达 `Done` 仅证明该 userdev 启动路径完成加载，不代表上述项目已通过。
