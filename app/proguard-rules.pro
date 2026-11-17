# Keep Firestore @Keep models and Kotlinx Serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @com.google.firebase.firestore.DocumentId <fields>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}
