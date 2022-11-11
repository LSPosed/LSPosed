### Changelog
- Update translation to fix crashes in some languages
- Some UI fixes
- Avoid calling `finishReceiver` for unordered broadcasts
- Clear application profile data before performing dexOpt
- Distinguish update channels when checking updates
- Fix hook/deoptimize static methods failed on some Android 13 devices
- Repository shows assets size and download counts
- Fix hooking proxy method
- Init resources hook when calling `hookSystemWideLayout`

### 更新日志
- 更新翻译以修复部分语言下的崩溃
- 一些用户界面的修复
- 避免为无序广播调用 `finishReceiver`
- 执行 dexOpt 之前清空应用 profile 数据
- 检查更新时候区分更新通道
- 修复某些 Android 13 设备上静态方法挂钩/反优化失败
- 仓库显示附件大小和下载次数
- 修复代理方法挂钩
- 调用 `hookSystemWideLayout` 时候初始化资源钩子
