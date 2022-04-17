### Changelog
- Fix rare inactivation after reboot
- Prevent infinite recursion when modules hooked critical methods
- Fix resource hook on ZUI devices
- Fix resource hook for layout
- Skip duplicate scopes for module process
- Avoid creating the LoadedApk from ourselves
- App UI improvements

### 更新日志
- 修复一个低概率的未激活
- 阻止因模块 hook 部分关键方法引发的无限递归
- 修复 ZUI 设备上的资源钩子
- 修复对布局的资源钩子
- 修复模块重复加载
- 避免自行创建 LoadedApk 引发的问题
- App UI 更新与修复

<!--
## What's Changed
* Fix a ui bug & upgrade AGP to 7.1.3 by @Howard20181 in https://github.com/LSPosed/LSPosed/pull/1827
* Avoid using system methods in callback by @yujincheng08 in https://github.com/LSPosed/LSPosed/pull/1830
* No need to implement Method.invoke ourselves by @canyie in https://github.com/LSPosed/LSPosed/pull/1831
* Fix regen signature by @vvb2060 in https://github.com/LSPosed/LSPosed/pull/1829
* Fix blur effect on Android 12 by @Howard20181 in https://github.com/LSPosed/LSPosed/pull/1832
* Constructor of ActivityThread is private by @canyie in https://github.com/LSPosed/LSPosed/pull/1833
* Check preload dex by @vvb2060 in https://github.com/LSPosed/LSPosed/pull/1834
* Bump core from 1.3.4 to 1.4.0 by @dependabot in https://github.com/LSPosed/LSPosed/pull/1837
* Adjusting the dialog style & better RTL support  by @Howard20181 in https://github.com/LSPosed/LSPosed/pull/1838
* Fix readme template RTL in dark mode by @Howard20181 in https://github.com/LSPosed/LSPosed/pull/1839
* Use Toast instead of Snackbar because of raggedy fab by @Howard20181 in https://github.com/LSPosed/LSPosed/pull/1840
* Skip duplicate scopes for module process by @yujincheng08 in https://github.com/LSPosed/LSPosed/pull/1845
* `XC_LayoutInflated` should be comparable by @yujincheng08 in https://github.com/LSPosed/LSPosed/pull/1851
* Avoid creating the LoadedApk from ourselves by @yujincheng08 in https://github.com/LSPosed/LSPosed/pull/1852
* [translation] Update translation from Crowdin by @XposedBot in https://github.com/LSPosed/LSPosed/pull/1853
* fmt by @kotori2 in https://github.com/LSPosed/LSPosed/pull/1854


**Full Changelog**: https://github.com/LSPosed/LSPosed/compare/v1.8.1...v1.8.2
-->