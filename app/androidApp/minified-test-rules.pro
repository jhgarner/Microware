# The instrumentation runner shares these libraries with the app, so keep the parts it calls.
-keep class androidx.tracing.** { *; }
-keep class kotlin.** { *; }
-keep class kotlinx.serialization.** { *; }
