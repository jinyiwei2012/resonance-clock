# Resonance Clock

一个面向 Minecraft 1.20.1 Forge 的测试版 Curios 遗物 Mod。提供共鸣遗物、投影钟环与翼、可选飞行授权、服务端权威的“时光扭结”，以及 0.2.0 新增的“时之紊乱”与净化。

## 环境与构建

- Minecraft 1.20.1
- Forge 47.4.5
- Curios Forge 5.14.1+1.20.1
- Java 17

使用 Java 17 在项目根目录执行：

```shell
./gradlew build
```

Windows 可使用：

```bat
gradlew.bat build
```

当前版本为 `0.2.2` 预发布测试版。将 Forge、Curios Forge 和构建出的 Mod JAR 放入同一 Forge 实例的 `mods` 目录；不需要 Relics 或 Iron 前置，客户端与服务器应使用相同 Mod 版本。

当前本地 JAR 已由 Java 17 构建，且其 `META-INF/mods.toml` 声明许可证为 `MIT`。

## 使用与默认配置

- 管理员可使用 `/give @s resonance_clock:resonance_relic` 获取遗物。
- 管理员可使用 `/give @s resonance_clock:temporal_purifier` 获取一次性净化物品；对无诅咒玩家使用不会消耗。
- `/resonance_clock curse <targets>` 与 `/resonance_clock clear <targets>` 需要权限等级 2；玩家可用 `/resonance_clock status` 自查，管理员可查询其他玩家。
- 时之紊乱固定记录施加时状态，默认每累计 24000 个在线服务端 tick 回溯一次。死亡、离线或关闭功能时暂停；死亡重生仍保留，牛奶和卸下遗物不会解除。
- 已有时光扭结时拒绝施加诅咒；成功创建新的时光扭结锚点会原子净化诅咒，失败不会净化。
- 遗物可使用本 Mod 注册的 Curios `resonance_clock` 独立槽位；已有 `charm` 槽兼容逻辑仍保留。仅放在普通背包中不会生效；请从 Curios 控制设置中查看或绑定界面入口。未验证与所有第三方槽位 Mod 的并存行为。
- 默认按 `V` 使用“时光扭结”，可在控制设置中重绑。
- 扭结默认持续 10 秒，冷却 30 秒。
- 默认只放大原版饥饿伤害至 8 倍，不加快饥饿消耗。
- `flightEnabled` 默认是 `false`。飞行授权使用原版 `mayfly`，无法可靠区分多个来源；若其他 Mod、命令或权限系统也授予飞行，必须保持该开关关闭。
- 权限等级 2 的管理员可使用 `/resonance_clock debug on|off|status [target]` 临时启用服务端详细现场日志；该工具只读且随服务器停止复位，详见 [运行时调试](docs/debug.md)。

服务器配置由 Forge 在世界的 server config 目录生成，以 `resonance_clock-server.toml` 为准。

## 验证状态

0.2.2 修复了 0.2.1 将合法原版未着火 `fireTicks = -1`、负 air 和有限 modded food/saturation 快照错误拒绝的问题；现有 version 1 合法锚点可继续倒计时，但已取消的锚点不会恢复。调试仅记录非敏感的校验 reason/field，不记录原始 NBT 或坐标。自动化覆盖服务端 capture 和 Curios capability 实际插入，不证明真人网络、传送或 GUI；详见 [验证摘要](docs/verification.md)。

## 文档

- [首版设计摘要](docs/design.md)
- [参考资料核查摘要](docs/reference-review.md)
- [后续阶段摘要](docs/roadmap.md)
- [验证摘要](docs/verification.md)
- [运行时调试](docs/debug.md)
- [第三方声明](THIRD_PARTY_NOTICES.md)

原始需求文本、参考图片、模组归档和完整本地诊断记录不随仓库分发。

## 许可

原创代码和原创程序图标采用 [MIT License](LICENSE)。第三方依赖、Minecraft 和未分发的原始参考资料不包含在该授权中。
