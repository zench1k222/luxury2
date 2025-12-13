# ───────────────────────────────────────────────
# Основные настройки
# ───────────────────────────────────────────────
-dontoptimize
-dontpreverify
-dontwarn
-allowaccessmodification
-overloadaggressively
-dontusemixedcaseclassnames
-optimizationpasses 3

# Оставляем имя пакета (чтобы не было конфликтов с Minecraft)
-repackageclasses ''

# Если хочешь, можно переименовывать в свой namespace:
# -repackageclasses 'myobf'

# ───────────────────────────────────────────────
# Сохраняем аннотации и атрибуты, нужные для Fabric/Mixins
# ───────────────────────────────────────────────
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*

# ───────────────────────────────────────────────
# Не трогаем mixins
# ───────────────────────────────────────────────
-keep @org.spongepowered.asm.mixin.Mixin class *
-keep class * {
    @org.spongepowered.asm.mixin.Shadow *;
    @org.spongepowered.asm.mixin.Final *;
    @org.spongepowered.asm.mixin.Overwrite *;
    @org.spongepowered.asm.mixin.Unique *;
}

-keepclassmembers class * {
    @org.spongepowered.asm.mixin.Shadow *;
}

# ───────────────────────────────────────────────
# Fabric Loader и API не обфусцируем
# ───────────────────────────────────────────────
-keep class net.fabricmc.** { *; }
-keep class net.minecraft.** { *; }   # не трогаем MC классы
-keep class com.mojang.** { *; }

# ───────────────────────────────────────────────
# Оставляем главный класс мода
# ───────────────────────────────────────────────
-keep class dev.luxury.Luxury { *; }

# если мод использует @Environment
-keep class net.fabricmc.api.EnvType { *; }
