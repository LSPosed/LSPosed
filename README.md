[![Build status](https://ci.appveyor.com/api/projects/status/qu3vj1d64nqia1b8/branch/master?svg=true)](https://ci.appveyor.com/project/ElderDrivers/edxposed/branch/master)

# EdXposed

A Riru module trying to provide a ART hooking framework (initially for Android Pie) which delivers consistent APIs with the OG Xposed, leveraging YAHFA (or SandHook) hooking framework.

## Supported versions

- Android Oreo (8.0, 8.1) 
- Android Pie (9.0)

For devices with Android 7.x and lower, original Xposed is strongly recommended.

## Build requirements

Same as [Riru-Core's](https://github.com/RikkaApps/Riru/blob/master/README.md#build-requirements)
and zip binaries can be downloaded from [here](http://gnuwin32.sourceforge.net/packages/zip.htm).

## Build

1. Execute task `:edxp-core:[zip|push][Yahfa|Sandhook]Release` to build flashable zip for corresponding variant.
2. Find the flashable under `edxp-core/release/`.
3. Flash the zip in recovery mode or via Magisk Manager.

## Install

1. Install Magisk v19.0+.
2. Install [Riru-Core](https://github.com/RikkaApps/Riru/releases) v19+ from Magisk repo.
3. Download [EdXposed](https://github.com/solohsu/EdXposed/releases) and install it in Magisk Manager or recovery mode.
4. Install companion application.
4. Reboot.
5. Have fun! :)

## Companion applications

- For v0.2.9.5 and before: [Xposed Installer](https://github.com/DVDAndroid/XposedInstaller).
- For v0.2.9.6 and v0.2.9.7: [Xposed Installer](https://github.com/DVDAndroid/XposedInstaller) and [EdXp Manager](https://github.com/solohsu/EdXpManager)(optional).
- For v0.2.9.8 and later: [EdXposed Installer](https://github.com/solohsu/XposedInstaller) and [EdXposed Manager](https://github.com/ElderDrivers/EdXposedManager).

## Useful links

- [List of Xposed Modules For Android Pie Working With EdXposed](https://forum.xda-developers.com/xposed/list-xposed-modules-android-pie-ed-t3892768) (thanks to Uraniam9 @ xda-developers)

## Known issues

- May not be compatible with all ART devices.
- File access services are not implemented yet, now EdXp simply uses magiskpolicy to enable needed SELinux policies.

## Get help

- GitHub issues (recommended): [Issues](https://github.com/solohsu/EdXposed/issues/)
- QQ Group: [855219808](http://shang.qq.com/wpa/qunwpa?idkey=fae42a3dba9dc758caf63e971be2564e67bf7edd751a2ff1c750478b0ad1ca3f)

## Contribute

- Apparently this framework is far from stable and all kinds of PRs are welcome. :)
- [Buy me a coffee](https://www.paypal.me/givin2u) if you like my work.

## Credits 

- [YAHFA](https://github.com/rk700/YAHFA): the core ART hooking framework
- [Magisk](https://github.com/topjohnwu/Magisk/): makes all these possible
- [Riru](https://github.com/RikkaApps/Riru): provides a way to inject codes into zygote process
- [XposedBridge](https://github.com/rovo89/XposedBridge): the OG xposed framework APIs
- [dexmaker](https://github.com/linkedin/dexmaker) and [dalvikdx](https://github.com/JakeWharton/dalvik-dx): to dynamiclly generate YAHFA hooker classes
- [Whale](https://github.com/asLody/whale): used for inline hooking
- [SandHook](https://github.com/ganyao114/SandHook/): ART hooking framework for SandHook variant

