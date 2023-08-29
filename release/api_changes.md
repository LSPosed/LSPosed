### API Changes
* Implementation of [Modern Xposed API](https://github.com/LSPosed/LSPosed/wiki/Develop-Xposed-Modules-Using-Modern-Xposed-API):
Currently, most part of the new API has been roughly stable (except `helper`). We hope developers can test the new API to provide feedback on possible issues. The modern API will be published to Maven Central with the release of LSPosed 2.0.0, so before this, you can make suggestions to help make it better.

* Allow hooking processes of the `android` package besides `system_server` ([See this commit](https://github.com/LSPosed/LSPosed/commit/6f6c4b67d736e96a61f89b5db22c2e9bbde19461)): For historical reasons, the package name of `system_server` was changed to `android` (See [this commit from rovo89](https://github.com/rovo89/XposedBridge/commit/6b49688c929a7768f3113b4c65b429c7a7032afa)). To correct this behavior, for legacy modules, no code adjustment is needed, but the system framework is displayed as `system` instead of `android` in manager, with a new package `android` which is responsible for system dialogs, etc. For modern modules, the meaning of `system` and `android` in the declared scope have the same meaning as they display in manager.

### API 变更
* 实现了 [Modern Xposed API](https://github.com/LSPosed/LSPosed/wiki/Develop-Xposed-Modules-Using-Modern-Xposed-API)：
目前，新 API 的大部分已经相对稳定（除了 `helper`）。我们希望开发者能够测试新 API，并提供反馈以解决可能存在的问题。现代 API 将在 LSPosed 2.0.0 发布时发布到 Maven Central，因此在此之前，您可以提出建议以帮助改进它。
* 允许钩住 `system_server` 外的 `android` 进程（[查看此提交](https://github.com/LSPosed/LSPosed/commit/6f6c4b67d736e96a61f89b5db22c2e9bbde19461)）：
由于历史原因，`system_server` 的包名被更改为 `android`，`ChooserActivity`等系统UI的包名被更改为 `system`（请参阅 [rovo89 的此提交](https://github.com/rovo89/XposedBridge/commit/6b49688c929a7768f3113b4c65b429c7a7032afa)）。为纠正此行为，管理器作用域界面中的 `system` 和 `android` 的含义现与它们在实际含义相同。我们保留传统模块代码层面的相反含义，但现代模块已得到更正。

```text
system_server: uid=1000 pkg=system  proc=system
ChooserActivity,ResolverActivity: uid=1000 pkg=android proc=android:ui,system:ui
```
