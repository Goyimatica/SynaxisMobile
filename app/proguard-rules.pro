# Add project specific ProGuard rules here.

# Coil 3 discovers its OkHttp network fetcher through a Java service loader
# (META-INF/services). R8 must keep the factory and the service registration
# alive, or a shrunk build silently loses every picture at runtime.
-keep class coil3.network.okhttp.** { *; }
-keep class * implements coil3.fetch.Fetcher$Factory { *; }

# org.json is used by the repos through reflection-free accessors; it is part
# of the platform and needs no keeps. OkHttp and Coroutines ship their own.
