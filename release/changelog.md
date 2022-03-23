### Changelog
- Add a new feature to only allow module classloaders to access Xposed API
- Fix language switch
- Support Android 13 DP1 and DP2
- Fix icon creation and resource hooks on ZUI devices
- Improve cache performance in `XposedHelpers` (Thanks @RinOrz)
- Fix parasitic manager in Android 8.1
- Fix module deactivation after reboot in rare cases
- Fix resource cache since Android 11
- Fix the first invoke invalid of hooked static methods since Android 12
- Refactor to use [LSPlant](https://github.com/LSPosed/LSPlant) as ART hook framework

### 更新日志
- 添加只允许模块类加载器使用 Xposed API 的新特性
- 修复语言切换
- 支持 安卓 13 DP1 和 DP2
- 修复 ZUI 设备上的图标创建和资源挂钩
- 提升 `XposedHelpers` 的缓存性能 (感谢 @RinOrz)
- 修复安卓 8.1 的寄生管理器
- 修复罕见情况下重启后模块取消激活
- 修复自 Android 11 以来的资源缓存
- 修复自 Android 12 以来的被 hook 静态方法第一次调用失效的问题
- 重构以使用 [LSPlant](https://github.com/LSPosed/LSPlant) 作为 ART hook 框架
