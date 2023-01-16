### New Xposed API proposal
As Android version iterates, the original Xposed API by rovo89 reaches its limits. Now we are working on the new modern Xposed API with features of application scope management, remote preferences, dex parser interface and so on.
The new API will be implemented in the next releasing of LSPosed, and it is welcome to post your suggestions on [https://github.com/libxposed](https://github.com/libxposed).

### Changelog
- Some manager UI fixes
- Update DoH
- Set the dex2oat wrapper owner and group to root:shell
- Guard backup during hook
- Add notification to open parasitic manager
- Fix hook 32bit process
- Fix dex2oat fallback
- Fix webview permission

### 更新日志
- 一些管理器界面修复
- 更新安全 DNS 实现
- 将 dex2oat 包装器权限设置为 root:shell
- hook 期间保护备份
- 添加从通知打开寄生管理器的方式
- 修复 hook 32 位进程
- 修复 dex2oat 回退
- 修复 webview 文件权限
