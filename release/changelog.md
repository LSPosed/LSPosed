### New Xposed API proposal
As Android version iterates, the original Xposed API by rovo89 reaches its limits. Now we are working on the new modern Xposed API with features of application scope management, remote preferences, dex parser interface and so on.
The new API will be implemented in the next releasing of LSPosed, and it is welcome to post your suggestions on https://github.com/libxposed.

### New way to open parasitic manager
Some devices cannot create shortcuts due to kernel issues, now LSPosed switches to opening parasitic manager from notification. This notification is always present and can be disable in manager settings.

### Changelog
- Some manager UI fixes
- Update DoH
- Set the dex2oat wrapper owner and group to root:shell
- Guard backup during hook
- Add notification to open parasitic manager
- Fix hook 32bit process
- Fix dex2oat fallback
- Fix webview permission


### 新的 Xposed API 提案
随着 Android 版本的迭代，原有的 rovo89 Xposed API 已经达到了极限。 我们正在开发新的现代 Xposed API，它具有模块作用域管理、远程配置文件、dex 解析器接口等功能。
新的 API 将在 LSPosed 的下一个版本中实现，欢迎在 https://github.com/libxposed 上发表您的建议。

### 新的管理器打开方式
有部分设备因内核问题无法创建快捷方式，现在 LSPosed 切换到从通知打开管理器。此通知一直存在，也可以在管理器设置中关闭通知。

### 更新日志
- 一些管理器界面修复
- 更新安全 DNS 实现
- 将 dex2oat 包装器权限设置为 root:shell
- hook 期间保护备份
- 添加从通知打开寄生管理器的方式
- 修复 hook 32 位进程
- 修复 dex2oat 回退
- 修复 webview 文件权限
