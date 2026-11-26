# Keep rules will be added as the app grows.

# ONNX Runtime uses JNI and resolves these classes by name.
-keep class ai.onnxruntime.** { *; }
-keepclassmembers class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

# Tink / AndroidX Security Crypto annotations and reflection.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.api.client.http.**
-dontwarn org.joda.time.Instant
