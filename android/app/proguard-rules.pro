# kotlinx.serialization and Retrofit ship their own consumer rules. Keep our DTOs and routes serializable.
-keep,includedescriptorclasses class com.chinmay.catalogue.**$$serializer { *; }
-keepclassmembers class com.chinmay.catalogue.** {
    *** Companion;
}
-keepclasseswithmembers class com.chinmay.catalogue.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# JavaMail for Android (optional demo email) loads its SMTP transport and content handlers by class name, from
# META-INF/javamail.* and META-INF/mailcap. R8 can't see those lookups, so keep the library's classes as they are.
-keep class javax.mail.** { *; }
-keep class javax.activation.** { *; }
-keep class com.sun.mail.** { *; }
-keep class com.sun.activation.** { *; }
-dontwarn java.awt.**
-dontwarn java.beans.**
-dontwarn javax.security.sasl.**
-dontwarn javax.naming.**
