# kotlinx.serialization and Retrofit ship their own consumer rules. Keep our DTOs and routes serializable.
-keep,includedescriptorclasses class com.chinmay.catalogue.**$$serializer { *; }
-keepclassmembers class com.chinmay.catalogue.** {
    *** Companion;
}
-keepclasseswithmembers class com.chinmay.catalogue.** {
    kotlinx.serialization.KSerializer serializer(...);
}
