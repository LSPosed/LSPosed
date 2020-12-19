# EdXposed Framework

[简体中文](https://github.com/ElderDrivers/EdXposed/wiki/%E7%AE%80%E4%BB%8B)

## Introduction 

A Riru module trying to provide an ART hooking framework (initially for Android Pie) which delivers consistent APIs with the OG Xposed, leveraging YAHFA (or SandHook) hooking framework, supports Android 8.0 ~ **10**.

> Xposed is a framework for modules that can change the behavior of the system and apps without touching any APKs. That's great because it means that modules can work for different versions and even ROMs without any changes (as long as the original code was not changed too much). It's also easy to undo. As all changes are done in the memory, you just need to deactivate the module and reboot to get your original system back. There are many other advantages, but here is just one more: Multiple modules can do changes to the same part of the system or app. With modified APKs, you to decide for one. No way to combine them, unless the author builds multiple APKs with different combinations.

What are the differences between EdXposed Framework and Xposed Framework?

1. EdXposed fully supports Android Pie, Q and R
2. EdXposed have App List mode. Only the apps you want to apply Xposed modules are hooked. Other apps in system run in a completely clean environment
3. EdXposed doesn't need to reboot system to active most modules
4. EdXposed is hard to detect. EdXposed use Riru to inject, doesn't modify the libart and app_process

## How to use ?

To put it simply, just follow these steps:

1. Install Magisk
2. Flash the Riru Magisk module. You can find it in [Riru release page](https://github.com/RikkaApps/Riru/releases).
3. Flash the Riru - EdXposed Magisk module. You can find it in [EdXposed release page](https://github.com/ElderDrivers/EdXposed/releases).
4. Install EdXposed Manager. You can find it in [EdXposed Manager release page](https://github.com/ElderDrivers/EdXposedManager/releases).
5. Reboot :)

More informations in detail:

[**Official Website**](http://edxp.meowcat.org/)

[**Wiki**](https://github.com/ElderDrivers/EdXposed/wiki)

[**Telegram Channel**](https://t.me/EdXposed/)

## Community Discussion

- QQ Group: [855219808](http://shang.qq.com/wpa/qunwpa?idkey=fae42a3dba9dc758caf63e971be2564e67bf7edd751a2ff1c750478b0ad1ca3f)
- Telegram: [@Code_of_MeowCat](http://t.me/Code_of_MeowCat)

Notice: These community group don't accept any bug report, please use [Get help](#get-help) to report.

## Get Help

- GitHub Issues: [Issues](https://github.com/ElderDrivers/EdXposed/issues/)

- Notice(for Chinese): In view of the low quality of issues submitted, please read the Chinese user report first[EdXposedIssuesReport_cn](http://edxp.meowcat.org/assets/EdXposedIssuesReport_cn.txt)(If you don't read the instructions, the submitted issue is likely to be closed)
