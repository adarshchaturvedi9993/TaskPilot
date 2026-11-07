# Add project specific ProGuard rules here.
-keep class com.example.data.model.** { *; }
-keep class com.example.data.local.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
