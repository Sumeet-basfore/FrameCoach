# MediaPipe Tasks Vision is reflection-heavy — keep its classes in release builds.
# Required per 02_Technical_Architecture_Document.md §4. No-op until T3 wires MediaPipe in,
# but harmless to carry now so the rule isn't forgotten when that ticket lands.
-keep class com.google.mediapipe.** { *; }

# CameraX ships its own consumer/proguard rules; AGP picks them up automatically.
