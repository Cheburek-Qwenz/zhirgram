# TDLib вызывается из C++ по именам классов и методов через JNI. Без этого R8 всё переименует,
# сборка пройдёт, а приложение упадёт на телефоне.
-keep class org.drinkless.tdlib.** { *; }
-keepclassmembers class org.drinkless.tdlib.** { *; }
