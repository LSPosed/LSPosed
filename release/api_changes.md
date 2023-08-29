### API Changes
* Implementation of [Modern Xposed API](https://github.com/LSPosed/LSPosed/wiki/Develop-Xposed-Modules-Using-Modern-Xposed-API):
Currently, most part of the new API has been roughly stable (except `helper`). We hope developers can test the new API to provide feedback on possible issues. The modern API will be published to Maven Central with the release of LSPosed 2.0.0, so before this, you can make suggestions to help make it better.

* Allow hooking processes of the `android` package besides `system_server` ([See this commit](https://github.com/LSPosed/LSPosed/commit/6f6c4b67d736e96a61f89b5db22c2e9bbde19461)): For historical reasons, the package name of `system_server` was changed to `android` (See [this commit from rovo89](https://github.com/rovo89/XposedBridge/commit/6b49688c929a7768f3113b4c65b429c7a7032afa)). To correct this behavior, for legacy modules, no code adjustment is needed, but the system framework is displayed as `system` instead of `android` in manager, with a new package `android` which is responsible for system dialogs, etc. For modern modules, the meaning of `system` and `android` in the declared scope have the same meaning as they display in manager.

```text
system_server: uid=1000 pkg=system  proc=system
ChooserActivity,ResolverActivity: uid=1000 pkg=android proc=android:ui,system:ui
```
