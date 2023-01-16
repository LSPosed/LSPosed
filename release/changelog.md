### New Xposed API proposal
As Android version iterates, the original Xposed API by rovo89 reaches its limits. Now we are working on the new modern Xposed API with features of application scope management, remote preferences, dex parser interface and so on.
The new API will be implemented in the next releasing of LSPosed, and it is welcome to post your suggestions on [https://github.com/libxposed](https://github.com/libxposed).

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

[中文更新日志](https://docs.lsposed.org/release/changelog_zh)
