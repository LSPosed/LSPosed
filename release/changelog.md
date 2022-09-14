### Changelog
- Tested support for Android 13
- Fix module preference on non-primary users
- Fix log saving
- Apply the SELinux label of the entire WebView cache directory
- Refactor core
- Fix `cancelNotificationWithTag` for Android 11-
- Fix `broadcastIntentWithFeature` on Android 12+
- Manager targets SDK 33
- Replace the corruption handler of DB to avoid crashing
- Use abseil to improve performance
- Display module/releases last publish time
- Fix deoptimize static methods
- Preliminary support for Android 14

### 更新日志
- 通过 Android 13 兼容性测试
- 修复非主用户的模块配置
- 修复日志保存
- 整个应用 WebView 缓存目录的 SELinux 标签
- 重构核心
- 修复 Android 11- 的 `cancelNotificationWithTag`
- 修复 Android 12+ 的 `broadcastIntentWithFeature`
- 管理器目标 SDK 33
- 替换数据库的损坏句柄以避免崩溃
- 使用 abseil 提升性能
- 显示模块/发行版的发布时间
- 修复静态方法的去优化
- 初步 Android 14 支持