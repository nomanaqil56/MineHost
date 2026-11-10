# MineHost ProGuard Rules

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Minecraft & Server models
-keep class com.minehost.app.domain.model.** { *; }
-keep class com.minehost.app.data.local.entity.** { *; }
-keep class com.minehost.app.core.common.** { *; }

