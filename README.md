# Resonance Clock

一个面向 Minecraft 1.20.1 Forge 的测试版 Curios 遗物 Mod。首版提供共鸣遗物、投影钟环与翼的客户端外观、可选飞行授权，以及服务端权威的“时光扭结”状态回溯。

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

当前版本为 `0.1.0` 测试版。将 Forge、Curios Forge 和构建出的 Mod JAR 放入同一 Forge 实例的 `mods` 目录；客户端与服务器应使用相同 Mod 版本。

当前本地 JAR 已由 Java 17 构建，且其 `META-INF/mods.toml` 声明许可证为 `MIT`。

## 使用与默认配置

- 管理员可使用 `/give @s resonance_clock:resonance_relic` 获取遗物。
- 遗物使用 Curios 的 `charm` 槽位；仅放在普通背包中不会生效。
- 默认按 `V` 使用“时光扭结”，可在控制设置中重绑。
- 扭结默认持续 10 秒，冷却 30 秒。
- 默认只放大原版饥饿伤害至 8 倍，不加快饥饿消耗。
- `flightEnabled` 默认是 `false`。飞行授权使用原版 `mayfly`，无法可靠区分多个来源；若其他 Mod、命令或权限系统也授予飞行，必须保持该开关关闭。

服务器配置由 Forge 在世界的 server config 目录生成，以 `resonance_clock-server.toml` 为准。

## 验证状态

已完成编译与隔离开发专服启动到 `Done` 的验证，但受控 stdin 正常停服没有取得证据。发布 JAR 整包加载、客户端、多人同步、实际 Curios 槽位行为、视觉验收和整合包兼容性均尚未测试。详见 [验证摘要](docs/verification.md)。

## 文档

- [首版设计摘要](docs/design.md)
- [参考资料核查摘要](docs/reference-review.md)
- [后续阶段摘要](docs/roadmap.md)
- [验证摘要](docs/verification.md)
- [第三方声明](THIRD_PARTY_NOTICES.md)

原始需求文本、参考图片、模组归档和完整本地诊断记录不随仓库分发。

## 许可

原创代码和原创程序图标采用 [MIT License](LICENSE)。第三方依赖、Minecraft 和未分发的原始参考资料不包含在该授权中。
