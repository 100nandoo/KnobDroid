# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ACRA instantiates these via reflection — must not be renamed or removed
-keep class dev.halim.knobdroid.acra.CrashFileSenderFactory { *; }
-keep class dev.halim.knobdroid.acra.CrashFileReportSender { *; }

# ACRA reads BuildConfig fields by reflection for the crash report metadata
-keep class dev.halim.knobdroid.BuildConfig { *; }

# Keep stack trace line numbers in crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile