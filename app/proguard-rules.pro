# Extension APKs are independently obfuscated. Keeping Tankobun's class names
# prevents parent-first class loading from resolving their short names (a, b,
# p0, and so on) to unrelated app classes. Mihon uses the same boundary rule.
-dontobfuscate

# Tachiyomi extensions are loaded from external APKs and link against these
# compatibility shims by their original JVM names.
-keep class eu.kanade.tachiyomi.** { *; }
-keep interface eu.kanade.tachiyomi.** { *; }
-keep enum eu.kanade.tachiyomi.** { *; }

-keep class uy.kohesive.injekt.** { *; }
-keep interface uy.kohesive.injekt.** { *; }

-keep class app.cash.quickjs.** { *; }
-keep interface app.cash.quickjs.** { *; }

# External Tachiyomi extension APKs are compiled against these runtime
# libraries and resolve them through Tankobun's class loader.
-keep class androidx.preference.** { *; }
-keep interface androidx.preference.** { *; }

-keep class kotlin.** { *; }
-keep interface kotlin.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keep interface kotlinx.coroutines.** { *; }
-keep class kotlinx.serialization.** { *; }
-keep interface kotlinx.serialization.** { *; }

-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }
-keep interface okio.** { *; }
-keep class rx.** { *; }
-keep interface rx.** { *; }
-keep class org.jsoup.** { *; }
-keep interface org.jsoup.** { *; }

-keepattributes InnerClasses,EnclosingMethod,Signature,*Annotation*

# Tink references these compile-time annotations through AndroidX Security.
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
