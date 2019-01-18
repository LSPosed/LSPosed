# EdXposed

A Riru module trying to provide a ART hooking framework (mainly for Android Pie) which delivers a consistent APIs with the OG Xposed, leveraging YAHFA hooking framework.

## Credits 

- [YAHFA](https://github.com/rk700/YAHFA): the core java hooking framework
- [Riru](https://github.com/RikkaApps/Riru): provides a way to inject codes to zygote process
- [XposedBridge](https://github.com/rovo89/XposedBridge): the OG xposed framework APIs
- [dexmaker](https://github.com/linkedin/dexmaker) and [dalvikdx](https://github.com/JakeWharton/dalvik-dx): dynamiclly generate YAHFA hooker classes

## Known issues

- resources hooking is not supported yet
- may not be compatible with all ART devices
- only a few Xposed modules has been tested for working

## Build

1. run `:Bridge:makeAndCopyRelease` in Gradle window to build `edxposed.dex`
2. run `:Core:zipRelease` to build Magisk Riru module flashable zip file
3. find the flashable under `Core/release/`
4. flash the zip in recovery mode or in Magisk Manager

## Contribute

Apparently this framework is far from stable and all kinds of PRs are welcome. :)