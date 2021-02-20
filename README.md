# LSPosed Framework

![Core](https://github.com/LSPosed/LSPosed/workflows/Core/badge.svg) ![Manager](https://github.com/LSPosed/LSPosed/workflows/Manager/badge.svg)

## Introduction 

A Riru module trying to provide an ART hooking framework (initially for Android Pie) which delivers consistent APIs with the OG Xposed, leveraging YAHFA and SandHook hooking framework, supports Android 8.0 ~ **11**.

> Xposed is a framework for modules that can change the behavior of the system and apps without touching any APKs. That's great because it means that modules can work for different versions and even ROMs without any changes (as long as the original code was not changed too much). It's also easy to undo. As all changes are done in the memory, you just need to deactivate the module and reboot to get your original system back. There are many other advantages, but here is just one more: Multiple modules can do changes to the same part of the system or app. With modified APKs, you to decide for one. No way to combine them, unless the author builds multiple APKs with different combinations.

## Supported Versions

Android 8 ~ 11

## Install

1. Install Magisk v21+
2. Install [Riru](https://github.com/RikkaApps/Riru/releases) v23+ from Magisk repo.
3. [Download](#download) and install LSPosed in Magisk Manager
4. Install [LSPosed Manager](https://github.com/LSPosed/LSPosed/releases)
5. Reboot.
6. Have fun! :)

## Download

For stable release, please go to [Github Release page](https://github.com/LSPosed/LSPosed/releases)
For canary build, please check [Github Actions](https://github.com/LSPosed/LSPosed/actions)
Note: debug build is only available on Github Actions. 

## Useful Links

- [List of Xposed Modules For Android Pie Working With LSPosed](https://forum.xda-developers.com/xposed/list-xposed-modules-android-pie-ed-t3892768) (thanks to Uraniam9 @ xda-developers)

## Get Help

- GitHub issues: [Issues](https://github.com/LSPosed/LSPosed/issues/)
- (For Chinese speakers) 本项目只接受英语**标题**的issue。如果您不懂英语，请使用[翻译工具](https://www.deepl.com/zh/translator)

## For Developers

Developers are welcomed to write Xposed modules with hooks based on LSPosed Framework. Module written based on LSPosed framework is fully compatible with the original Xposed Framework, so contrary a Xposed Framework-based module will work well with the LSPosed framework too.

- [Xposed Framework API](https://api.xposed.info/)

We use the module repository of the original Xposed, so you simply upload the module to repository, then you can download your module in LSPosed.

- [Xposed Module Repository](https://repo.xposed.info/)

## Community Discussion

- Telegram: [@LSPosed](http://t.me/LSPosed)

Notice: These community group don't accept any bug report, please use [Get help](#get-help) to report.

## Credits 

- [YAHFA](https://github.com/rk700/YAHFA): the core ART hooking framework
- [Magisk](https://github.com/topjohnwu/Magisk/): makes all these possible
- [Riru](https://github.com/RikkaApps/Riru): provides a way to inject codes into zygote process
- [XposedBridge](https://github.com/rovo89/XposedBridge): the OG xposed framework APIs
- [dexmaker](https://github.com/linkedin/dexmaker) and [dalvikdx](https://github.com/JakeWharton/dalvik-dx): to dynamiclly generate YAHFA hooker classes
- [SandHook](https://github.com/ganyao114/SandHook/): ART hooking framework for SandHook variant
- [Dobby](https://github.com/jmpews/Dobby): used for inline hooking
- [EdXposed](https://github.com/ElderDrivers/EdXposed): fork source

## License

LSPosed is licensed under the **GNU General Public License v3 (GPL-3)** (http://www.gnu.org/copyleft/gpl.html).