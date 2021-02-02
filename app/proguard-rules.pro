# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-keep class io.github.lsposed.manager.Constants { *; }
-keepnames class io.github.lsposed.manager.adapters.* { *; }
-keepnames class io.github.lsposed.manager.receivers.* { *; }
-keepnames class io.github.lsposed.manager.ui.* { *; }
-keepnames class io.github.lsposed.manager.utils.* { *; }
-keepnames class io.github.lsposed.manager.* { *; }