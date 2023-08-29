### [API Changes, click to read more](https://docs.lsposed.org/release/api_changes)
* Implementation of [Modern Xposed API]
* Allow hooking processes of the `android` package besides `system_server`

### [Changelog](https://github.com/LSPosed/LSPosed/releases/tag/v1.9.0)
* Magisk version requires 24.0+, and for Riru 26.1.7+
* Make dex2oat wrapper more compatible
* Fix some hooks on Android 8.1
* Fix backup race, fix 'JNI DETECTED ERROR IN APPLICATION: java_object == null'
* Fix `processName` for `handleLoadedPackage`'s `lpparam`
* Fix `isFirstPackage` for `afterHookedMethod`
* Fix NPE due to null `getModule()` return value
* Fix a race by lock-free backup implementation
* Skip secondary classloaders that do not include code
* Only clear module's `LoadedApks`
* Upgrade Dobby to fix arm32 native hook
* Adapts to Android 14
* Fixes known issues and performance optimizations

[中文](https://docs.lsposed.org/release/changelog_zh)
