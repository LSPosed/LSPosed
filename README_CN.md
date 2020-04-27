# EdXposed Framework

[![最新构建](https://ci.appveyor.com/api/projects/status/qu3vj1d64nqia1b8/branch/master?svg=true)](https://ci.appveyor.com/project/ElderDrivers/edxposed/branch/master) [![English](art/README_EN.png)](README.md)

## Introduction 

基于 Riru 的 ART hook 框架 (最初用于 Android Pie) ，提供与原版 Xposed 相同的 API, 使用 YAHFA (或 SandHook) 进行 hook, supports Android 8.0 ~ **10**.

> Xposed 框架是一套开放源代码的、在Android高权限模式下运行的框架服务，可以在不修改APK文件的情况下修改程序的运行，基于它可以制作出许多功能强大的模块，且在功能不冲突的情况下同时运作

## 支持的版本

- Android Oreo (8.x, sdk26/27)
- Android Pie (9, sdk28)
- Android Q (10, sdk29)

如果你使用 Android Nougat (7.x) 或更低版本, 请使用原版的 Xposed 框架

## 编译需求

见 [Riru-Core 的编译需求](https://github.com/RikkaApps/Riru/blob/master/README.md#build-requirements)
zip 可执行文件可以在 [这里](http://gnuwin32.sourceforge.net/packages/zip.htm) 下载

## 编译和使用

1. 执行 `:edxp-core:[zip|push][Yahfa|Sandhook]Release` 来构建对应分支的可刷入的 zip 文件
2. 在 `edxp-core/release/` 中找到可刷入的 zip 文件
3. 经由 Magisk Manager 或在恢复模式(Recovery)中刷入

## 安装

1. 安装 Magisk v19+ (对于 华为 / 荣耀 或 一加 7T / 7TPro 设备, 需要 Magisk v20.2+)
2. 在 Magisk 仓库中安装 [Riru-Core](https://github.com/RikkaApps/Riru/releases) v19 或更高版本.
3. [下载](#下载)并在恢复模式(Recovery)或经由 Magisk Manager 安装 EdXposed.
4. 安装 [框架管理应用](#框架管理应用).
4. 重启手机.
5. 完成 :)

## 下载

Edxposed 拥有三个不同的版本

- Stable：经过测试的稳定版, 适合一般用户，更新缓慢.
***在 [Magisk Manager] 中的 [下载] 页中下载 Stable 版本***

- Alpha: 多次提交更新的测试版.
***在 [[Github Releases](https://github.com/ElderDrivers/EdXposed/releases)] 中下载 Alpha 版本***

- Canary: 由 CI 自动生成的测试版.
***在 [[EdXposed Manager](https://github.com/ElderDrivers/EdXposedManager)] 中下载 Canary 版本***

## 框架管理应用

> Xposed Installer 和 EdXposed Installer 均已停更，后续版本将取消支持，请使用 [EdXposed Manager](https://github.com/ElderDrivers/EdXposedManager)

- 对于 v0.2.9.5 或更低版本: [Xposed Installer](https://github.com/DVDAndroid/XposedInstaller).
- 对于 v0.2.9.6 和 v0.2.9.7: [Xposed Installer](https://github.com/DVDAndroid/XposedInstaller) + [EdXp Manager](https://github.com/solohsu/EdXpManager)(optional).
- 对于 v0.2.9.8 或更高版本: [EdXposed Installer](https://github.com/solohsu/XposedInstaller) 或者 [EdXposed Manager](https://github.com/ElderDrivers/EdXposedManager).
- 对于最新版，我们建议使用 [EdXposed Manager](https://github.com/ElderDrivers/EdXposedManager).

## 外部链接

- [List of Xposed Modules For Android Pie Working With EdXposed](https://forum.xda-developers.com/xposed/list-xposed-modules-android-pie-ed-t3892768) (感谢 Uraniam9 @ xda-developers)

## 已知问题

见 [Wiki: 已知问题和漏洞](https://github.com/ElderDrivers/EdXposed/wiki/已知问题和漏洞)

## 获取帮助

- GitHub issues: [Issues](https://github.com/ElderDrivers/EdXposed/issues/)

- 注意: 鉴于部分用户提交的Issues质量过低，对于中文用户反馈，请先阅读[EdXposed错误提交说明_cn](http://edxp.meowcat.org/assets/EdXposedIssuesReport_cn.txt)(不看说明提交的Issue会有很大可能被close)

## 社区交流

- QQ 群组: [855219808](http://shang.qq.com/wpa/qunwpa?idkey=fae42a3dba9dc758caf63e971be2564e67bf7edd751a2ff1c750478b0ad1ca3f)
- Telegram 电报: [@Code_of_MeowCat](http://t.me/Code_of_MeowCat)

注意: 这些社区群组不接收问题反馈, 请使用 [获取帮助](#获取帮助) 进行反馈.

## 贡献

- 显然，框架还不够稳定，欢迎使用PR贡献代码. :)
- 如果你愿意，可以[请我喝杯咖啡](https://www.paypal.me/givin2u).

## 鸣谢

- [YAHFA](https://github.com/rk700/YAHFA): ART hook 核心框架
- [Magisk](https://github.com/topjohnwu/Magisk/): 让一切成为可能
- [Riru](https://github.com/RikkaApps/Riru): 提供一种将代码注入 zygote 进程的方法
- [XposedBridge](https://github.com/rovo89/XposedBridge): 原版 xposed 框架的 API
- [dexmaker](https://github.com/linkedin/dexmaker) 和 [dalvikdx](https://github.com/JakeWharton/dalvik-dx): 动态生成 YAHFA hook 类
- [Whale](https://github.com/asLody/whale): 用于 hook 内联方法
- [SandHook](https://github.com/ganyao114/SandHook/): SandHook 分支的 ART hooking 框架

