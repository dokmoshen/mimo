# Keep rules will be added as the app grows.

# ONNX Runtime uses JNI and resolves these classes by name.
-keep class ai.onnxruntime.** { *; }
-keepclassmembers class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**
