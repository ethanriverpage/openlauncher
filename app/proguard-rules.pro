-keepattributes *Annotation*
-keep class com.openlauncher.app.data.** { *; }
-keep class com.openlauncher.app.model.** { *; }

# Gson: settings lists are parsed with TypeToken subclasses (generic signatures needed)
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep class * extends com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken

# Climate: ClimateState.toString() is diffed for the state log; keep field names readable
-keep class com.openlauncher.app.climate.ClimateState { *; }
