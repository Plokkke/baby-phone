# ML Kit's code scanner reaches parts of itself by reflection: minified, the scanner crashed on launch.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_code_scanner.** { *; }
