### 1.9.0 更新日志
* 修复在拨号器中输入暗码启动管理器无效的问题
* 修复三星设备上的通知问题
* 添加 Vercel/Cloudflare 作为模块仓库的后备方案
* Magisk 版本要求 24.0+，对于 Riru，要求 Riru 26.1.7+
* 使 dex2oat 包装器更兼容，例如在 KernelSU 上
* 修复 Android 8.1 上的一些钩子问题
* 为创建快捷方式和通知添加更多提示
* 修复备份冲突，修复 'JNI DETECTED ERROR IN APPLICATION: java_object == null'
* 修复 `handleLoadedPackage` 的 `lpparam` 中的 `processName`
* 修复 `afterHookedMethod` 的 `isFirstPackage`
* 修复 Android 14 的通知意图
* 修复管理器的暗色主题
* 无条件允许创建快捷方式，除非不支持默认桌面
* 修复由于空的 `getModule()` 返回值引起的 NPE
* 修复 `AfterHooker` 类名中的拼写错误
* 辅助功能：为搜索按钮添加标签
* 将 EUID 设置为 1000 以修复 Flyme 上的通知和模块列表
* 通过无锁备份实现修复竞争问题
* 预定义一些 SQLite 模式以提高性能
* 为 Android P+ 设置 db 同步模式，修复一些 Oplus 设备无法工作的问题
* 跳过不包含代码的次级类加载器
* 在渲染空的 Markdown 时避免 NPE，修复管理器崩溃问题
* 为仓库模块添加已安装提示
* [翻译] 从 Crowdin 更新翻译
* 将目标 SDK 升级到 34
* 只清除模块的 `LoadedApks` 而不是全部清除
* 升级 Dobby，修复在 arm32 上的native hook问题
* 显示管理器的包名而不是版本号
* 始终允许固定快捷方式，无论它们是否已固定
* 修复 Android 14 启动完成后的 ANR 问题
* 修复 Android 14 的 `IActivityManager.bindService`
* 不为非自适应图标应用系统图标形状
* 修复管理器的任务图标
* 默认启用 Xposed API 调用保护功能
