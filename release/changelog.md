### Changelog
- Fix `IContentProvider` for Android P and below
- Properly handle invalid `Parcel`
- Fix invalid `setOverrideTable` address on some devices
- Move manager APK to the module path
- Fix remote preferences listener not working on system_server
- Fix hook crash on Android 14
- Fix static method hook on some Android 14 devices
- Fix some `LoadedApk`s not calling `onPackageLoad`
- Fix the wrong path of the new `XSharedPreferences`
- Add tips about deploying optimizations in Android Studio

### 更新日志
- 修复 Android P 及以下版本的 `IContentProvider`
- 正确处理无效的 `Parcel`
- 修复某些设备上无效的 `setOverrideTable` 地址
- 将管理器 APK 移动到模块路径
- 修复系统框架上 remote preference 监听器无效的问题
- 修复 Android 14 上的 hook 错误
- 修复某些设备上 Android 14 的静态方法 hook 问题
- 修复一些 `LoadedApk` 没有回调 `onPackageLoad`
- 修复新 `XSharedPreferences` 的路径错误
- 增加 Android Studio 部署优化提示
