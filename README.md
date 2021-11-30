# LSPosed Framework

![Build](https://shields.io/github/workflow/status/LSPosed/LSPosed/Core?event=push&logo=github) [![Crowdin](https://badges.crowdin.net/e/c3e952afcb24101a4455fcb7917a568d/localized.svg)](https://lsposed.crowdin.com/lsposed) [![Channel](https://img.shields.io/badge/Follow-Telegram-blue.svg?logo=telegram)](https://t.me/LSPosed) [![Chat](https://img.shields.io/badge/Join-QQ%E9%A2%91%E9%81%93-red?logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxMjEgMTQzLjg5Ij48cGF0aCBmaWxsPSIjZmFhYjA3IiBkPSJNNjAuNTAzIDE0Mi4yMzdjLTEyLjUzMyAwLTI0LjAzOC00LjE5NS0zMS40NDUtMTAuNDYtMy43NjIgMS4xMjQtOC41NzQgMi45MzItMTEuNjEgNS4xNzUtMi42IDEuOTE4LTIuMjc1IDMuODc0LTEuODA3IDQuNjYzIDIuMDU2IDMuNDcgMzUuMjczIDIuMjE2IDQ0Ljg2MiAxLjEzNnptMCAwYzEyLjUzNSAwIDI0LjAzOS00LjE5NSAzMS40NDctMTAuNDYgMy43NiAxLjEyNCA4LjU3MyAyLjkzMiAxMS42MSA1LjE3NSAyLjU5OCAxLjkxOCAyLjI3NCAzLjg3NCAxLjgwNSA0LjY2My0yLjA1NiAzLjQ3LTM1LjI3MiAyLjIxNi00NC44NjIgMS4xMzZ6bTAgMCIvPjxwYXRoIGQ9Ik02MC41NzYgNjcuMTE5YzIwLjY5OC0uMTQgMzcuMjg2LTQuMTQ3IDQyLjkwNy01LjY4MyAxLjM0LS4zNjcgMi4wNTYtMS4wMjQgMi4wNTYtMS4wMjQuMDA1LS4xODkuMDg1LTMuMzcuMDg1LTUuMDFDMTA1LjYyNCAyNy43NjggOTIuNTguMDAxIDYwLjUgMCAyOC40Mi4wMDEgMTUuMzc1IDI3Ljc2OSAxNS4zNzUgNTUuNDAxYzAgMS42NDIuMDggNC44MjIuMDg2IDUuMDEgMCAwIC41ODMuNjE1IDEuNjUuOTEzIDUuMTkgMS40NDQgMjIuMDkgNS42NSA0My4zMTIgNS43OTV6bTU2LjI0NSAyMy4wMmMtMS4yODMtNC4xMjktMy4wMzQtOC45NDQtNC44MDgtMTMuNTY4IDAgMC0xLjAyLS4xMjYtMS41MzcuMDIzLTE1LjkxMyA0LjYyMy0zNS4yMDIgNy41Ny00OS45IDcuMzkyaC0uMTUzYy0xNC42MTYuMTc1LTMzLjc3NC0yLjczNy00OS42MzQtNy4zMTUtLjYwNi0uMTc1LTEuODAyLS4xLTEuODAyLS4xLTEuNzc0IDQuNjI0LTMuNTI1IDkuNDQtNC44MDggMTMuNTY4LTYuMTE5IDE5LjY5LTQuMTM2IDI3LjgzOC0yLjYyNyAyOC4wMiAzLjIzOS4zOTIgMTIuNjA2LTE0LjgyMSAxMi42MDYtMTQuODIxIDAgMTUuNDU5IDEzLjk1NyAzOS4xOTUgNDUuOTE4IDM5LjQxM2guODQ4YzMxLjk2LS4yMTggNDUuOTE3LTIzLjk1NCA0NS45MTctMzkuNDEzIDAgMCA5LjM2OCAxNS4yMTMgMTIuNjA3IDE0LjgyMiAxLjUwOC0uMTgzIDMuNDkxLTguMzMyLTIuNjI3LTI4LjAyMSIvPjxwYXRoIGZpbGw9IiNmZmYiIGQ9Ik00OS4wODUgNDAuODI0Yy00LjM1Mi4xOTctOC4wNy00Ljc2LTguMzA0LTExLjA2My0uMjM2LTYuMzA1IDMuMDk4LTExLjU3NiA3LjQ1LTExLjc3MyA0LjM0Ny0uMTk1IDguMDY0IDQuNzYgOC4zIDExLjA2NS4yMzggNi4zMDYtMy4wOTcgMTEuNTc3LTcuNDQ2IDExLjc3MW0zMS4xMzMtMTEuMDYzYy0uMjMzIDYuMzAyLTMuOTUxIDExLjI2LTguMzAzIDExLjA2My00LjM1LS4xOTUtNy42ODQtNS40NjUtNy40NDYtMTEuNzcuMjM2LTYuMzA1IDMuOTUyLTExLjI2IDguMy0xMS4wNjYgNC4zNTIuMTk3IDcuNjg2IDUuNDY4IDcuNDQ5IDExLjc3MyIvPjxwYXRoIGZpbGw9IiNmYWFiMDciIGQ9Ik04Ny45NTIgNDkuNzI1Qzg2Ljc5IDQ3LjE1IDc1LjA3NyA0NC4yOCA2MC41NzggNDQuMjhoLS4xNTZjLTE0LjUgMC0yNi4yMTIgMi44Ny0yNy4zNzUgNS40NDZhLjg2My44NjMgMCAwMC0uMDg1LjM2Ny44OC44OCAwIDAwLjE2LjQ5NmMuOTggMS40MjcgMTMuOTg1IDguNDg3IDI3LjMgOC40ODdoLjE1NmMxMy4zMTQgMCAyNi4zMTktNy4wNTggMjcuMjk5LTguNDg3YS44NzMuODczIDAgMDAuMTYtLjQ5OC44NTYuODU2IDAgMDAtLjA4NS0uMzY1Ii8+PHBhdGggZD0iTTU0LjQzNCAyOS44NTRjLjE5OSAyLjQ5LTEuMTY3IDQuNzAyLTMuMDQ2IDQuOTQzLTEuODgzLjI0Mi0zLjU2OC0xLjU4LTMuNzY4LTQuMDctLjE5Ny0yLjQ5MiAxLjE2Ny00LjcwNCAzLjA0My00Ljk0NCAxLjg4Ni0uMjQ0IDMuNTc0IDEuNTggMy43NzEgNC4wN20xMS45NTYuODMzYy4zODUtLjY4OSAzLjAwNC00LjMxMiA4LjQyNy0yLjk5MyAxLjQyNS4zNDcgMi4wODQuODU3IDIuMjIzIDEuMDU3LjIwNS4yOTYuMjYyLjcxOC4wNTMgMS4yODYtLjQxMiAxLjEyNi0xLjI2MyAxLjA5NS0xLjczNC44NzUtLjMwNS0uMTQyLTQuMDgyLTIuNjYtNy41NjIgMS4wOTctLjI0LjI1Ny0uNjY4LjM0Ni0xLjA3My4wNC0uNDA3LS4zMDgtLjU3NC0uOTMtLjMzNC0xLjM2MiIvPjxwYXRoIGZpbGw9IiNmZmYiIGQ9Ik02MC41NzYgODMuMDhoLS4xNTNjLTkuOTk2LjEyLTIyLjExNi0xLjIwNC0zMy44NTQtMy41MTgtMS4wMDQgNS44MTgtMS42MSAxMy4xMzItMS4wOSAyMS44NTMgMS4zMTYgMjIuMDQzIDE0LjQwNyAzNS45IDM0LjYxNCAzNi4xaC44MmMyMC4yMDgtLjIgMzMuMjk4LTE0LjA1NyAzNC42MTYtMzYuMS41Mi04LjcyMy0uMDg3LTE2LjAzNS0xLjA5Mi0yMS44NTQtMTEuNzM5IDIuMzE1LTIzLjg2MiAzLjY0LTMzLjg2IDMuNTE4Ii8+PHBhdGggZmlsbD0iI2ViMTkyMyIgZD0iTTMyLjEwMiA4MS4yMzV2MjEuNjkzczkuOTM3IDIuMDA0IDE5Ljg5My42MTZWODMuNTM1Yy02LjMwNy0uMzU3LTEzLjEwOS0xLjE1Mi0xOS44OTMtMi4zIi8+PHBhdGggZmlsbD0iI2ViMTkyMyIgZD0iTTEwNS41MzkgNjAuNDEycy0xOS4zMyA2LjEwMi00NC45NjMgNi4yNzVoLS4xNTNjLTI1LjU5MS0uMTcyLTQ0Ljg5Ni02LjI1NS00NC45NjItNi4yNzVMOC45ODcgNzYuNTdjMTYuMTkzIDQuODgyIDM2LjI2MSA4LjAyOCA1MS40MzYgNy44NDVoLjE1M2MxNS4xNzUuMTgzIDM1LjI0Mi0yLjk2MyA1MS40MzctNy44NDV6bTAgMCIvPjwvc3ZnPg==)](https://qun.qq.com/qqweb/qunpro/share?_wv=3&_wwv=128&inviteCode=1xY0qn&from=246610&biz=ka) [![Download](https://img.shields.io/github/v/release/LSPosed/LSPosed?color=critical&label=Downloads&logo=data:image/png;base64,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)](https://github.com/LSPosed/LSPosed/releases/latest) [![Stars](https://img.shields.io/github/stars/LSPosed/LSPosed?label=Stars&color=important&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABwAAAAdCAYAAAC5UQwxAAADKUlEQVRIib2WX0iTURTAz737tpWEissM6c9cOUUkCqmHpYEtfAiFyl4yCoQefOmhHozAECGweskMw+hRitRQsD8ULSl0H1mUIDRr5myVpgvJQvP7f+P7Nsfc9s3dMTpw4ePee87vnHvvOeeD/y2IEEKNFOYHMom8lGHedGKWVjcloDJu7QLxRz7exTtpdRlqmurlot+KAHAqutRKsu/YeaQABgIge/e30upTR2hY6K8FEzhADfE3q9DqU0Uo+uoaQFCpQU01UmXS2UJjg+7RjCI3EHBoQFUIABFhGO0lFcmaSDpC6cuZ01p0kZcQilL21TQmayfpCMkoGkIA5TEuKlqkLL/dVWG2ONe80xggH7iXj4XPdiz5rUicKgDBZ8OC36Y+EsDggGj/1HlZ+2KJectXhSnwEaN1Ckw2n8zs8JrzTn1ftZ2bbjeb5i42gwHKkLy0QVNWwBE2hiNGIlEixopTGFjtvg0Zf4kEb+W8C1e1CCVP2XXm1/t9kAGO1NI5gajwJWBJVqEXlXrrNfNMybtzYu6RXuCBTTMOgAOW5FYOqjCIfKVGe3+baDnaC8tphC4Dq+Q4Xcg+eGllatUBGgv72kRLbXdaoBrskAvbXc2R0zE3Zix80C5Zjgeh9I0kAlb1DNufN0cv6eahOFnXYFzoPgmMUk4FE9Gwkl39EO8cuBZvOWHiK2NZj7H053C4lK0lMgDBxpdot1CptzNhEmCymKnlYrKiWiNiwg6kC+R/9uWAqGCqvEQASAIszHYWUwOx4CkNVxwaIeBAwoSdGogEb6wSClUOtWvwoe/oI1cbszBeqmdX97yR4C2KcYcL1kcpt/4O4PUcE7h1VqudplBJDDmAhU9F9EDxY3EYKGiFmZWzK11SXlOLOftgsA1t67gvT9Q0GhYeaUcJ5tDfgOS36tkFNS3iDWUUhsgbIOQ1uGXPnhtcoGej3l5u/sk6yeNoJSPgJiNAyDtwc/MvcLy98Q3MdJSQIXArY9YubqbTrgeKHnzgbr78oeQ2eQVu8VtTVbw9cRNfnL58APFzmxnbzR7do0kg4lRjNWGwZNp65Wkq+ukTAPgHIIGzcZjmG+EAAAAASUVORK5CYII=
)](https://github.com/LSPosed/LSPosed)

## Introduction 

A Riru module trying to provide an ART hooking framework which delivers consistent APIs with the OG Xposed, leveraging YAHFA hooking framework.

> Xposed is a framework for modules that can change the behavior of the system and apps without touching any APKs. That's great because it means that modules can work for different versions and even ROMs without any changes (as long as the original code was not changed too much). It's also easy to undo. As all changes are done in the memory, you just need to deactivate the module and reboot to get your original system back. There are many other advantages, but here is just one more: Multiple modules can do changes to the same part of the system or app. With modified APKs, you to decide for one. No way to combine them, unless the author builds multiple APKs with different combinations.

## Supported Versions

Android 8.1 ~ 12, 12L DP1

## Install

1. Install Magisk v23+
2. Install [Riru](https://github.com/RikkaApps/Riru/releases) v25+ from Magisk repo
3. [Download](#download) and install LSPosed in Magisk app
4. Reboot
5. Follow the prompts to add LSPosed shortcut to launcher
    - Some launchers won't show the prompt but silently add the shortcut
    - If the shortcut cannot be added, you can install the manager manually by `/data/adb/lspd/manager.apk`
    - If you accidentally delete the shortcut, reboot your device or install the manager manually to add the shortcut again
    - If you don't need the shortcut, install the manager manually and you can disable future shortcut adding in the settings
    - In any case, you can dial `*#*#5776733#*#*` (aka LSPosed) to launch the manager if you have a dialer
6. Have fun :)

## Download

For stable release, please go to [Github Release page](https://github.com/LSPosed/LSPosed/releases)
For canary build, please check [Github Actions](https://github.com/LSPosed/LSPosed/actions)
Note: debug build is only available on Github Actions.

## Get Help

- GitHub issues: [Issues](https://github.com/LSPosed/LSPosed/issues/)
- (For Chinese speakers) 本项目只接受英语**标题**的issue。如果您不懂英语，请使用[翻译工具](https://www.deepl.com/zh/translator)

## For Developers

Developers are welcomed to write Xposed modules with hooks based on LSPosed Framework. Module written based on LSPosed framework is fully compatible with the original Xposed Framework, so contrary a Xposed Framework-based module will work well with the LSPosed framework too.

- [Xposed Framework API](https://api.xposed.info/)

We use our own module repository. We welcome developers to submit modules to our repository, and then modules can be downloaded in LSPosed.

- [LSPosed Module Repository](https://github.com/Xposed-Modules-Repo)

## Community Discussion

- Telegram: [@LSPosed](https://t.me/s/LSPosed)

Notice: These community group don't accept any bug report, please use [Get help](#get-help) to report.

## Translation Contribute

You can contribute translation [here](https://lsposed.crowdin.com/lsposed).

## Credits 

- [YAHFA](https://github.com/rk700/YAHFA): the core ART hooking framework
- [Magisk](https://github.com/topjohnwu/Magisk/): makes all these possible
- [Riru](https://github.com/RikkaApps/Riru): provides a way to inject codes into zygote process
- [XposedBridge](https://github.com/rovo89/XposedBridge): the OG xposed framework APIs
- [DexBuilder](https://github.com/LSPosed/DexBuilder): to dynamically generate YAHFA hooker classes
- [Dobby](https://github.com/jmpews/Dobby): used for inline hooking
- [EdXposed](https://github.com/ElderDrivers/EdXposed): fork source
- ~[SandHook](https://github.com/ganyao114/SandHook/): ART hooking framework for SandHook variant~
- ~[dexmaker](https://github.com/linkedin/dexmaker) and [dalvikdx](https://github.com/JakeWharton/dalvik-dx): to dynamically generate YAHFA hooker classes~

## License

LSPosed is licensed under the **GNU General Public License v3 (GPL-3)** (http://www.gnu.org/copyleft/gpl.html).
