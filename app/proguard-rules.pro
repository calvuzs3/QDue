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
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile


# PROTO ######################################################

# Keep virtual scrolling classes
-keep class net.calvuz.qdue.ui.shared.proto.** { *; }

# Keep CompletableFuture for async operations
-keep class java.util.concurrent.CompletableFuture { *; }
-keep class java.util.concurrent.CompletionStage { *; }

# Keep data callback interfaces
-keep interface net.calvuz.qdue.ui.proto.VirtualCalendarDataManager$DataAvailabilityCallback { *; }

# Prevent obfuscation of animation-related classes
-keep class androidx.dynamicanimation.** { *; }

# ############################################################