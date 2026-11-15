# Filament / gltfio используют JNI: сохраняем классы движка при включении минификации.
-keep class com.google.android.filament.** { *; }
-dontwarn com.google.android.filament.**
