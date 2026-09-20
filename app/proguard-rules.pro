# NewPipeExtractor ships a small JS engine (Rhino) to solve YouTube's
# signature cipher - keep it intact under minification.
-keep class org.mozilla.javascript.** { *; }
-dontwarn org.mozilla.javascript.**

-keep class org.schabi.newpipe.extractor.** { *; }
-dontwarn org.schabi.newpipe.extractor.**

-dontwarn okhttp3.**
-dontwarn okio.**
