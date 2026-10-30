# dadb (ADB local) usa reflexão mínima, mas mantemos as classes públicas por segurança.
-keep class dadb.** { *; }
-dontwarn dadb.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
